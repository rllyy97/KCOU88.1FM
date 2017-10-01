package fm.kcou.kcou881fm;
// Author: Riley Evans, started September 13 2017
import android.content.Context;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/rpc/c8180/streaminfo.get";
//    String stream2 = "";
//    String stream2Meta = "";

    JSONObject json;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    public void playPauseStream(final View v) throws JSONException, IOException {
        findViewById(R.id.connectionWarning).setVisibility(View.INVISIBLE);

        if(playing==1){
            mediaPlayer.stop();
            playing = 0;
            ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
            playButton.setBackground(getResources().getDrawable(R.drawable.ic_radio_white_24px));
            mediaPlayer.reset();
        }

        else if(isNetworkAvailable()&&playing!=2){
            playing = 2;

            try {
                mediaPlayer.setDataSource(stream1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
            TextView artistTextView = (TextView) findViewById(R.id.artistTextView);
            TextView albumTextView = (TextView) findViewById(R.id.albumTextView);
            titleTextView.setText("");
            artistTextView.setText(R.string.buffering);
            albumTextView.setText("");

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                public void onPrepared(MediaPlayer player){
                    player.start();
                    playing = 1;
                    ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
                    playButton.setBackground(getResources().getDrawable(R.drawable.ic_pause_white_24px));
                    final Timer t = new Timer();
                    t.scheduleAtFixedRate(new TimerTask(){
                        public void run() {
                            if(playing!=0) {
                                AsyncMeta getMeta = new AsyncMeta();
                                getMeta.execute(stream1Meta);
                            }
                            else {
                                t.purge();
                                cancel();
                            }
                        }
                    }, 0, 60000);
                }
            });
        }
        else if(!isNetworkAvailable()){
            findViewById(R.id.connectionWarning).setVisibility(View.VISIBLE);
            playing = 0;
        }

    }

    /////     Android Make API Call for json

    private class AsyncMeta extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... url) {
            try {
                json = readJsonFromUrl(url[0]);
                onPostExecute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected Void onPostExecute() throws IOException, JSONException {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    try {
                        setStreamMeta();
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }

        private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
            try {
                InputStream is = new URL(url).openStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                is.close();
                return json;
            } catch (IOException ex){
                return null;
            }
        }

        private String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {sb.append((char) cp);}
            return sb.toString();
        }

        private void setStreamMeta() throws JSONException, IOException {
            TextView artistTextView = (TextView) findViewById(R.id.artistTextView);
            TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
            TextView albumTextView = (TextView) findViewById(R.id.albumTextView);
            artistTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("artist"));
            titleTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("title"));
            albumTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("album"));
        }
    }

    /////

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
