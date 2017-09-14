package fm.kcou.kcou881fm;
// Author: Riley Evans, started September 13 2017
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {

    String stream1 = "http://radio.kcou.fm:8180/stream"; // your URL here
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/rpc/c8180/streaminfo.get?x=1.xml";
    String stream2 = "";
    String stream2Meta = "";


    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    public MainActivity() throws ParserConfigurationException, IOException, SAXException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if(!isNetworkAvailable()){
            findViewById(R.id.connectionWarning).setVisibility(View.VISIBLE);
        }
    }

    public void playPauseStream(final View v){
        findViewById(R.id.connectionWarning).setVisibility(View.INVISIBLE);
        if(playing==1){
            mediaPlayer.stop();
            playing = 0;
            mediaPlayer.reset();
            TextView playPauseButton = (TextView) findViewById(R.id.playButton);
            playPauseButton.setText("Play Music");
        }
        else if(isNetworkAvailable()){
            try {
                mediaPlayer.setDataSource(stream1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            TextView playPauseButton = (TextView) findViewById(R.id.playButton);
            playPauseButton.setText("Buffering");
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                public void onPrepared(MediaPlayer player){
                    player.start();
                    playing = 1;
                    TextView playPauseButton = (TextView) findViewById(R.id.playButton);
                    playPauseButton.setText("Pause Music");
//                    try {
//                        getMetaData(v);
//                    } catch (ParserConfigurationException e) {
//                        e.printStackTrace();
//                    }
                }
            });
        }
        else{
            findViewById(R.id.connectionWarning).setVisibility(View.VISIBLE);
            playing = 0;
        }

    }

//    DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newInstance();
//    DocumentBuilder db1 = dbf1.newDocumentBuilder();
//    Document metaDoc1 = db1.parse(new URL(stream1Meta).openStream()); // Causing crash on app open
//
//    public void getMetaData(View v) throws ParserConfigurationException {
//        NodeList trackList = metaDoc1.getElementsByTagName("track");
//        Element artist = (Element) trackList.item(0);
//        Element title = (Element) trackList.item(1);
//        Element album = (Element) trackList.item(2);
//
//        TextView artistTextView = (TextView) findViewById(R.id.artistTextView);
//        artistTextView.setText(artist.getTagName());
//        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
//        titleTextView.setText(title.getTagName());
//        TextView albumTextView = (TextView) findViewById(R.id.albumTextView);
//        albumTextView.setText(album.getTagName());
//    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
