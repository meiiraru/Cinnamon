package cinnamon.sound;

import cinnamon.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class Lyrics {

    private String
            title = "",   //ti
            artist = "",  //ar
            album = "",   //al
            author = "",  //au
            by = "",      //by
            tool = "",    //re / tool
            version = ""; //ve

    private int
            length = 0, //length
            offset = 0; //offset

    private final List<Pair<Integer, String>> lyrics = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<Pair<Integer, String>> getLyrics() {
        return lyrics;
    }
}