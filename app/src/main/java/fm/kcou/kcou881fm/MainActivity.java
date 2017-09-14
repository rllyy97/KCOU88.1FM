package fm.kcou.kcou881fm;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    String url = "http://radio.kcou.fm:8180/stream"; // your URL here
    MediaPlayer mediaPlayer = new MediaPlayer();
    boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    public void playPauseStream(View v){
        if(playing){
            mediaPlayer.stop();
            playing = false;
            mediaPlayer.reset();
        }
        else{
            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                public void onPrepared(MediaPlayer player){
                    player.start();
                    playing = true;
                }
            });
        }

    }

}
