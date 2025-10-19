package com.example.meumusicplayer.model;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String data; // Caminho para o arquivo
    private final long duration;

    public Song(long id, String title, String artist, String data, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.data = data;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getData() {
        return data;
    }

    public long getDuration() {
        return duration;
    }
}