package com.example.meumusicplayer.mvp;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.example.meumusicplayer.model.Song;
import com.example.meumusicplayer.service.MusicService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainPresenter implements MainContract.Presenter, IACRCloudListener {

    private static final String TAG = "MainPresenter";

    private MainContract.View view;
    private Context context;
    private MusicService musicService;
    private boolean isServiceBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<Song> masterSongList = new ArrayList<>();

    private ACRCloudClient acrClient;
    private ACRCloudConfig acrConfig;
    private boolean isRecognizing = false;
    private final ActivityResultLauncher<String> requestAudioPermissionLauncher;
    private final ActivityResultLauncher<String> requestStoragePermissionLauncher;

    public MainPresenter(MainContract.View view) {
        this.view = view;
        this.context = view.getContext();

        requestAudioPermissionLauncher = ((AppCompatActivity) context).registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) startRecognitionProcess();
                    else if (this.view != null) this.view.showRecognitionError("Permissão de áudio negada.");
                });

        requestStoragePermissionLauncher = ((AppCompatActivity) context).registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) loadSongs();
                    else Toast.makeText(context, "Permissão de armazenamento negada", Toast.LENGTH_SHORT).show();
                });

        setupACRCloud();
    }

    private void setupACRCloud() {
        acrConfig = new ACRCloudConfig();
        acrConfig.host = "identify-eu-west-1.acrcloud.com";
        acrConfig.accessKey = "ebdbf94f27585e9638cd1a46a50dd2cd";
        acrConfig.accessSecret = "PLGnkKGkWNJWNkkeSfHut28SG5me338O4jcBmvw";
        acrConfig.context = this.context;
        acrConfig.acrcloudListener = this;
        acrClient = new ACRCloudClient();
        acrClient.initWithConfig(acrConfig);
    }

    @Override
    public void onViewCreated() {
        startAndBindService();
        checkAndLoadSongs();
    }

    private void checkAndLoadSongs() {
        String permission = Manifest.permission.READ_MEDIA_AUDIO;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            requestStoragePermissionLauncher.launch(permission);
        }
    }

    private void loadSongs() {
        new Thread(() -> {
            ContentResolver contentResolver = context.getContentResolver();
            Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            Cursor cursor = contentResolver.query(songUri, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC");

            List<Song> loadedSongs = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    loadedSongs.add(new Song(id, title, artist, data, duration));
                } while (cursor.moveToNext());
                cursor.close();
            }
            masterSongList.clear();
            masterSongList.addAll(loadedSongs);
            if (view != null) {
                handler.post(() -> view.setSongList(masterSongList));
            }
            if (isServiceBound && musicService != null) {
                musicService.setSongList(masterSongList);
            }
        }).start();
    }

    @Override
    public void onSongClicked(Song song) {
        if (isServiceBound && musicService != null) {
            int songIndex = masterSongList.indexOf(song);
            if (songIndex != -1) {
                musicService.playSong(songIndex);
            }
        }
    }
    
    // ... (outros métodos permanecem os mesmos)

    @Override
    public void onViewDestroyed() {
        handler.removeCallbacksAndMessages(null);
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
        if (acrClient != null) {
            acrClient.release();
            acrClient = null;
        }
        view = null;
    }

    private void startAndBindService() {
        Intent intent = new Intent(context, MusicService.class);
        ContextCompat.startForegroundService(context, intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isServiceBound = true;
            musicService = ((MusicService.MusicBinder) service).getService();
            musicService.setSongList(masterSongList);
            handler.post(updateUIRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            musicService = null;
            handler.removeCallbacksAndMessages(null);
        }
    };

    @Override public void onRecognizeMusicClicked() {
        if (isRecognizing || acrClient == null) return;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognitionProcess();
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecognitionProcess() {
        if (view != null) view.showRecognizingProgress(true);
        isRecognizing = true;
        if (isServiceBound && musicService != null && musicService.isPlaying()) {
            musicService.playPause();
        }
        acrClient.startRecognize();
    }

    @Override
    public void onResult(String result) {
        if (view == null) return;
        handler.post(() -> view.showRecognizingProgress(false));
        isRecognizing = false;

        try {
            JSONObject jsonResult = new JSONObject(result);
            if (jsonResult.getJSONObject("status").getInt("code") == 0) {
                JSONObject metadata = jsonResult.getJSONObject("metadata");
                JSONArray music = metadata.getJSONArray("music");
                if (music.length() > 0) {
                    JSONObject songData = music.getJSONObject(0);
                    String title = songData.getString("title");
                    String artist = songData.getJSONArray("artists").getJSONObject(0).getString("name");
                    handler.post(() -> view.showRecognitionResult(title, artist));
                }
            } else {
                String errorMsg = jsonResult.getJSONObject("status").getString("msg");
                handler.post(() -> view.showRecognitionError(errorMsg));
            }
        } catch (Exception e) {
            handler.post(() -> view.showRecognitionError("Erro ao processar o resultado."));
        }
    }
    
    @Override
    public void onResult(ACRCloudResult acrCloudResult) {}

    @Override public void onVolumeChanged(double volume) {}
    @Override public void onPlayPauseClicked() { if (isServiceBound && musicService != null) musicService.playPause(); }
    @Override public void onNextClicked() { if (isServiceBound && musicService != null) musicService.playNextSong(); }
    @Override public void onPreviousClicked() { if (isServiceBound && musicService != null) musicService.playPreviousSong(); }
    @Override public void onShuffleClicked() { if (isServiceBound && musicService != null && view != null) view.updateShuffleButton(musicService.toggleShuffle()); }
    @Override public void onRepeatClicked() { if (isServiceBound && musicService != null && view != null) view.updateRepeatButton(musicService.cycleRepeatMode().ordinal()); }
    @Override public void onSeekBarMoved(int position) { if (isServiceBound && musicService != null) musicService.seekTo(position); }
    @Override public void onLibraryClicked() { view.navigateToLibrary(); }
    @Override public void onManagePlaylistsClicked() { view.navigateToManagePlaylists(); }
    @Override public void onSongAddedToPlaylist(Song song, long playlistId) {}
    @Override public void onNewPlaylistCreated(String name) {}
    @Override public void onSearchQueryChanged(String newText) {}
    @Override public void onSongMenuClicked(Song song, View anchorView) {}

    private final Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            if (isServiceBound && musicService != null && view != null) {
                view.updateCurrentSong(musicService.getCurrentSong());
                view.updateSeekBar(musicService.getCurrentPosition(), musicService.getDuration());
                view.updatePlaybackUI(musicService.isPlaying());
                handler.postDelayed(this, 1000);
            }
        }
    };
}
