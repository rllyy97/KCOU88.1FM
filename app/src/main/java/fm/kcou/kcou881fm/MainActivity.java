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
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.URL;
import java.nio.charset.Charset;



public class MainActivity extends AppCompatActivity {

    String stream1 = "http://radio.kcou.fm:8180/stream"; // your URL here
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/rpc/c8180/streaminfo.get"; ////////cut the string, take only what is after the ???
//    String stream2 = "";
//    String stream2Meta = "";


    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting
//
//    public MainActivity() throws ParserConfigurationException, IOException, SAXException {
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        if(!isNetworkAvailable()) {
//            findViewById(R.id.connectionWarning).setVisibility(View.VISIBLE);
//        }
        MyAsyncTask getMeta = new MyAsyncTask();
        getMeta.execute(stream1Meta);

    }

    public void playPauseStream(final View v){
        findViewById(R.id.connectionWarning).setVisibility(View.INVISIBLE);
        if(playing==1){
            mediaPlayer.stop();
            playing = 0;
            mediaPlayer.reset();
            TextView playPauseButton = (TextView) findViewById(R.id.playButton);
            playPauseButton.setText(getString(R.string.play));
        }
        else if(isNetworkAvailable()&&playing!=2){
            playing = 2;
            TextView playPauseButton = (TextView) findViewById(R.id.playButton);
            playPauseButton.setText(getString(R.string.buffering));
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
                    TextView playPauseButton = (TextView) findViewById(R.id.playButton);
                    playPauseButton.setText(getString(R.string.pause));

                }
            });
        }
        else if(!isNetworkAvailable()){
            findViewById(R.id.connectionWarning).setVisibility(View.VISIBLE);
            playing = 0;
        }

    }

    /////     Android Make API Call for json

    private class MyAsyncTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... url) {
            JSONObject json = null;
            try {
                json = readJsonFromUrl(url[0]);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            try {
                assert json != null;
                System.out.println(json.get("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
            try {
                InputStream is = new URL(url).openStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
//                jsonText = jsonText.split("?")[1];
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
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

    }



    /////

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
