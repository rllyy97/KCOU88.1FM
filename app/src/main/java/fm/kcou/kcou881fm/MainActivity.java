package fm.kcou.kcou881fm;
// Author: Riley Evans, started September 13 2017
import android.content.Context;

import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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

    private static final String TAG = "MyActivity";

    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Recent = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
//    String stream2 = "";
//    String stream2Meta = "";

    JSONObject jsonRecent;
    JSONObject jsonArt;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        AsyncRecentMeta getRecentMeta = new AsyncRecentMeta();
        getRecentMeta.execute(stream1Recent);
    }

    public void playPauseStream(final View v) throws JSONException, IOException {
        findViewById(R.id.connectionWarning).setVisibility(View.INVISIBLE);

        if(playing==1){
            mediaPlayer.stop();
            playing = 0;
            ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
            playButton.setBackground(getResources().getDrawable(R.drawable.ic_radio_white_24px));
                // Warning is not being handled to keep support for API 19, resolving requires API 21
            mediaPlayer.reset();
        }

        else if(isNetworkAvailable()&&playing!=2){
            playing = 2;

            ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
            final RotateAnimation rotateAnim = new RotateAnimation(
                    0, 3240, playButton.getWidth()/2, playButton.getHeight()/2);
            rotateAnim.setDuration(2500); // Use 0 ms to rotate instantly
            rotateAnim.setFillAfter(true); // Must be true or the animation will reset
            playButton.startAnimation(rotateAnim);

            try {
                mediaPlayer.setDataSource(stream1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                public void onPrepared(MediaPlayer player){
                    player.start();
                    playing = 1;

                    ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
                    rotateAnim.cancel();
                    rotateAnim.reset();
                    playButton.setBackground(getResources().getDrawable(R.drawable.ic_pause_white_24px));
                        // Warning is not being handled to keep support for API 19, resolving requires API 21
                    new AsyncRecentMeta().execute(stream1Recent);
                    final Timer t = new Timer();
                    t.scheduleAtFixedRate(new TimerTask(){
                        public void run() {
                            if(playing!=0) {
                                new AsyncRecentMeta().execute(stream1Recent);
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


    /////     Android Make API Call for json : Now Playing

    private class AsyncMetaArt extends AsyncTask<Void, Void, Void> {
        ImageView albumArt;
        String title;
        String artist;
        AsyncMetaArt(ImageView img, String title, String artist){
            this.albumArt=img;
            this.title=title;
            this.artist=artist;
        }

        protected Void doInBackground(Void... param) {
            try {
                title = title.replaceAll("^\"|\"$", "");
                artist = artist.replaceAll("^\"|\"$", "");
                String trackSearchURL = "http://ws.audioscrobbler.com/2.0/?method=track.search&track=" + title + "&artist=" + artist + "&api_key=03d2bcf571b5e6ea2c744cf32fd40cb9&format=json&limit=1";
                jsonArt = readJsonFromUrl(trackSearchURL);
                onPostExecute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private int onPostExecute() throws IOException, JSONException {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    try {
                        setArt(albumArt);
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return 1;
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

        private void setArt(ImageView albumArt) throws JSONException, IOException {
            String albumArtURL = jsonArt.getJSONObject("results").getJSONObject("trackmatches").getJSONArray("track").getJSONObject(0).getJSONArray("image").getJSONObject(2).getString("#text");
            Picasso.with(getApplicationContext()).load(albumArtURL).into(albumArt);
        }
    }


    /////     Android Make API Call for json : Recent Tracks

    private class AsyncRecentMeta extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... url) {
            try {
                jsonRecent = readJsonFromUrl(url[0]);
                onPostExecute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Void onPostExecute() throws IOException, JSONException {
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
            String[][] data = new String[5][3];
            for(int i=0;i<5;i++){
                String fullTitle = jsonRecent.getJSONArray("items").getJSONObject(i).getString("title");
                String[] track = fullTitle.split(" - ");
                data[i][0] = track[1];
                data[i][1] = track[0];
                data[i][2] = jsonRecent.getJSONArray("items").getJSONObject(i).getString("description");
            }

            LinearLayout recentTracks = (LinearLayout) findViewById(R.id.slidingUpDrawer);
            for(int j=0;j<5;j++){
                LinearLayout track = (LinearLayout) recentTracks.getChildAt(j);
                LinearLayout metaText = (LinearLayout) track.getChildAt(1);
                for(int k=0;k<3;k++) {
                    TextView text = (TextView) metaText.getChildAt(k);
                    text.setText(data[j][k]);
                }
                new AsyncMetaArt((ImageView)track.getChildAt(0), data[j][0], data[j][1]).execute();

            }
        }
    }

    /////

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
