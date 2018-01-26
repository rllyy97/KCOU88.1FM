package fm.kcou.kcou881fm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;

// Created by riley on 1/25/2018

public class StreamService extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener {
//    private static final String ACTION_PLAY = "com.example.action.PLAY";
//    private static final String ACTION_STOP = "com.example.action.STOP";
    MediaPlayer mMediaPlayer = null;
    int currentState = 0; // 0=paused, 1=playing 2=connecting
    WifiManager.WifiLock wifiLock;
    Context context;

    public void build(Context context) {
        this.context = context;
        mMediaPlayer = new MediaPlayer();

        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(String.valueOf(WifiManager.WIFI_MODE_FULL));
            wifiLock.setReferenceCounted(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initMediaPlayer() {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource("http://radio.kcou.fm:8180/stream");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnErrorListener(this);
    }

    void play() {
        currentState=2;
        initMediaPlayer();
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock.acquire();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.prepareAsync();
        onPrepared(mMediaPlayer);
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        currentState=1;
    }

    void stop() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
//        mMediaPlayer.release();
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
        currentState=0;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        wifiLock.release();
        return false;
    }

    public void onDestroy() {
        if (currentState == 1) stop();
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

    int getState(){
        return currentState;
    }

//    MediaPlayer getMediaPlayer(){
//        return mMediaPlayer;
//    }

}
