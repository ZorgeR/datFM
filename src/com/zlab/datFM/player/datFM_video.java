package com.zlab.datFM.player;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.zlab.datFM.R;
import com.zlab.datFM.datFM;

import java.io.IOException;

public class datFM_video extends Activity {

        //String FileName = "";
        String MediaURL = "";
        //String mURL;

        //MediaPlayer mediaPlayer;
        VideoView videoWindow;
        //Button play;
        //SeekBar seek;
        //SeekBar volume;
        //TextView time;
        AudioManager am;
        int total;
        ProgressDialog audio_cache = null;

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.datfm_player_video);

            MediaURL = getIntent().getExtras().getString("MediaURL");

            //FileName = getIntent().getExtras().getString("FileName");
            audio_cache = ProgressDialog.show(this, "Кеширование ...", "Подождите ...", true);

            //TextView txtBookName 		= (TextView) findViewById(R.id.datFM_audio_FileName);
            videoWindow = (VideoView) findViewById(R.id.datfm_player_videoView);
            videoWindow.setMediaController(new MediaController(this));
            videoWindow.requestFocus(0);

            //txtBookName.setText(FileName);

            // VIEW
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            //play = (Button)findViewById(R.id.button_play);
            //seek = (SeekBar)findViewById(R.id.seekBar1);
            //volume = (SeekBar) findViewById(R.id.seekBar_volume);
            //time = (TextView) findViewById(R.id.datFM_audio_time);

            // Перемотка

            // ГРОМКОСТЬ
            am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
           // volume.setMax(maxVolume);
            //volume.setProgress(curVolume);

            videoWindow.setOnPreparedListener(MyVideoViewPreparedListener);
            videoWindow.setOnCompletionListener(myVideoViewCompletionListener);
            // ПЛЕЕР
            startPlay();
        }

    private void startPlay(){
        if(MediaURL!=null){
            Uri uri = Uri.parse(MediaURL);
            videoWindow.setVideoURI(uri);

            //mURL=MediaURL;
            if (datFM.pref_build_in_video_player){
                //preparePlayer();
                videoWindow.start();
            }else{
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "audio/*");
                startActivity(intent);
            }
        }
    }
    MediaPlayer.OnPreparedListener MyVideoViewPreparedListener
            = new MediaPlayer.OnPreparedListener(){
        @Override
        public void onPrepared(MediaPlayer arg0) {
            audio_cache.dismiss();
        }};
    MediaPlayer.OnCompletionListener myVideoViewCompletionListener
            = new MediaPlayer.OnCompletionListener(){
        @Override
        public void onCompletion(MediaPlayer arg0) {
            videoWindow.stopPlayback();
        }};

    public void closePlayer(View v){
        finish();
    }
    public void fullScreen(View v){
    }

        @Override
        public void onDestroy() {
            super.onDestroy();
            releaseMediaPlayer();
        }

        private void releaseMediaPlayer() {
            if (videoWindow != null) {
                if(videoWindow.isPlaying()) {
                    videoWindow.stopPlayback();
                }
            }
        }
}
