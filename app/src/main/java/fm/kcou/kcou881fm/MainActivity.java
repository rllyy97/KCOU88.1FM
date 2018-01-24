package fm.kcou.kcou881fm;
// Author: Riley Evans, started September 13 2017

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;

//import android.util.Log;

public class MainActivity extends AppCompatActivity {

//    private static final String TAG = "MyActivity";

    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Recent = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
//    String stream2 = "";
//    String stream2Meta = "";

    JSONObject jsonRecent;
    JSONObject jsonArt;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    ImageView bigArt;
    ConstraintLayout mainContent;
    ArrayList<Song> recentSongsArrayList;
    Palette palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(isNetworkAvailable()){
            getInitialSongList();

            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask(){
                public void run() {
                    new AsyncGetTopTrack().execute(stream1Recent);
                }
            }, 30000, 30000);
        } else {
            ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        bigArt = (ImageView) findViewById(R.id.bigArt);
        mainContent = (ConstraintLayout)findViewById(R.id.mainContent);

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

//        float density  = getResources().getDisplayMetrics().density;
        float dpWidth  = outMetrics.widthPixels;

        bigArt.setLayoutParams(new ConstraintLayout.LayoutParams((int)dpWidth,(int)dpWidth));

        bigArt.setMaxHeight((int)dpWidth);
        bigArt.setMaxWidth((int)dpWidth);
        bigArt.setMinimumHeight((int)dpWidth);
        bigArt.setMinimumWidth((int)dpWidth);

    }

    public void playPauseStream(final View v) throws JSONException, IOException {
        ImageButton playButton = (ImageButton) findViewById(R.id.playButton);
        if(playing==1){
            mediaPlayer.stop();
            playing = 0;
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_radio_white_24px, null));
            mediaPlayer.reset();
        }

        else if(isNetworkAvailable()&&playing!=2){
            playing = 2;
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
                    playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_white_24px, null));

                }
            });
        }
        else if(!isNetworkAvailable()){
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
            playing = 0;
        }
    }

    protected void getInitialSongList(){
        AsyncGetInitialSongList getRecentMeta = new AsyncGetInitialSongList();
        getRecentMeta.execute(stream1Recent);
    }

    protected void setSongList(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                LinearLayout recentTracks = (LinearLayout) findViewById(R.id.slidingUpDrawer);
                int j=0;
                for(Song song : recentSongsArrayList){
                    LinearLayout track = (LinearLayout) recentTracks.getChildAt(j+1);
                    LinearLayout metaText = (LinearLayout) track.getChildAt(1);

                    TextView title = (TextView)metaText.getChildAt(0);
                    title.setText(song.getTitle());
                    TextView artist = (TextView)metaText.getChildAt(1);
                    artist.setText(song.getArtist());
                    TextView album = (TextView)metaText.getChildAt(2);
                    album.setText(song.getAlbum());

                    TextView timeStamp = (TextView) track.getChildAt(2);
                    long unixTime = Long.parseLong(song.getTime());
                    java.util.Date time = new java.util.Date(unixTime*1000);
                    DateFormat df = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
                    timeStamp.setText(df.format(time));

                    ImageView img = (ImageView) track.getChildAt(0);
                    img.setImageBitmap(song.getImage());

                    j++;
                }
                Song currentSong = recentSongsArrayList.get(0);
                TextView currentTitle = (TextView)findViewById(R.id.currentTitle);
                currentTitle.setText(currentSong.getTitle());
                TextView currentArtist = (TextView)findViewById(R.id.currentArtist);
                currentArtist.setText(currentSong.getArtist());
                TextView currentAlbum = (TextView)findViewById(R.id.currentAlbum);
                currentAlbum.setText(currentSong.getAlbum());
            }
        });
    }

    /////     Android Make API Call for json : Artist Image

    private class AsyncMetaArt extends AsyncTask<Void, Void, Void> {
        Bitmap albumArt;
        String title;
        String artist;
        int trackNumber;
        AsyncMetaArt(Bitmap img, String title, String artist, int trackNumber){
            this.albumArt=img;
            this.title=title;
            this.artist=artist;
            this.trackNumber=trackNumber;
        }

        protected Void doInBackground(Void... param) {
            try {
                artist = artist.replaceAll("^\"|\"$", "");
                String trackSearchURL = "http://ws.audioscrobbler.com/2.0/?method=artist.search&artist=" + artist + "&api_key=03d2bcf571b5e6ea2c744cf32fd40cb9&format=json&limit=1";
                jsonArt = readJsonFromUrl(trackSearchURL);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                try {
                    setArt();
                    if(trackNumber==0)setBigArt();
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

        private void setArt() throws JSONException, IOException {
            String albumArtURL = jsonArt.getJSONObject("results").getJSONObject("artistmatches").getJSONArray("artist").getJSONObject(0).getJSONArray("image").getJSONObject(2).getString("#text");
            try{
                if(recentSongsArrayList.get(trackNumber).getImage() == null){
                    LinearLayout recentTracks = (LinearLayout) findViewById(R.id.slidingUpDrawer);
                    LinearLayout track = (LinearLayout) recentTracks.getChildAt(trackNumber+1);
                    ImageView artistIcon = (ImageView) track.getChildAt(0);
                    Picasso.with(getBaseContext()).load(albumArtURL).into(artistIcon, new Callback.EmptyCallback() {
                        @Override public void onSuccess() {
                            assignArt();
                        }
                        @Override
                        public void onError() {
                        }
                    });
                } else {
                    LinearLayout recentTracks = (LinearLayout) findViewById(R.id.slidingUpDrawer);
                    LinearLayout track = (LinearLayout) recentTracks.getChildAt(trackNumber+1);
                    ImageView artistIcon = (ImageView) track.getChildAt(0);
                    artistIcon.setImageBitmap(recentSongsArrayList.get(trackNumber).getImage());
                }

            } catch (Exception e){
//                albumArt = ((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.no_cover, null)).getBitmap();
            }
        }

        private void assignArt(){
            LinearLayout recentTracks = (LinearLayout) findViewById(R.id.slidingUpDrawer);
            LinearLayout track = (LinearLayout) recentTracks.getChildAt(trackNumber+1);
            ImageView artistIcon = (ImageView) track.getChildAt(0);
            albumArt = ((BitmapDrawable)artistIcon.getDrawable()).getBitmap();
            recentSongsArrayList.get(trackNumber).setImage(albumArt);
        }

        private void setBigArt() throws JSONException, IOException {
            String albumArtURL = jsonArt.getJSONObject("results").getJSONObject("artistmatches").getJSONArray("artist").getJSONObject(0).getJSONArray("image").getJSONObject(2).getString("#text");
            albumArtURL = albumArtURL.replaceAll("174s","1080x1080");
            try{
                if(recentSongsArrayList.get(0).getArtist().equals("ID/PSA")){
                    Picasso.with(getBaseContext()).load(R.drawable.background).into(bigArt, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            colorEverything(R.color.colorAccent);
                        }
                        @Override
                        public void onError() {
                            colorEverything(R.color.colorAccent);
                        }
                    });
                } else if(albumArtURL == null) {
                    Picasso.with(getBaseContext()).load(R.drawable.background).into(bigArt, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            colorEverything(R.color.colorAccent);
                        }
                        @Override
                        public void onError() {
                            colorEverything(R.color.colorAccent);
                        }
                    });
                } else {
                    Picasso.with(getBaseContext()).load(albumArtURL).into(bigArt, new Callback.EmptyCallback() {
                        @Override public void onSuccess() {

                            Bitmap big = ((BitmapDrawable)bigArt.getDrawable()).getBitmap();
                            palette = Palette.from(big).generate();
                            int vibrant = palette.getVibrantColor(0);
                            Palette.Swatch darkVibrant = palette.getDarkVibrantSwatch();
                            Palette.Swatch mutedVibrant = palette.getDarkMutedSwatch();
                            if(vibrant != 0) {
                                colorEverything(vibrant);
                            } else if (darkVibrant != null) {
                                colorEverything(darkVibrant.getRgb());
                            } else if (mutedVibrant != null) {
                                colorEverything(mutedVibrant.getRgb());
                            } else {
                                colorEverything(Color.BLACK);
                            }

                        }
                        @Override
                        public void onError() {
                            colorEverything(R.color.colorAccent);
                        }
                    });
                }


            } catch (Exception e){
                bigArt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.no_cover, null));
            }
        }
    }

    private int makeTransparent(int color){
        int alpha;
        int red = 0xFF & ( color >> 16);
        int green = 0xFF & (color >> 8 );
        int blue = 0xFF & (color);

        alpha = 0;
        return (alpha << 24) | (red << 16 ) | (green<<8) | blue;
    }

    private void colorEverything(int color){
        mainContent.setBackgroundColor(color);
        bigArt.setForeground(generateGradient(color));
    }

    private GradientDrawable generateGradient(int color){
        int tColor = makeTransparent(color);
        GradientDrawable gd = new GradientDrawable();
        int colors[] = new int[3];
        colors[0] = tColor;
        colors[1] = tColor;
        colors[2] = color;
        gd.setColors(colors);
        gd.setOrientation(TOP_BOTTOM);
        return gd;
    }

    /////     Android Make API Call for json : Recent Tracks

    private class AsyncGetInitialSongList extends AsyncTask<String, Void, Void> {
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
                    } catch (JSONException | IOException | InterruptedException e) {
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

        private void setStreamMeta() throws JSONException, IOException, InterruptedException {
            ArrayList<Song> al = new ArrayList<>();
            if(jsonRecent == null)
                return;
            for(int i=0;i<6;i++) {
                if(jsonRecent.getJSONArray("items").getJSONObject(i) == null){
                    al.add(new Song("","N/A","", null,""));
                }
                else {
                    String fullTitle = jsonRecent.getJSONArray("items").getJSONObject(i).getString("title");                // java.lang.NullPointerException: Attempt to invoke virtual method 'org.json.JSONArray org.json.JSONObject.getJSONArray(java.lang.String)' on a null object reference
                    String[] track = fullTitle.split(" - ");
                    String title = track[1];
                    String artist = track[0];
                    String album = jsonRecent.getJSONArray("items").getJSONObject(i).getString("description");
                    String time = jsonRecent.getJSONArray("items").getJSONObject(i).getString("date");
//                    Bitmap image = ((BitmapDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.no_cover, null)).getBitmap();
                    new AsyncMetaArt(null, title, artist, i).execute();
                    al.add(new Song(title,artist,album,null,time));
                }
            }
            recentSongsArrayList = al;
            setSongList();
        }
    }

    ///// Get Top Track

    private class AsyncGetTopTrack extends AsyncTask<String, Void, Void> {
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
            if(jsonRecent == null)
                return;
            if(jsonRecent.getJSONArray("items").getJSONObject(0) == null){                                              // java.lang.NullPointerException: Attempt to invoke virtual method 'org.json.JSONArray org.json.JSONObject.getJSONArray(java.lang.String)' on a null object reference
                recentSongsArrayList.add(0,new Song("N/A","N/A","",null,""));
            }
            else {
                String fullTitle = jsonRecent.getJSONArray("items").getJSONObject(0).getString("title");
                String[] track = fullTitle.split(" - ");
                String title = track[1];
                String artist = track[0];
                String album = jsonRecent.getJSONArray("items").getJSONObject(0).getString("description");
                String time = jsonRecent.getJSONArray("items").getJSONObject(0).getString("date");
                if(!recentSongsArrayList.get(0).getTitle().equals(title)){
                    recentSongsArrayList.add(0,new Song(title, artist, album, null, time));
                    new AsyncMetaArt(null, title, artist, 0).execute();
                }


            }
            while(recentSongsArrayList.size() > 6){
                recentSongsArrayList.remove(6);
            }
            setSongList();
        }
    }

    ///// Network Check

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /////

}
