package fm.kcou.kcou881fm;
// Author: Riley Evans, started September 13 2017

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
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

    StreamService stream;

//    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Recent = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
//    String stream2 = "";
//    String stream2Meta = "";

    JSONObject jsonRecent;
    JSONObject jsonArt;

    ImageView bigArt;
    ConstraintLayout mainContent;
    ArrayList<Song> recentSongsArrayList;
    Palette palette;
    ImageButton playButton;
    int mNotificationId = 881;
    NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playButton = (ImageButton) findViewById(R.id.playButton);
        stream = new StreamService();
        if(isNetworkAvailable()){
            getInitialSongList();
            stream.build(getBaseContext(),playButton);
            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask(){
                public void run() {
                    new AsyncGetTopTrack().execute(stream1Recent);
                    buildNotification();
                }
            }, 15000, 15000);
        } else {
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
        }

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
        if(stream.getState() == 1){
            spin();
            stream.stop();
        }
        else if(isNetworkAvailable()&&stream.getState()!=2){
            spin();
            stream.play();
            buildNotification();
        }
        else if(!isNetworkAvailable()){
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask(){
                public void run() {
                    if(!isNetworkAvailable()){
                        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
                    } else {
                        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_radio_white_24px, null));
                        t.cancel();
                        t.purge();
                    }
                }
            }, 5000, 5000);
        }
    }

    protected void spin(){
        final RotateAnimation rotateAnimateUp = new RotateAnimation(
                0, 360, playButton.getWidth()/2, playButton.getHeight()/2);
        rotateAnimateUp.setDuration(250); // Use 0 ms to rotate instantly
        rotateAnimateUp.setFillAfter(true); // Must be true or the animation will reset
        rotateAnimateUp.setInterpolator(new AccelerateInterpolator());
        rotateAnimateUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation){}
            @Override
            public void onAnimationEnd(Animation animation) {
                rotateAnimateUp.cancel();
                rotateAnimateUp.reset();
                if(stream.getState()==1)playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_white_24px, null));
                else playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_radio_white_24px, null));
                final RotateAnimation rotateAnimateDown = new RotateAnimation(
                        0, 360, playButton.getWidth()/2, playButton.getHeight()/2);
                rotateAnimateDown.setDuration(250); // Use 0 ms to rotate instantly
                rotateAnimateDown.setFillAfter(true); // Must be true or the animation will reset
                rotateAnimateDown.setInterpolator(new DecelerateInterpolator());
                rotateAnimateDown.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {
                        rotateAnimateDown.cancel();
                        rotateAnimateDown.reset();
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
                playButton.startAnimation(rotateAnimateDown);
            }

            @Override
            public void onAnimationRepeat(Animation animation){}
        });
        playButton.startAnimation(rotateAnimateUp);
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

    @SuppressLint("StaticFieldLeak")
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
                    if(recentSongsArrayList.get(trackNumber).getArtist().equals("ID/PSA")){
                        setDefualtImage(artistIcon);
                    } else if(albumArtURL == null) {
                        setDefualtImage(artistIcon);
                    } else {
                        Picasso.with(getBaseContext()).load(albumArtURL).into(artistIcon, new Callback.EmptyCallback() {
                            @Override public void onSuccess() {
                                assignArt();
                            }
                            @Override public void onError() {}
                        });
                    }
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
                    setDefualtImage(bigArt);
                } else if(albumArtURL == null) {
                    setDefualtImage(bigArt);
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

    void setDefualtImage(ImageView imageView){
        Picasso.with(getBaseContext()).load(R.drawable.background).config(Bitmap.Config.RGB_565).fit().centerCrop().into(imageView, new Callback.EmptyCallback() {
            @Override
            public void onSuccess() {colorEverything(getColor(R.color.colorPrimaryLight));}
            @Override
            public void onError() {colorEverything(getColor(R.color.colorPrimaryLight));}
        });
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

    @SuppressLint("StaticFieldLeak")
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

        private void onPostExecute() throws IOException, JSONException {
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
                    new AsyncMetaArt(null, title, artist, i).execute();
                    al.add(new Song(title,artist,album,null,time));
                }
            }
            recentSongsArrayList = al;
            setSongList();
        }
    }

    ///// Get Top Track

    @SuppressLint("StaticFieldLeak")
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

        private void onPostExecute() throws IOException, JSONException {
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

                java.util.Date oldTime = new java.util.Date(Long.parseLong(recentSongsArrayList.get(0).getTime())*1000);
                java.util.Date newTime = new java.util.Date((Long.parseLong(time))*1000);
                if(oldTime.before(newTime)){
                    recentSongsArrayList.add(0,new Song(title, artist, album, null, time));
                    new AsyncMetaArt(null, title, artist, 0).execute();
                } else { return; }


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
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /////

    void buildNotification(){
        // The id of the channel.
//        String CHANNEL_ID = "stream_notification_channel";
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_radio_white_24px)
                        .setContentTitle(recentSongsArrayList.get(0).getTitle())
                        .setContentText(recentSongsArrayList.get(0).getArtist())
                        .setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(1 /* #1: pause button */)
                                .setMediaSession(MediaSessionCompat.Token.fromToken(stream.getmMediaSession().getSessionToken())));
        Intent resultIntent = new Intent(getBaseContext(),this.getClass());
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(getBaseContext(), 0,
                        resultIntent, 0);
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }
}
