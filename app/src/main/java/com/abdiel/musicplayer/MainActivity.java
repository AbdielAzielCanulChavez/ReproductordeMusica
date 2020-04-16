package com.abdiel.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 12345;

    private static final int PERMISSIONS_COUNT = 1;

    @SuppressLint("NewApi")
    private boolean arePermissionDenied(){
        for(int i = 0;i < PERMISSIONS_COUNT; i++){

            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED){
                    return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(arePermissionDenied()){
            ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        }else {
            //lo mandamos al metodo on resume
            onResume();
        }
    }

    private boolean isMusicPlayerInit;

    private List<String> musicFilesList;

    private void addMusicFIlesFrom(String dirPath){

        final File musicDir = new File(dirPath);
        if(!musicDir.exists()){
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles();
        for(File file : files){
            final String path = file.getAbsolutePath();
            if(path.endsWith(".mp3")){
                musicFilesList.add(path);
            }
        }
    }

    private void fillMusicList(){
        musicFilesList.clear();
        addMusicFIlesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC)));
        addMusicFIlesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));

    }


    private MediaPlayer mp; //hacemos la variable global para que mas metodos lo puedan usar ademas de uso en el click del seekbar

    private int playMusicFile(String path){

        mp = new MediaPlayer();
          try {
              mp.setDataSource(path);
              mp.prepare();
              mp.start();
          }catch (Exception e){
              e.printStackTrace();
          }
        return mp.getDuration();
    }


    private int songPosition;
    private volatile boolean isSongPlaying;

    private int mPosition;

    private void playSong(){
        final String musicFilePath = musicFilesList.get(mPosition);
        final int songDuration =   playMusicFile(musicFilePath)/1000;   //division para minutos y segundos
        seekBar.setMax(songDuration);
        seekBar.setVisibility(View.VISIBLE);
        playBackControls.setVisibility(View.VISIBLE);
        //para que se vean los minutos y segundos
        songDurationTextView.setText(String.valueOf(songDuration/60)+":"+String.valueOf(songDuration%60));


        //hacemos un hilo para la tarea de la barra
        new Thread(){
            public void run(){

                songPosition = 0;
                isSongPlaying = true;
                while (songPosition<songDuration){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(isSongPlaying){
                        songPosition++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                seekBar.setProgress(songPosition);
                                songPositionTextView.setText(String.valueOf(songPosition/60)+":"+String.valueOf(songPosition%60));
                            }
                        });
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mp.pause();
                        songPosition = 0;
                        mp.seekTo(songPosition);
                        songPositionTextView.setText("0");
                        pauseButton.setText("play");
                        isSongPlaying = false;
                        seekBar.setProgress(songPosition);

                    }
                });


            }
        }.start(); //se inicia el hilo
    }

    //
    private TextView songPositionTextView;
    private TextView songDurationTextView;
    private SeekBar seekBar;
    private View playBackControls;
    private Button pauseButton;

    //aqui hacemos si el usuario no de da en allow o permitir a la app tomar datos de su dispositivo mandamos a segundo plano la app
    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && arePermissionDenied()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if(!isMusicPlayerInit){

            final ListView listView = findViewById(R.id.listview);
            final TextAdapter textAdapter = new TextAdapter();
            musicFilesList = new ArrayList<>();
            fillMusicList();
            textAdapter.setData(musicFilesList);
            listView.setAdapter(textAdapter);

             seekBar = findViewById(R.id.seekbar); //declaramos el seekbar


            //eventos del seekbar
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgress;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    //cambios cuando la cancion esta en reproducccion
                    songProgress = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //metodo del onclick cuando se le da click a parar o stop
                   songPosition=songProgress;
                    mp.seekTo(songProgress*1000);
                }
            });

            songPositionTextView = findViewById(R.id.currentPosition);
            songDurationTextView = findViewById(R.id.songDuration);
            pauseButton = findViewById(R.id.pauseButton);

            playBackControls=findViewById(R.id.playBackButtons);

            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isSongPlaying){
                        mp.pause();
                        pauseButton.setText("play");
                    }else {
                        if(songPosition==0){
                            playSong();
                        }else {
                            mp.start();
                        }
                        pauseButton.setText("pause");
                    }
                    isSongPlaying = !isSongPlaying;

                }
            });




            //metodo cuando se le hace click
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mPosition = position;
                    playSong();
                    }
            });


             isMusicPlayerInit=true;
        }
    }

    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<>();
        void setData(List<String> mData){
            data.clear();
            data.addAll(mData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount(){
            return data.size();
        }

        @Override
        public Object getItem(int position){
            return null;
        }

        @Override
        public long getItemId(int position){
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            if(convertView == null){
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.myItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = data.get(position);
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            return convertView;
        }

        class ViewHolder{
            TextView info;
            ViewHolder(TextView mInfo){
                info = mInfo;
            }
        }
    }


}
