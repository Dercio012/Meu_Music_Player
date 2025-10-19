package com.example.meumusicplayer.database;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.meumusicplayer.model.Song;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PlaylistRepository {

    private final PlaylistDao playlistDao;
    private final Executor databaseExecutor;

    public PlaylistRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        this.playlistDao = db.playlistDao();
        this.databaseExecutor = Executors.newSingleThreadExecutor();
    }

    public void showPlaylistsDialog(Context context, Song song) {
        databaseExecutor.execute(() -> {
            List<PlaylistEntity> playlists = playlistDao.getAllPlaylists();

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Adicionar à Playlist");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
                    for (PlaylistEntity playlist : playlists) {
                        adapter.add(playlist.name);
                    }

                    builder.setAdapter(adapter, (dialog, which) -> {
                        PlaylistEntity selectedPlaylist = playlists.get(which);
                        addSongToPlaylist(song, selectedPlaylist.playlistId, context);
                    });

                    builder.setNegativeButton("Cancelar", null);
                    builder.show();
                });
            }
        });
    }

    private void addSongToPlaylist(Song song, long playlistId, Context context) {
        databaseExecutor.execute(() -> {
            // CORREÇÃO DEFINITIVA: Passa os IDs diretamente para o construtor.
            PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef(playlistId, song.getId());
            playlistDao.insertPlaylistSongCrossRef(crossRef);

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Música adicionada à playlist", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
