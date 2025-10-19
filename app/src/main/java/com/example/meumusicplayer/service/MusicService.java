package com.example.meumusicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.meumusicplayer.MainActivity;
import com.example.meumusicplayer.R;
import com.example.meumusicplayer.model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private final IBinder binder = new MusicBinder();
    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
    @Nullable @Override public IBinder onBind(Intent intent) { return binder; }

    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentSongIndex = -1;
    private boolean isShuffle = false;
    private RepeatMode repeatMode = RepeatMode.NONE;
    private final Random random = new Random();

    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_PLAY_PAUSE = "com.example.meumusicplayer.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.meumusicplayer.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.meumusicplayer.ACTION_PREVIOUS";

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);

        mediaSession = new MediaSessionCompat(this, "MusicService");
        updatePlaybackState(false);
        mediaSession.setActive(true);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createEmptyNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;

        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY_PAUSE: playPause(); break;
            case ACTION_NEXT: playNextSong(); break;
            case ACTION_PREVIOUS: playPreviousSong(); break;
        }
        return START_STICKY;
    }

    public void setSongList(List<Song> songs) { this.songList = new ArrayList<>(songs); }

    public void playSong(int position) {
        if (position < 0 || position >= songList.size()) return;
        currentSongIndex = position;
        Song songToPlay = songList.get(currentSongIndex);
        try {
            mediaPlayer.reset();
            // SOLUÇÃO DEFINITIVA: Usar URI em vez de caminho de arquivo (data)
            Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songToPlay.getId());
            mediaPlayer.setDataSource(this, songUri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
        updateNotification(true);
        updatePlaybackState(true);
    }

    public void playPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification(false);
            updatePlaybackState(false);
        } else {
            // Só inicia se a lista de músicas não estiver vazia
            if (!songList.isEmpty()) {
                mediaPlayer.start();
                updateNotification(true);
                updatePlaybackState(true);
            }
        }
    }

    public void playNextSong() {
        if (songList.isEmpty()) return;
        if (isShuffle) { currentSongIndex = random.nextInt(songList.size()); }
        else { currentSongIndex = (currentSongIndex + 1) % songList.size(); }
        playSong(currentSongIndex);
    }

    public void playPreviousSong() {
        if (songList.isEmpty()) return;
        if (isShuffle) { currentSongIndex = random.nextInt(songList.size()); }
        else { currentSongIndex = (currentSongIndex > 0) ? currentSongIndex - 1 : songList.size() - 1; }
        playSong(currentSongIndex);
    }

    @Override public void onCompletion(MediaPlayer mp) {
        updatePlaybackState(false);
        switch (repeatMode) {
            case ONE: playSong(currentSongIndex); break;
            case ALL: playNextSong(); break;
            case NONE:
                // Se não for aleatório e não for a última música, toca a próxima
                if (!isShuffle && currentSongIndex < songList.size() - 1) {
                    playNextSong();
                } else if (isShuffle) { // Se for aleatório, sempre toca a próxima
                    playNextSong();
                } else { // Se for a última música e sem repetição, para.
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(0);
                    updateNotification(false);
                }
                break;
        }
    }

    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
    public int getCurrentPosition() { return mediaPlayer != null && mediaPlayer.isPlaying() ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return mediaPlayer != null && mediaPlayer.isPlaying() ? mediaPlayer.getDuration() : 0; }
    public void seekTo(int position) { if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.seekTo(position); }
    public Song getCurrentSong() {
        if (currentSongIndex == -1 || songList.isEmpty() || currentSongIndex >= songList.size()) return null;
        return songList.get(currentSongIndex);
    }
    public boolean toggleShuffle() { isShuffle = !isShuffle; return isShuffle; }
    public RepeatMode cycleRepeatMode() {
        if (repeatMode == RepeatMode.NONE) repeatMode = RepeatMode.ALL;
        else if (repeatMode == RepeatMode.ALL) repeatMode = RepeatMode.ONE;
        else repeatMode = RepeatMode.NONE;
        return repeatMode;
    }
    public int getRepeatModeOrdinal() { return repeatMode.ordinal(); }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music Service Channel", NotificationManager.IMPORTANCE_LOW );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createEmptyNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Player").setContentText("Selecione uma música na biblioteca")
                .setSmallIcon(R.drawable.ic_music_note).setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build();
    }

    private void updateNotification(boolean isPlaying) {
        Song currentSong = getCurrentSong();
        Notification notification = (currentSong == null) ? createEmptyNotification() : buildSongNotification(currentSong, isPlaying);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildSongNotification(Song song, boolean isPlaying) {
        Intent prevIntent = new Intent(this, MusicService.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPI = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent playPauseIntent = new Intent(this, MusicService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePI = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent nextIntent = new Intent(this, MusicService.class).setAction(ACTION_NEXT);
        PendingIntent nextPI = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPI = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);
        int playPauseIcon = isPlaying ? R.drawable.ic_play : R.drawable.ic_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.getTitle()).setContentText(song.getArtist())
                .setSmallIcon(R.drawable.ic_music_note).setContentIntent(contentPI)
                .addAction(R.drawable.ic_previous, "Previous", prevPI)
                .addAction(playPauseIcon, "Play/Pause", playPausePI)
                .addAction(R.drawable.ic_next, "Next", nextPI)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build();
    }

    private void updatePlaybackState(boolean isPlaying) {
        long position = (mediaPlayer != null && mediaPlayer.isPlaying()) ? mediaPlayer.getCurrentPosition() : 0;
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO
        );
        stateBuilder.setState(
                isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                position, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (mediaSession != null) { mediaSession.release(); }
        stopForeground(true);
    }

    public enum RepeatMode { NONE, ALL, ONE }
}
