package fm.kcou.kcou881fm;

import android.graphics.Bitmap;

class Song {

    private String title;
    private String artist;
    private String album;
    private Bitmap image;
    private String time;

    Song(String title, String artist, String album, Bitmap image, String time){
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.image = image;
        this.time = time;
    }

    String getTitle(){
        return this.title;
    }

    String getArtist(){
        return this.artist;
    }

    String getAlbum(){
        return this.album;
    }

    Bitmap getImage(){
        return this.image;
    }

    String getTime(){
        return this.time;
    }

    void setImage(Bitmap image){
        this.image = image;
    }

}
