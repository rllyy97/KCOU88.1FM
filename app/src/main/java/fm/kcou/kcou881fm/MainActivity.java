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
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
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
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/rpc/c8180/streaminfo.get";
    String stream1Recent = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
//    String stream2 = "";
//    String stream2Meta = "";

    JSONObject json;
    JSONObject jsonRecent;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        TextView artistTextView = (TextView) findViewById(R.id.artistTextView);
        TextView albumTextView = (TextView) findViewById(R.id.albumTextView);
        titleTextView.setSelected(true);
        artistTextView.setSelected(true);
        albumTextView.setSelected(true);


        AsyncMeta getMeta = new AsyncMeta();
        getMeta.execute(stream1Meta);
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
                        // Warning is not being handled to keep support for API 19, resolving requires API 21
                    final Timer t = new Timer();
                    t.scheduleAtFixedRate(new TimerTask(){
                        public void run() {
                            if(playing!=0) {
                                AsyncMeta getMeta = new AsyncMeta();
                                getMeta.execute(stream1Meta);
                                AsyncRecentMeta getRecentMeta = new AsyncRecentMeta();
                                getRecentMeta.execute(stream1Recent);
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
            TextView artistTextView = (TextView) findViewById(R.id.artistTextView);
            TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
            TextView albumTextView = (TextView) findViewById(R.id.albumTextView);
            ImageView albumArt = (ImageView) findViewById(R.id.albumArt);
            artistTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("artist"));
            titleTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("title"));
            albumTextView.setText(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("album"));
            String albumArtURL = json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("imageurl");
            if (albumArtURL.equals("http://sc7.shoutcaststreaming.us:2197/static/c8180/covers/nocover.png")){
                Picasso.with(getApplicationContext()).load(R.drawable.no_cover).into(albumArt);
            }
            else{
                Picasso.with(getApplicationContext()).load(json.getJSONArray("data").getJSONObject(0).getJSONObject("track").getString("imageurl")).into(albumArt);
            }
        }
    }

    /////

    /////     Android Make API Call for json

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
            String[][] data = new String[4][3];
            for(int i=0;i<4;i++){
                String fullTitle = jsonRecent.getJSONArray("items").getJSONObject(i+1).getString("title");
                String[] track = fullTitle.split(" - ");
                data[i][0] = track[1];
                data[i][1] = track[0];
                data[i][2] = jsonRecent.getJSONArray("items").getJSONObject(i+1).getString("description");
            }

            LinearLayout recentTracks = (LinearLayout) findViewById(R.id.whatWasThatRecent);
            for(int j=0;j<4;j++){
                View v = recentTracks.getChildAt(j);
                LinearLayout l = (LinearLayout) v;
                for(int k=0;k<3;k++){
                    View t = l.getChildAt(k);
                    TextView text = (TextView) t;
                    text.setText(data[j][k]);
                }
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
