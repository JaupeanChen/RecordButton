package com.example.recordbutton;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RecordButton extends AppCompatButton {
    public static final int MAX_INTERVAL_TIME = 60 * 1000;
    public static final int MIN_INTERVAL_TIME = 1000;
    public int MAX_CANCEL_LENGTH = -200;

    public static final int VOLUME_WHAT_100 = 100;
    public static final int TIME_WHAT_101 = 101;
    public static final int STOP_WHAT_102 = 102;

    private String mFilePath;
    private String mFileName;
    private String mFile;

    //文件名前缀
    private int mPref;
    private long mStartTime;
    private long intervalTime;

    private MediaRecorder mMediaRecorder;
    private Dialog mDialog;

    private ImageView volumeImage;
    private TextView recordTimeText;
    private TextView recordTipText;

    private SharedPreferences preferences;

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
        switch (action){
            case MotionEvent.ACTION_DOWN:
                MAX_CANCEL_LENGTH = - getMeasuredHeight() / 2;
                startY = (int) event.getY();
                prepareAndStartRecord();
                break;
            case MotionEvent.ACTION_UP:
                int endY = (int) event.getY();
                if (endY - startY < MAX_CANCEL_LENGTH){
                    cancelRecording();
                }else {
                    finishRecording();
                    preferences = getContext().getSharedPreferences("count",Context.MODE_PRIVATE);
                    mPref += 1;
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("count",mPref);
                    editor.apply();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int nowY = (int) event.getY();
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
        if (mFilePath != null){     //如果初始化时mFilePath = "" ，则要用TextUtil.isEmpty()来判断，也就是isEmpty还可以对s.length()=0进行判断
            tempFilePath = mFilePath;
        }else {
            tempFilePath = getDefaultPath();
        }

        preferences = getContext().getSharedPreferences("count",Context.MODE_PRIVATE);
        mPref = preferences.getInt("count",0);

        String tempFileName;
        if (mFileName != null){
            tempFileName = mFileName;
        }else {
            tempFileName = getDefaultName();
        }

        mFile = tempFilePath + "/" + tempFileName;

        mStartTime = System.currentTimeMillis();

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
        }catch (IOException e){
            e.printStackTrace();
        }

        mMediaRecorder.start();
        showDialog();
        mThread = new RecordingThread();
        mThread.start();

    }

    public void finishRecording(){
        long now = System.currentTimeMillis();
        intervalTime = now - mStartTime;
        //如果时间不到一秒，那就提示然后删掉
        if (intervalTime < MIN_INTERVAL_TIME){
            cancelRecording();
            Toast.makeText(getContext(),"时间太短！",Toast.LENGTH_SHORT).show();
        }else {
            //正常结束录音
            stopRecording();
            Toast.makeText(getContext(),"保存到：" + getFile() + "," + (getIntervalTime() / 1000) + "秒",Toast.LENGTH_LONG).show();

            //进行音频转换为文件

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
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mDialog.isShowing()){
            mDialog.dismiss();
        }

    }

    public void showDialog(){
        if (mDialog == null){
            mDialog = new Dialog(getContext(),R.style.RecordDialog);  //
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.record_dialog_layout, null);
            volumeImage = dialogView.findViewById(R.id.record_volume_image);
            recordTimeText = dialogView.findViewById(R.id.record_time_text);
            recordTipText = dialogView.findViewById(R.id.record_tip_text);
            mDialog.addContentView(dialogView,
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mDialog.show();
        }else {
            mDialog.show();
        }


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
        return String.valueOf(mPref) + "_" + UUID.randomUUID() + ".mp3";
    }

    public String getmFileName() {
        return mFileName;
    }

    public void setmFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public String getFile(){
        return mFile;
    }

    public int getIntervalTime(){
        return  (int) intervalTime;
    }



    class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case VOLUME_WHAT_100:
                    int volume =  (int) msg.obj;
                    if (volumeImage != null){
                        volumeImage.getDrawable().setLevel(3000 + 6000 * volume /90);
                    }
                    break;
                case TIME_WHAT_101:
                    long currentTime = System.currentTimeMillis();
                    int time = (int) ((currentTime - mStartTime) / 1000);
                    int min = time / 60;
                    int second = time % 60;
                    if (min >= 10){
                        if (second >= 10){
                            recordTimeText.setText(min + ":" + second);
                        }else {
                            recordTimeText.setText(min + ":" + "0" + second);
                        }
                    }else {
                        if (second >= 10){
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
                if (mMediaRecorder == null || !isRunning){
                    break;
                }

                long nowTime = System.currentTimeMillis();
                if (nowTime - mStartTime > MAX_INTERVAL_TIME){
                    mHandler.sendEmptyMessage(STOP_WHAT_102);
                }

                mHandler.sendEmptyMessage(TIME_WHAT_101);

                int volume = mMediaRecorder.getMaxAmplitude();
                if (volume != 0){
                    int v = (int) (20 * Math.log(volume) / Math.log(10));
                    Message volumeMessage = mHandler.obtainMessage();
                    volumeMessage.obj = v;
                    volumeMessage.what = VOLUME_WHAT_100;
                    mHandler.sendMessage(volumeMessage);
                }








            }
        }

        public void exit(){
            isRunning = false;
        }

    }



}
