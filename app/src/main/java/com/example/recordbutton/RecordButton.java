package com.example.recordbutton;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RecordButton extends AppCompatButton {
    public static final int MAX_INTERVAL_TIME = 6 * 1000;
    public static final int MIN_INTERVAL_TIME = 1000;
    public int MAX_CANCEL_LENGTH = -200;

    public static final int VOLUME_WHAT_100 = 100;
    public static final int TIME_WHAT_101 = 101;
    public static final int STOP_WHAT_102 = 102;

    private String mFilePath;
    private String mFileName;
    private String mFile;

    //文件名前缀
    private String mPref;
    private long mStartTime;

    private MediaRecorder mMediaRecorder;
    private Dialog mDialog;

    private ImageView volumeImage;
    private TextView recordTimeText;
    private TextView recordTipText;

    //按下button时的Y坐标
    private int startY;

    private MyHandler mHandler;
    private RecordingThread mThread;



    public RecordButton(Context context){
        super(context);
        init();
    }

    public RecordButton(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
        init();
    }

    public RecordButton(Context context, AttributeSet attributeSet, int defStyleAttr){
        super(context,attributeSet,defStyleAttr);
        init();
    }

    public void init(){
        mHandler = new MyHandler();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        MAX_CANCEL_LENGTH = - getMeasuredHeight() / 2;
        switch (action){
            case MotionEvent.ACTION_DOWN:
                startY = (int) getY();
                mStartTime = SystemClock.currentThreadTimeMillis();
                prepareAndStartRecord();
                break;
            case MotionEvent.ACTION_UP:
                int endY = (int) getY();
                if (endY - startY < MAX_CANCEL_LENGTH){
                    cancelRecording();
                }else {
                    finishRecording();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int nowY = (int) getY();
                if (nowY - startY < MAX_CANCEL_LENGTH){
                    recordTipText.setText("松开取消发送");
                }else {
                    recordTipText.setText("上滑取消发送");
                }
        }

        return true;
    }

    public void prepareAndStartRecord(){
        String tempFilePath;
        if (mFilePath != null){
            tempFilePath = mFilePath;
        }else {
            tempFilePath = getDefaultPath();
        }

        String tempFileName;
        if (mFileName != null){
            tempFileName = mFileName;
        }else {
            tempFileName = getDefaultName();
        }

        mFile = tempFilePath + "/" + tempFileName;

        if (mMediaRecorder == null){
            mMediaRecorder = new MediaRecorder();
        }else {
            mMediaRecorder.reset();
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOutputFile(mFile);
        try {
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mMediaRecorder.setAudioSamplingRate(44100);
//            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            mMediaRecorder.setOutputFile(mFile);
//            mMediaRecorder.setOutputFile(mFilePath);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        }catch (IOException e){
            e.printStackTrace();
        }

        showDialog();
        mThread = new RecordingThread();
        mThread.start();

    }

    public void finishRecording(){
        long now = SystemClock.currentThreadTimeMillis();
        //如果时间不到一秒，那就提示然后删掉
        if (now - mStartTime < MIN_INTERVAL_TIME){
            recordTipText.setText("时间过短！");
            cancelRecording();
        }else {
            //正常结束录音
            stopRecording();
        }
    }

    public void cancelRecording(){
        stopRecording();
        File file = new File(mFile);
        if (file.exists()){
            file.delete();
        }
    }

    public void stopRecording(){

        if (mThread != null){
            mThread.exit();
            mThread = null;
        }
        if (mMediaRecorder != null){
            mMediaRecorder.stop();
            mMediaRecorder = null;
        }
        mDialog.dismiss();
    }

    public void showDialog(){
        if (mDialog == null){
            mDialog = new Dialog(getContext());
        }
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.record_dialog_layout, null);
        volumeImage = dialogView.findViewById(R.id.record_volume_image);
        recordTimeText = findViewById(R.id.record_time_text);
        recordTipText = findViewById(R.id.record_tip_text);
        mDialog.addContentView(dialogView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mDialog.show();

    }




    public String getmFilePath() {
        return mFilePath;
    }

    public void setmFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    /**
     * 默认文件路径
     */
    public String getDefaultPath(){
        return getContext().getExternalFilesDir("audio").getAbsolutePath();
    }

    /**
     * 默认文件名
     */
    public String getDefaultName(){
        return mPref + UUID.randomUUID() + ".mp3";
    }

    public String getmFileName() {
        return mFileName;
    }

    public void setmFileName(String mFileName) {
        this.mFileName = mFileName;
    }



    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case VOLUME_WHAT_100:
                    long volume = (long) msg.obj;
                    volumeImage.getDrawable().setLevel((int) volume);
                    break;
                case TIME_WHAT_101:
                    long currentTime = SystemClock.currentThreadTimeMillis();
                    int time = (int) (currentTime - mStartTime) / 1000;
                    int min = time / 60;
                    int second = time % 60;
                    if (min > 10){
                        if (second > 10){
                            recordTimeText.setText(min + ":" + second);
                        }else {
                            recordTimeText.setText(min + ":" + "0" + second);
                        }
                    }else {
                        if (second > 10){
                            recordTimeText.setText("0" + min + ":" + second);
                        }else {
                            recordTimeText.setText("0" + min + ":" + "0" + second);
                        }
                    }
                    break;
                case STOP_WHAT_102:
                    stopRecording();
                    break;
            }

        }
    }



    class RecordingThread extends Thread{
        private boolean isRunning = true;

        @Override
        public void run() {
            while (isRunning){
                try {
                    Thread.sleep(200);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                long volume = mMediaRecorder.getMaxAmplitude();
                Message volumeMessage = mHandler.obtainMessage();
                volumeMessage.obj = volume;
                volumeMessage.what = VOLUME_WHAT_100;
                mHandler.sendMessage(volumeMessage);

                long nowTime = SystemClock.currentThreadTimeMillis();
                if (nowTime - mStartTime > MAX_INTERVAL_TIME){
                    mHandler.sendEmptyMessage(STOP_WHAT_102);
                }

                mHandler.sendEmptyMessage(TIME_WHAT_101);



            }
        }

        public void exit(){
            isRunning = false;
        }

    }



}
