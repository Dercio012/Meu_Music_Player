package com.example.meumusicplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meumusicplayer.adapter.SongAdapter;
import com.example.meumusicplayer.database.AppDatabase;
import com.example.meumusicplayer.model.Song;
import com.example.meumusicplayer.service.MusicService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongInteractionListener {

    private static final String TAG = "PlaylistDetailActivity";
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private AppDatabase database;
    private ExecutorService databaseExecutor;
    private List<Song> playlistSongs = new ArrayList<>();
    private MusicService musicService;
    private boolean isServiceBound = false;
    private long playlistId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra("PLAYLIST_ID", -1);
        String playlistName = getIntent().getStringExtra("PLAYLIST_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar_playlist_detail);
        toolbar.setTitle(playlistName != null ? playlistName : "Playlist");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        database = AppDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        recyclerView = findViewById(R.id.recyclerViewPlaylistSongs);
        songAdapter = new SongAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);

        if (playlistId != -1) {
            loadSongsForPlaylist();
        } else {
            Toast.makeText(this, "Erro: ID da Playlist inválido.", Toast.LENGTH_SHORT).show();
            finish();
        }

        bindToMusicService();
    }

    private void loadSongsForPlaylist() {
        databaseExecutor.execute(() -> {
            List<Long> songIds = database.playlistDao().getSongIdsForPlaylist(playlistId);
            if (songIds.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(PlaylistDetailActivity.this, "Playlist vazia.", Toast.LENGTH_SHORT).show();
                    songAdapter.updateSongs(new ArrayList<>()); // Limpa a lista na UI
                });
                return;
            }

            List<Song> songs = findSongsInMediaStoreByIds(songIds);
            playlistSongs.clear();
            playlistSongs.addAll(songs);

            runOnUiThread(() -> songAdapter.updateSongs(playlistSongs));
        });
    }

    private List<Song> findSongsInMediaStoreByIds(List<Long> songIds) {
        List<Song> result = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION };

        String[] selectionArgs = new String[songIds.size()];
        StringBuilder selection = new StringBuilder(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < songIds.size(); i++) {
            selection.append("?");
            selectionArgs[i] = String.valueOf(songIds.get(i));
            if (i < songIds.size() - 1) selection.append(",");
        }
        selection.append(")");

        try (Cursor cursor = contentResolver.query(collection, projection, selection.toString(), selectionArgs, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); int titleCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); int artistCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); int dataCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA); int durationCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                do {
                    result.add(new Song(cursor.getLong(idCol), cursor.getString(titleCol), cursor.getString(artistCol), cursor.getString(dataCol), cursor.getLong(durationCol)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar músicas no MediaStore", e);
            runOnUiThread(() -> Toast.makeText(this, "Erro ao buscar músicas.", Toast.LENGTH_SHORT).show());
        }
        return result;
    }

    private void bindToMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) { musicService = ((MusicService.MusicBinder) service).getService(); isServiceBound = true; }
        @Override public void onServiceDisconnected(ComponentName name) { isServiceBound = false; musicService = null; }
    };

    @Override
    public void onSongClick(Song song) {
        if (!isServiceBound || musicService == null) { Toast.makeText(this, "Serviço não conectado", Toast.LENGTH_SHORT).show(); return; }

        musicService.setSongList(playlistSongs);
        int songIndex = playlistSongs.indexOf(song);
        if (songIndex != -1) {
            musicService.playSong(songIndex);
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(mainIntent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
             Toast.makeText(this, "Música não encontrada na lista atual.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSongMenuClick(Song song, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.playlist_song_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_remove_from_playlist) {
                showRemoveSongDialog(song);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public void showCreatePlaylistDialog() {

    }

    private void showRemoveSongDialog(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Remover Música")
                .setMessage("Deseja remover '" + song.getTitle() + "' desta playlist?")
                .setPositiveButton("Remover", (dialog, which) -> removeSongFromPlaylist(song))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void removeSongFromPlaylist(Song song) {
        databaseExecutor.execute(() -> {
            database.playlistDao().deleteSongFromPlaylist(playlistId, song.getId());
            runOnUiThread(this::loadSongsForPlaylist); // Recarrega a lista para atualizar a UI
        });
    }
    
    @Override public boolean onSupportNavigateUp() { finish(); overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); return true; }
    @Override public void onBackPressed() { super.onBackPressed(); overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
        if (isServiceBound) unbindService(serviceConnection);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
