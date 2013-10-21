package com.zlab.datFM.player;


import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.zlab.datFM.R;

public class datFM_audio extends Activity {

        String FileName = "";
        //String Author = "";
        String MediaURL = "";
        String mURL;
        boolean BuildInPlayer;
        //private DownloadManager mgr=null;
        //private File offlinebookfile;
        //private Button downloadButton;

        MediaPlayer mediaPlayer;
        Button play;
        SeekBar seek;
        SeekBar volume;
        TextView time;
        AudioManager am;
        int total;
        ProgressDialog audio_cache = null;

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.datfm_player_audio);
            BuildInPlayer=true;

            MediaURL = getIntent().getExtras().getString("MediaURL");
            FileName = getIntent().getExtras().getString("FileName");
            //Author = getIntent().getExtras().getString("Author");
            //BookLogoURL = getIntent().getExtras().getString("BookLogoURL");
            //ReleaseDATA = getIntent().getExtras().getString("ReleaseDATA");
            //SizeKB = getIntent().getExtras().getString("SizeKB");
            //LanguageCODE = getIntent().getExtras().getString("LanguageCODE");
            //BuildInPlayer = getIntent().getExtras().getBoolean("BuildInPlayer");

            TextView txtBookName 		= (TextView) findViewById(R.id.datFM_audio_FileName);
            //TextView txtAuthor 		= (TextView) findViewById(R.id.txtBookAuthor_detail);
//			TextView txtBookLogoURL 	= (TextView) findViewById(R.id.txtBookTotal);
            //TextView txtReleaseDATA 	= (TextView) findViewById(R.id.txtBookData_detail);
//			TextView txtMediaURL 		= (TextView) findViewById(R.id.txtStorageSize);
//			TextView txtSizeKB 			= (TextView) findViewById(R.id.txtType);
//			TextView txtLanguageCODE	= (TextView) findViewById(R.id.txtLastUpdate);

            txtBookName.setText(FileName);
            //txtAuthor.setText(Author);
//			txtBookLogoURL.setText(BookLogoURL);
            //txtReleaseDATA.setText(ReleaseDATA);
//			txtMediaURL.setText(MediaURL);
//			txtSizeKB.setText(SizeKB);
//			txtLanguageCODE.setText(LanguageCODE);

            // Меняем шрифт
			/*
		    Typeface ptcaption=Typeface.createFromAsset(getAssets(),"fonts/ptcaption.ttf");
		    Typeface ptcaptionnormal=Typeface.createFromAsset(getAssets(),"fonts/ptcaptionnormal.ttf");
		    txtBookName.setTypeface(ptcaptionnormal);
		    txtAuthor.setTypeface(ptcaption);
		    txtReleaseDATA.setTypeface(ptcaptionnormal);
			 */

            //mgr=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);

            //offlinebookfile = new File(android.os.Environment.getExternalStorageDirectory()+"/Download/audiobooks/", BookName+".mp3");
            //downloadButton = (Button) findViewById(R.id.button_download);
            //if (offlinebookfile.exists()==true){downloadButton.setEnabled(false);} else {downloadButton.setEnabled(true);}


            // VIEW
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            play = (Button)findViewById(R.id.button_play);
            seek = (SeekBar)findViewById(R.id.seekBar1);
            volume = (SeekBar) findViewById(R.id.seekBar_volume);
            time = (TextView) findViewById(R.id.datFM_audio_time);

            // Перемотка
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch(SeekBar cur) {
                    mediaPlayer.seekTo(cur.getProgress()*total/100);
                }
                public void onStartTrackingTouch(SeekBar arg0) {
                }
                public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                }
            });

            // ГРОМКОСТЬ
            am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            volume.setMax(maxVolume);
            volume.setProgress(curVolume);
            volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch(SeekBar arg0) {
                }
                public void onStartTrackingTouch(SeekBar arg0) {
                }
                public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
                }
            });
            // ПЛЕЕР
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        audio_cache.dismiss();
                        total = mediaPlayer.getDuration();
                        mp.start();
                        playbuttonview.sendEmptyMessage(0);
                        whatchdog();
                    }
                });
                mediaPlayer.setOnCompletionListener(new OnCompletionListener(){
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        playbuttonview.sendEmptyMessage(0);
                    }
                });
            }
            startPlay();
        }

        public void play_book_button_click (View view){
            switch (view.getId()) {
                case R.id.button_play:{
                    startPlay();
                    };
            }
        }
    private void startPlay(){
        if(MediaURL!=null){
            Uri uri = Uri.parse(MediaURL);
            mURL=MediaURL;
            if (BuildInPlayer){
                preparePlayer();
            }else{
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "audio/*");
                startActivity(intent);
            }}
    }
    public void closePlayer(View v){
        finish();
    }

        @Override
        public void onDestroy() {
            super.onDestroy();
            releaseMediaPlayer();
        }

        private Handler playbuttonview = new Handler(){
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                Button playview = (Button) findViewById(R.id.button_play);
                if(mediaPlayer.isPlaying()) {
                    playview.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pause));
                } else {
                    playview.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_play));
                }
            }
        };

        private Handler setTimeHandler = new Handler(){
            public void handleMessage(Message msg){
                //Toast.makeText(com.zlab.audiobooks.AudiobooksDetail.this, String.valueOf(msg), Toast.LENGTH_LONG).show();
                int CurentSec = msg.what/1000;
                int TotalSec = total/1000;
                int hours = CurentSec / 3600;
                int minutes = (CurentSec % 3600) / 60;
                int seconds = CurentSec % 60;

                int hours_total = TotalSec / 3600;
                int minutes_total = (TotalSec % 3600) / 60;
                int seconds_total = TotalSec % 60;

                String CurentTimeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                String TotalTimeString = String.format("%02d:%02d:%02d", hours_total, minutes_total, seconds_total);

                //String CurentTimeString = hours + ":" + minutes + ":" + seconds;
                //String TotalTimeString = hours_total + ":" + minutes_total + ":" + seconds_total;

                time.setText(CurentTimeString + " / " + TotalTimeString);

                //
                int progress = msg.what * 100 / total;
                seek.setProgress(progress);
            }
        };

        public void whatchdog(){
            new Thread() {
                public void run() {
                    try{
                        while(mediaPlayer != null && mediaPlayer.isPlaying()){
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            Message msg = new Message();
                            msg.what = currentPosition;
                            setTimeHandler.sendMessage(msg);
                            Thread.sleep(1000);
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        private void releaseMediaPlayer() {
            if (mediaPlayer != null) {
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
        private void preparePlayer() {
            audio_cache = ProgressDialog.show(this, "Кеширование ...", "Подождите ...", true);

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(mURL);
                mediaPlayer.prepareAsync();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this,"URI Error",Toast.LENGTH_LONG).show();audio_cache.dismiss();
                e.printStackTrace();
            } catch (IllegalStateException e) {
                // Toast.makeText(this,"Уже играю! Спокойной!",Toast.LENGTH_LONG).show();audio_cache.dismiss();
                audio_cache.dismiss();
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playbuttonview.sendEmptyMessage(0);
                } else {
                    mediaPlayer.start();
                    playbuttonview.sendEmptyMessage(0);
                    whatchdog();}
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(this,"IO Error",Toast.LENGTH_LONG).show();audio_cache.dismiss();
                e.printStackTrace();
            }
        }

}
