package com.example.recordbutton;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView nowTime;
    SeekBar seekBar;
    RecordButton recordButton;
    LinearLayout linearLayout;
    Button playButton;
    TextView mTotalTime;
    String file;
    MediaPlayer player;
    Thread thread;
    Handler playingHandler;
    boolean isPlaying;
    Timer timer;


    public boolean isReplay;
    public static final int TIME_WHAT_100 = 100;
    public static final int Finish_WHAT_101 = 101;
    public static final int UNSTICK_WAHT = 200;
    public static final int STICK_WAHT = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.seek_bar);
        nowTime = findViewById(R.id.text);
        linearLayout = findViewById(R.id.linearLayout);
        playButton = findViewById(R.id.play_button);
        mTotalTime = findViewById(R.id.total_time);
        playingHandler = new PlayingHandler();
        player = new MediaPlayer();
        initMediaPlayer();
        initTotalTime();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 10){
                    nowTime.setText("00:" + progress);
                }else {
                    nowTime.setText("00:0" + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (isPlaying && player.isPlaying()){
                    seekBar.setTag(player.isPlaying());
                    isPlaying = false;
                    player.pause();
                    playButton.setActivated(false);
                }else {
                    seekBar.setTag(false);
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if ((boolean)seekBar.getTag()){
                    playButton.setActivated(true);
                    player.seekTo(seekBar.getProgress()*1000);
                    player.start();
                    Log.i("MediaPlayer", "onStopTrackingTouch: " + player.getCurrentPosition());
                    thread = new MyThread();
                    thread.start();
                    isPlaying = true;
                }else {
                    player.seekTo(seekBar.getProgress()*1000);
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying){
                    playButton.setActivated(true);
                    player.start();
                    thread = new MyThread();
                    thread.start();
                    isPlaying = true;
                }else {
                    playButton.setActivated(false);
                    player.pause();
                    isPlaying = false;
                }

            }
        });
    }



    public void initMediaPlayer(){
        String name = getExternalFilesDir("audio").list()[0];
        file = getExternalFilesDir("audio").getAbsolutePath() + "/" + name;
        try {
            player.setDataSource(file);
            player.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void initTotalTime(){
        int time = player.getDuration();
        int second = time / 1000;
        if (second >= 10){
            mTotalTime.setText("00:" + second);
        }else {
            mTotalTime.setText("00:" + "0" + second);
        }
        seekBar.setMax(second);
    }

    class MyThread extends Thread{
        @Override
        public void run() {
            while (isPlaying){
                try {
                    Thread.sleep(300);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                int nowPosition = player.getCurrentPosition();
                if (nowPosition == player.getDuration()){
                    playingHandler.sendEmptyMessage(Finish_WHAT_101);
                }
                Message timeMsg = playingHandler.obtainMessage();
                timeMsg.obj = nowPosition;
                timeMsg.what = TIME_WHAT_100;
                //实时更新进度条
                playingHandler.sendMessage(timeMsg);
            }
        }
    }

    class PlayingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TIME_WHAT_100){
                int time = (int)msg.obj;
                int second = time / 1000;
                seekBar.setProgress(second);
                seekBar.animate();
            }else if (msg.what == Finish_WHAT_101){
//                player.stop();  //调用完stop()之后player就不能播放了
                playButton.setActivated(false);
                player.reset();
                isPlaying = false;
                isReplay = true;
                initMediaPlayer();
            }
        }
    }


    public void getProgressing(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int p = player.getCurrentPosition();
                seekBar.setProgress(p);
//                nowTime.setText("00:0" + p);
            }
        },0,200);
    }

    public void stopTimer(){
        if (timer!=null){
            timer.cancel();
        }
    }



    class SeekBarRunnable implements Runnable{
        @Override
        public void run() {
            while (isPlaying){
                seekBar.setProgress(player.getCurrentPosition());
                nowTime.setText(player.getCurrentPosition());
                try {
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }







}
