package com.example.meumusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meumusicplayer.adapter.SongAdapter;
import com.example.meumusicplayer.database.PlaylistRepository;
import com.example.meumusicplayer.model.Song;
import com.example.meumusicplayer.service.MusicService;

import java.util.ArrayList;
import java.util.List;

public abstract class SongListActivity extends AppCompatActivity implements SongAdapter.OnSongInteractionListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private Toolbar toolbar;

    private List<Song> masterSongList = new ArrayList<>();

    private MusicService musicService;
    private boolean isServiceBound = false;
    private PlaylistRepository playlistRepository;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadSongs();
                } else {
                    Toast.makeText(this, "Permissão negada. Não é possível carregar músicas.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        toolbar = findViewById(R.id.toolbar_song_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewSongList);
        songAdapter = new SongAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);

        playlistRepository = new PlaylistRepository(this);

        bindToMusicService();
        checkPermissions();
    }

    private void bindToMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            if (musicService != null && !masterSongList.isEmpty()){
                musicService.setSongList(masterSongList);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private void checkPermissions() {
        String permission = Manifest.permission.READ_MEDIA_AUDIO;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void loadSongs() {
        new Thread(() -> {
            ContentResolver contentResolver = getContentResolver();
            Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            Cursor cursor = contentResolver.query(songUri, projection, selection, null, null);
            List<Song> loadedSongs = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                do {
                    loadedSongs.add(new Song(
                            cursor.getLong(idCol), cursor.getString(titleCol),
                            cursor.getString(artistCol), cursor.getString(dataCol),
                            cursor.getLong(durationCol)
                    ));
                } while (cursor.moveToNext());
                cursor.close();
            }
            runOnUiThread(() -> {
                masterSongList.clear();
                masterSongList.addAll(loadedSongs);
                songAdapter.updateSongs(masterSongList);
                if (isServiceBound && musicService != null) { 
                    musicService.setSongList(masterSongList);
                }
            });
        }).start();
    }

    private void filterSongs(String query) {
        if (query.isEmpty()) {
            songAdapter.updateSongs(masterSongList);
        } else {
            List<Song> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();
            for (Song song : masterSongList) {
                if (song.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        (song.getArtist() != null && song.getArtist().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(song);
                }
            }
            songAdapter.updateSongs(filteredList);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.song_list_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search_list);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Buscar música ou artista...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onSongClick(Song song) {
        if (!isServiceBound || musicService == null) return;

        int masterIndex = masterSongList.indexOf(song);

        if (masterIndex != -1) {
            musicService.playSong(masterIndex);
            finish();
        }
    }

    @Override
    public void onSongMenuClick(Song song, View anchorView) {
        playlistRepository.showPlaylistsDialog(this, song);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}