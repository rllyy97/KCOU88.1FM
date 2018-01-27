package fm.kcou.kcou881fm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.ImageButton;
import android.widget.MediaController;

import java.io.IOException;

import static java.lang.Thread.sleep;

// Created by riley on 1/25/2018

public class StreamService extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener {

    MediaPlayer mMediaPlayer = null;
    int currentState = 0; // 0=paused, 1=playing 2=connecting
    WifiManager.WifiLock wifiLock;
    Context context;
    AudioManager am;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    MediaController mediaController;
    MediaSession mMediaSession;
    String sessionID = "881";
    ImageButton playButton;
//    int mNotificationId = 881;

    public void build(final Context context, ImageButton imageButton) {
        this.context = context;
        this.playButton = imageButton;
        mMediaPlayer = new MediaPlayer();
        mediaController = new MediaController(context);
        mediaController.setMediaPlayer(new MediaController.MediaPlayerControl() {
            @Override public void start() {play();}
            @Override public void pause() {stop();}
            @Override public int getDuration() {return 0;}
            @Override public int getCurrentPosition() {return 0;}
            @Override public void seekTo(int pos) {}
            @Override public boolean isPlaying() {return false;}
            @Override public int getBufferPercentage() {return 0;}
            @Override public boolean canPause() {return false;}
            @Override public boolean canSeekBackward() {return false;}
            @Override public boolean canSeekForward() {return false;}
            @Override public int getAudioSessionId() {return 0;}
        });
        mMediaSession = new MediaSession(context,sessionID);
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(String.valueOf(WifiManager.WIFI_MODE_FULL));
            wifiLock.setReferenceCounted(true);
        }
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stop();
                    playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_radio_white_24px, null));
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mMediaPlayer.setVolume(0.2f,0.2f);
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN || focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                    play();
                    playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pause_white_24px, null));
                    mMediaPlayer.setVolume(1.0f,1.0f);
                }
            }
        };
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

    boolean createFocus(){
        int result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    void play() {
        currentState=2;
        initMediaPlayer();
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock.acquire();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.prepareAsync();
        onPrepared(mMediaPlayer);
        if(!createFocus()){stop();}
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        am.abandonAudioFocus(afChangeListener);
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


    MediaSession getmMediaSession(){
        return  mMediaSession;
    }

//    MediaPlayer getMediaPlayer(){
//        return mMediaPlayer;
//    }

}
