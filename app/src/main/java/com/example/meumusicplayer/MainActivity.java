package com.example.meumusicplayer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meumusicplayer.adapter.SongAdapter;
import com.example.meumusicplayer.database.AppDatabase;
import com.example.meumusicplayer.database.PlaylistEntity;
import com.example.meumusicplayer.database.PlaylistSongCrossRef;
import com.example.meumusicplayer.model.Song;
import com.example.meumusicplayer.mvp.MainContract;
import com.example.meumusicplayer.mvp.MainPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// CORREÇÃO DEFINITIVA: Removido o modificador 'abstract'
public class MainActivity extends AppCompatActivity implements MainContract.View, SongAdapter.OnSongInteractionListener {

    private MainContract.Presenter presenter;
    private ImageView ivAlbumArt;
    private TextView tvSongTitle, tvSongArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat;
    private ProgressBar recognitionProgressBar;
    private ObjectAnimator albumArtAnimator;
    private RecyclerView recyclerViewSongs;
    private SongAdapter songAdapter;
    private AppDatabase database;
    private ExecutorService databaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        presenter = new MainPresenter(this);
        initializeViews();
        setupClickListeners();
        presenter.onViewCreated();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ivAlbumArt = findViewById(R.id.ivAlbumArt);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        seekBar = findViewById(R.id.seekBar);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        recognitionProgressBar = findViewById(R.id.progressBarRecognition);
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs);

        database = AppDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        songAdapter = new SongAdapter(this, new ArrayList<>(), this);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSongs.setAdapter(songAdapter);

        albumArtAnimator = ObjectAnimator.ofFloat(ivAlbumArt, View.ROTATION, 0f, 360f);
        albumArtAnimator.setDuration(20000);
        albumArtAnimator.setRepeatCount(ValueAnimator.INFINITE);
        albumArtAnimator.setInterpolator(new LinearInterpolator());

        updateCurrentSong(null);
    }

    private void setupClickListeners() {
        btnPlayPause.setOnClickListener(v -> presenter.onPlayPauseClicked());
        btnNext.setOnClickListener(v -> presenter.onNextClicked());
        btnPrevious.setOnClickListener(v -> presenter.onPreviousClicked());
        btnShuffle.setOnClickListener(v -> presenter.onShuffleClicked());
        btnRepeat.setOnClickListener(v -> presenter.onRepeatClicked());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatDuration(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { presenter.onSeekBarMoved(seekBar.getProgress()); }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_manage_playlists) {
            presenter.onManagePlaylistsClicked();
            return true;
        } else if (itemId == R.id.action_recognize_music) {
            presenter.onRecognizeMusicClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setSongList(List<Song> songList) {
        runOnUiThread(() -> songAdapter.updateSongs(songList));
    }

    @Override
    public void updatePlaybackUI(boolean isPlaying) {
        runOnUiThread(() -> {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            if (isPlaying && !albumArtAnimator.isStarted()) {
                albumArtAnimator.start();
            } else if (!isPlaying && albumArtAnimator.isRunning()) {
                albumArtAnimator.pause();
            }
        });
    }

    @Override
    public void updateCurrentSong(Song song) {
        runOnUiThread(() -> {
            if (song != null) {
                tvSongTitle.setText(song.getTitle());
                tvSongArtist.setText(song.getArtist());
            } else {
                tvSongTitle.setText("Nenhuma música");
                tvSongArtist.setText("Selecione uma música");
                ivAlbumArt.setImageResource(R.drawable.ic_music_note);
                albumArtAnimator.pause();
                ivAlbumArt.setRotation(0);
            }
        });
    }

    @Override
    public void updateSeekBar(int position, int max) {
        runOnUiThread(() -> {
            seekBar.setMax(max);
            seekBar.setProgress(position);
            tvCurrentTime.setText(formatDuration(position));
            tvTotalTime.setText(formatDuration(max));
        });
    }

    @Override
    public void updateShuffleButton(boolean isEnabled) {
        runOnUiThread(() -> btnShuffle.setColorFilter(isEnabled ? ContextCompat.getColor(this, R.color.teal_200) : ContextCompat.getColor(this, R.color.white)));
    }

    @Override
    public void updateRepeatButton(int repeatModeOrdinal) {
        runOnUiThread(() -> {
             if (repeatModeOrdinal == 0) { // NONE
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.white));
            } else if (repeatModeOrdinal == 1) { // ALL
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.teal_200));
            } else { // ONE
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.teal_200));
            }
        });
    }

    @Override
    public void showRecognitionResult(String title, String artist) {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                new AlertDialog.Builder(this)
                        .setTitle("Música Reconhecida")
                        .setMessage("Título: " + title + "\nArtista: " + artist)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    @Override
    public void showRecognitionError(String message) {
        runOnUiThread(() -> Toast.makeText(this, "Erro no reconhecimento: " + message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void showRecognizingProgress(boolean show) {
        runOnUiThread(() -> recognitionProgressBar.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    @Override
    public void showPermissionDeniedError() {

    }

    @Override
    public void navigateToManagePlaylists() {
        startActivity(new Intent(this, PlaylistsActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void startMusicRecognition() {

    }

    @Override
    public void showSongDetails(Song song) {

    }

    @Override
    public void showLibraryMessage() {

    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override public void navigateToLibrary() { /* Not used anymore */ }

    @Override
    public void onSongClick(Song song) {
        presenter.onSongClicked(song);
    }

    @Override
    public void onSongMenuClick(Song song, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.song_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add_to_playlist) {
                showAddToPlaylistDialog(song);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public void showCreatePlaylistDialog() {

    }

    public void showAddToPlaylistDialog(Song song) {
        databaseExecutor.execute(() -> {
            List<PlaylistEntity> playlists = database.playlistDao().getAllPlaylists();
            runOnUiThread(() -> {
                final CharSequence[] items = new CharSequence[playlists.size() + 1];
                items[0] = "[ Criar Nova Playlist ]";
                for (int i = 0; i < playlists.size(); i++) {
                    items[i + 1] = playlists.get(i).name;
                }

                new AlertDialog.Builder(this)
                    .setTitle("Adicionar a...")
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) {
                            showCreatePlaylistDialog(song);
                        } else {
                            addSongToPlaylist(song, playlists.get(which - 1).playlistId);
                        }
                    })
                    .show();
            });
        });
    }

    private void showCreatePlaylistDialog(final Song songToAdd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Playlist");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nome da Playlist");
        builder.setView(input);
        builder.setPositiveButton("Criar e Adicionar", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                createPlaylistAndAddSong(playlistName, songToAdd);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createPlaylistAndAddSong(String name, Song song) {
        databaseExecutor.execute(() -> {
            long playlistId = database.playlistDao().insertPlaylist(new PlaylistEntity(name));
            addSongToPlaylist(song, playlistId);
            runOnUiThread(() -> Toast.makeText(this, "Playlist '" + name + "' criada.", Toast.LENGTH_SHORT).show());
        });
    }

    private void addSongToPlaylist(Song song, long playlistId) {
        databaseExecutor.execute(() -> {
            database.playlistDao().insertPlaylistSongCrossRef(new PlaylistSongCrossRef(playlistId, song.getId()));
            runOnUiThread(() -> Toast.makeText(this, "'" + song.getTitle() + "' adicionado à playlist.", Toast.LENGTH_SHORT).show());
        });
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onViewDestroyed();
        databaseExecutor.shutdown();
        if (albumArtAnimator != null) {
            albumArtAnimator.cancel();
        }
    }
}