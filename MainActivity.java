package com.example.srv.audiomain;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnAudioStreamInterface, SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private Button mPlayButton = null;
    private Button mStopButton = null;

    private TextView mTextCurrentTime = null;
    private TextView mTextDuration = null;

    private SeekBar mSeekProgress = null;

    private ProgressDialog mProgressDialog = null;

    AudioStreamPlayer mAudioPlayer = null;

    FileDescriptor fileDescriptor = null;

    AssetFileDescriptor assetFileDescriptor = null;

    ArrayList<Music> musicList =null;

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};//권한 설정 변수
    private static final int MULTIPLE_PERMISSIONS = 101;//권한 동의 여부 문의 후 callback함수에 쓰일 변수

    VisualizerView visualizerView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayButton = (Button) this.findViewById(R.id.button_play);
        mPlayButton.setOnClickListener(this);
        mStopButton = (Button) this.findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(this);

        mTextCurrentTime = (TextView) findViewById(R.id.text_pos);
        mTextDuration = (TextView) findViewById(R.id.text_duration);

        mSeekProgress = (SeekBar) findViewById(R.id.seek_progress);
        mSeekProgress.setOnSeekBarChangeListener(this);
        mSeekProgress.setMax(0);
        mSeekProgress.setProgress(0);

        visualizerView = this.findViewById(R.id.visualizerView);

        if(checkPermissions()) {
            FileLoad file = new FileLoad(this);
            musicList = file.getMusicList();
        }

        updatePlayer(AudioStreamPlayer.State.Stopped);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        stop();
    }

    private void updatePlayer(AudioStreamPlayer.State state)
    {
        switch (state)
        {
            case Stopped:
            {
                if (mProgressDialog != null)
                {
                    mProgressDialog.cancel();
                    mProgressDialog.dismiss();

                    mProgressDialog = null;
                }
                mPlayButton.setSelected(false);
                mPlayButton.setText("Play");

                mTextCurrentTime.setText("00:00");
                mTextDuration.setText("00:00");

                mSeekProgress.setMax(0);
                mSeekProgress.setProgress(0);

                break;
            }
            case Prepare:
            case Buffering:
            {
                if (mProgressDialog == null)
                {
                    mProgressDialog = new ProgressDialog(this);
                }
                mProgressDialog.show();

                mPlayButton.setSelected(false);
                mPlayButton.setText("Play");

                mTextCurrentTime.setText("00:00");
                mTextDuration.setText("00:00");
                break;
            }
            case Pause:
            {
                break;
            }
            case Playing:
            {
                if (mProgressDialog != null)
                {
                    mProgressDialog.cancel();
                    mProgressDialog.dismiss();

                    mProgressDialog = null;
                }
                mPlayButton.setSelected(true);
                mPlayButton.setText("Pause");
                break;
            }
        }
    }

    private void pause()
    {
        if (this.mAudioPlayer != null)
        {
            this.mAudioPlayer.pause();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void play()
    {
        releaseAudioPlayer();

        mAudioPlayer = new AudioStreamPlayer();
        mAudioPlayer.setOnAudioStreamInterface(this);
        mAudioPlayer.setAssetFileDescriptor(this.assetFileDescriptor);

        //Music music = musicList.get(0);
        //mAudioPlayer.setUrlString(music.getFilePath());
        try{
            mAudioPlayer.play();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void releaseAudioPlayer()
    {
        if (mAudioPlayer != null)
        {
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;

        }
    }

    private void stop()
    {
        if (this.mAudioPlayer != null)
        {
            this.mAudioPlayer.stop();
        }
    }

    @Override
    public void onAudioPlayerStart(AudioStreamPlayer player)
    {
        runOnUiThread(new Runnable()
        {

            @Override
            public void run()
            {
                updatePlayer(AudioStreamPlayer.State.Playing);
            }
        });
    }

    @Override
    public void onAudioPlayerStop(AudioStreamPlayer player)
    {
        runOnUiThread(new Runnable()
        {

            @Override
            public void run()
            {
                updatePlayer(AudioStreamPlayer.State.Stopped);
            }
        });

    }

    @Override
    public void onAudioPlayerError(AudioStreamPlayer player)
    {
        runOnUiThread(new Runnable()
        {

            @Override
            public void run()
            {
                updatePlayer(AudioStreamPlayer.State.Stopped);
            }
        });

    }

    @Override
    public void onAudioPlayerBuffering(AudioStreamPlayer player)
    {
        runOnUiThread(new Runnable()
        {

            @Override
            public void run()
            {
                updatePlayer(AudioStreamPlayer.State.Buffering);
            }
        });

    }

    @Override
    public void onAudioPlayerDuration(final int totalSec)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (totalSec > 0)
                {
                    int min = totalSec / 60;
                    int sec = totalSec % 60;

                    mTextDuration.setText(String.format("%02d:%02d", min, sec));

                    mSeekProgress.setMax(totalSec);
                }
            }

        });
    }

    @Override
    public void onAudioPlayerCurrentTime(final int sec)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (!isSeekBarTouch)
                {
                    int m = sec / 60;
                    int s = sec % 60;

                    mTextCurrentTime.setText(String.format("%02d:%02d", m, s));

                    mSeekProgress.setProgress(sec);
                }
            }
        });
    }

    @Override
    public void onAudioSetPcmData(final byte[] pcm) {

        //visualizerView.updateVisualizer(pcm);

        //Log.d("", byteArrayToHex(pcm));

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                visualizerView.updateVisualizer(pcm);
            }
        });


    }

    @Override
    public void onAudioPlayerPause(AudioStreamPlayer player)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mPlayButton.setText("Play");
            }
        });
    }

    private boolean isSeekBarTouch = false;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        this.isSeekBarTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        this.isSeekBarTouch = false;

        int progress = seekBar.getProgress();

        this.mAudioPlayer.seekTo(progress);
    }


    public int getRawResIdByName(String resName) {
        String pkgName = this.getPackageName();
        // Return 0 if not found.
        int resID = this.getResources().getIdentifier(resName, "raw", pkgName);
        Log.i("AndroidVideoView", "Res Name: " + resName + "==> Res ID = " + resID);
        return resID;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.button_play:{
                if (mPlayButton.isSelected()){
                    if (mAudioPlayer != null && mAudioPlayer.getState() == AudioStreamPlayer.State.Pause){
                        mAudioPlayer.pauseToPlay();
                    }else{
                        pause();
                    }
                }else{

                    //Music music = musicList.get(0);


                    AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.test);

                    if(fd == null){
                        Log.d("AssetFileDescriptor = ", "fd null....");
                    }
                    this.assetFileDescriptor = fd;
                    play();


                }
                break;
            }
            case R.id.button_stop:
            {
                stop();
                break;
            }
        }
    }


    private boolean checkPermissions() {//사용권한 묻는 함수
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);//현재 컨텍스트가 pm 권한을 가졌는지 확인
            if (result != PackageManager.PERMISSION_GRANTED) {//사용자가 해당 권한을 가지고 있지 않을 경우
                permissionList.add(pm);//리스트에 해당 권한명을 추가한다
            }
        }
        if (!permissionList.isEmpty()) {//권한이 추가되었으면 해당 리스트가 empty가 아니므로, request 즉 권한을 요청한다.
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        if(a != null && a.length > 0) {
            for (final byte b : a) {
                sb.append(String.format("%02x ", b & 0xff));
            }
            return sb.toString();
        }else {
            return "no hex";
        }
    }
}
