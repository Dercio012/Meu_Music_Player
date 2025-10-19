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
import android.text.InputType;
import android.util.Log; // Log
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meumusicplayer.adapter.SongAdapter;
import com.example.meumusicplayer.database.AppDatabase;
import com.example.meumusicplayer.database.PlaylistEntity;
import com.example.meumusicplayer.database.PlaylistSongCrossRef;
import com.example.meumusicplayer.model.Song;
import com.example.meumusicplayer.service.MusicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // FormatDuration
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // FormatDuration

public class LibraryActivity extends AppCompatActivity implements SongAdapter.OnSongInteractionListener {

    private static final String TAG = "LibraryActivity"; // Log Tag

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private Toolbar toolbar;
    private List<Song> masterSongList = new ArrayList<>();
    private MusicService musicService;
    private boolean isServiceBound = false;
    private AppDatabase database;
    private ExecutorService databaseExecutor;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "Permissão READ_MEDIA_AUDIO: " + (isGranted ? "Concedida" : "Negada"));
                if (isGranted) { loadSongs(); }
                else { Toast.makeText(this, "Permissão negada.", Toast.LENGTH_LONG).show(); finish();} // Fecha se negar
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_library);

        toolbar = findViewById(R.id.toolbar_library);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); }

        recyclerView = findViewById(R.id.recyclerViewLibrarySongs);
        songAdapter = new SongAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);

        database = AppDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        bindToMusicService();
        checkPermissions();
    }

    private void bindToMusicService() { /* ... código sem mudanças ... */
        Log.d(TAG, "bindToMusicService"); Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() { /* ... código sem mudanças ... */
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected"); MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService(); isServiceBound = true;
            if (musicService != null && !masterSongList.isEmpty()){ musicService.setSongList(masterSongList); }
        }
        @Override public void onServiceDisconnected(ComponentName name) { Log.d(TAG, "onServiceDisconnected"); isServiceBound = false; musicService = null; }
    };

    private void checkPermissions() { /* ... código sem mudanças ... */
        Log.d(TAG, "checkPermissions"); String permission = Manifest.permission.READ_MEDIA_AUDIO;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) { permission = Manifest.permission.READ_EXTERNAL_STORAGE; }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) { Log.d(TAG,"Permissão já concedida."); loadSongs(); }
        else { Log.d(TAG,"Solicitando permissão..."); requestPermissionLauncher.launch(permission); }
    }

    private void loadSongs() { /* ... código sem mudanças (com background thread) ... */
        Log.d(TAG, "loadSongs: Carregando em background...");
        databaseExecutor.execute(() -> {
            ContentResolver contentResolver = getContentResolver(); Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION };
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"; String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = contentResolver.query(songUri, projection, selection, null, sortOrder);
            List<Song> loadedSongs = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                int idCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); int titleCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); int artistCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); int dataCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA); int durationCol=cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                do { loadedSongs.add(new Song( cursor.getLong(idCol), cursor.getString(titleCol), cursor.getString(artistCol), cursor.getString(dataCol), cursor.getLong(durationCol) )); } while (cursor.moveToNext());
                cursor.close();
            }
            Log.d(TAG, "loadSongs: " + loadedSongs.size() + " músicas carregadas.");
            runOnUiThread(() -> {
                masterSongList.clear(); masterSongList.addAll(loadedSongs);
                if(songAdapter != null) songAdapter.updateSongs(masterSongList);
                if (isServiceBound && musicService != null) { musicService.setSongList(masterSongList); }
            });
        });
    }

    private void filterSongs(String query) { /* ... código sem mudanças ... */
        Log.d(TAG, "filterSongs: query=" + query);
        if (query == null) return;
        if (query.isEmpty()) { if (songAdapter!=null) songAdapter.updateSongs(masterSongList); }
        else {
            List<Song> filteredList = new ArrayList<>(); String lowerCaseQuery = query.toLowerCase();
            for (Song song : masterSongList) { if (song.getTitle().toLowerCase().contains(lowerCaseQuery) || (song.getArtist() != null && song.getArtist().toLowerCase().contains(lowerCaseQuery))) { filteredList.add(song); } }
            if (songAdapter!=null) songAdapter.updateSongs(filteredList);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { /* ... código sem mudanças ... */
        getMenuInflater().inflate(R.menu.library_menu, menu); // Usa o menu da biblioteca
        MenuItem searchItem = menu.findItem(R.id.action_search_library); // Usa o ID da biblioteca
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Buscar na Biblioteca...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterSongs(newText); return true; } });
        return super.onCreateOptionsMenu(menu);
    }

    @Override public boolean onSupportNavigateUp() { /* ... código sem mudanças ... */
        finish(); overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); return true;
    }
    @Override public void onBackPressed() { /* ... código sem mudanças ... */
        super.onBackPressed(); overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // --- Implementação dos Listeners do Adapter ---
    @Override
    public void onSongClick(Song song) { // Tocar música
        Log.d(TAG, "onSongClick: " + song.getTitle());
        if (!isServiceBound || musicService == null) { Log.w(TAG,"Serviço não conectado."); return; }
        int masterIndex = masterSongList.indexOf(song);
        if (masterIndex != -1) {
            musicService.playSong(masterIndex);
            finish(); // Volta para o player
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Animação de volta
        } else {
            Log.w(TAG,"Índice não encontrado para a música clicada.");
        }
    }

    @Override
    public void onSongMenuClick(Song song, View anchorView) { // Menu do item (3 pontos)
        Log.d(TAG, "onSongMenuClick: " + song.getTitle());
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.song_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_add_to_playlist) {
                showAddToPlaylistDialog(song); return true;
            } else if (itemId == R.id.action_show_details) {
                showSongDetails(song); return true;
            }
            return false;
        });
        popup.show();
    }
    // --- Fim ---

    // --- DIÁLOGOS ---
    private void showSongDetails(Song song) { /* ... código sem mudanças ... */
        new AlertDialog.Builder(this) .setTitle("Detalhes").setMessage("Título: " + song.getTitle() + "\nArtista: " + song.getArtist() + "\nDuração: " + formatDuration(song.getDuration())) .setPositiveButton("OK", null).show();
    }
    public void showCreatePlaylistDialog() { /* ... código sem mudanças ... */
        AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle("Nova Playlist"); final EditText input = new EditText(this); input.setInputType(InputType.TYPE_CLASS_TEXT); input.setHint("Nome"); builder.setView(input);
        builder.setPositiveButton("Criar", (dialog, which) -> { String name = input.getText().toString().trim(); if (!name.isEmpty()) { createPlaylist(name); } else { Toast.makeText(this, "Nome inválido", Toast.LENGTH_SHORT).show(); } });
        builder.setNegativeButton("Cancelar", (d, w) -> d.cancel()); builder.show();
    }
    private void showAddToPlaylistDialog(Song song) { /* ... código sem mudanças ... */
        databaseExecutor.execute(() -> {
            List<PlaylistEntity> playlists = database.playlistDao().getAllPlaylists();
            runOnUiThread(() -> {
                int listSize = playlists.size(); CharSequence[] items = new CharSequence[listSize + 1]; items[0] = "[ Criar Nova Playlist ]";
                for (int i = 0; i < listSize; i++) { items[i + 1] = playlists.get(i).name; }
                AlertDialog.Builder builder = new AlertDialog.Builder(this); builder.setTitle("Adicionar '" + song.getTitle() + "' a:");
                builder.setItems(items, (dialog, which) -> { if (which == 0) { showCreatePlaylistDialog(); } else { PlaylistEntity selected = playlists.get(which - 1); addSongToPlaylist(song, selected.playlistId); } });
                builder.setNegativeButton("Cancelar", null); builder.show();
            });
        });
    }
    // --- FIM ---

    // --- MÉTODOS DE BANCO DE DADOS ---
    private void createPlaylist(String name) { /* ... código sem mudanças ... */
        databaseExecutor.execute(() -> {
            long newId = database.playlistDao().insertPlaylist(new PlaylistEntity(name));
            runOnUiThread(() -> { Toast.makeText(this, (newId > 0 ? "Playlist '" + name + "' criada!" : "Erro"), Toast.LENGTH_SHORT).show(); });
        });
    }
    private void addSongToPlaylist(Song song, long playlistId) { /* ... código sem mudanças ... */
        databaseExecutor.execute(() -> {
            database.playlistDao().insertPlaylistSongCrossRef(new PlaylistSongCrossRef(playlistId, song.getId()));
            runOnUiThread(() -> { Toast.makeText(this, "'" + song.getTitle() + "' adicionada!", Toast.LENGTH_SHORT).show(); });
        });
    }
    // --- FIM ---

    private String formatDuration(long duration) { /* ... código sem mudanças ... */ long minutes = TimeUnit.MILLISECONDS.toMinutes(duration); long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes); return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds); }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        databaseExecutor.shutdown();
        if (isServiceBound) {
            unbindService(serviceConnection);
        }
    }
}