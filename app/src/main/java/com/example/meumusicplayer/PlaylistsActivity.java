package com.example.meumusicplayer;

import android.content.Intent; // Necessário para PlaylistDetailActivity
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager; // Necessário para RecyclerView
import androidx.recyclerview.widget.RecyclerView;
import com.example.meumusicplayer.adapter.PlaylistAdapter;
import com.example.meumusicplayer.database.AppDatabase;
import com.example.meumusicplayer.database.PlaylistEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistInteractionListener {

    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private AppDatabase database;
    private ExecutorService databaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        Toolbar toolbar = findViewById(R.id.toolbar_playlists);
        setSupportActionBar(toolbar);
        // Habilita o botão de voltar na Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true); // Garante que o ícone apareça
        }


        database = AppDatabase.getDatabase(this);
        databaseExecutor = Executors.newSingleThreadExecutor();

        recyclerView = findViewById(R.id.recyclerViewPlaylists);
        adapter = new PlaylistAdapter(this, new ArrayList<>(), this);
        // Define o LayoutManager antes do Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_playlist);
        fab.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
    }

    private void loadPlaylists() {
        databaseExecutor.execute(() -> {
            List<PlaylistEntity> playlists = database.playlistDao().getAllPlaylists();
            runOnUiThread(() -> {
                if (adapter != null) { // Adiciona null check
                    adapter.updatePlaylists(playlists);
                }
            });
        });
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nova Playlist");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nome da Playlist");
        builder.setView(input);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                databaseExecutor.execute(() -> {
                    database.playlistDao().insertPlaylist(new PlaylistEntity(name));
                    loadPlaylists(); // Recarrega a lista
                });
            } else {
                Toast.makeText(this, "Nome inválido", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onPlaylistClick(PlaylistEntity playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("PLAYLIST_ID", playlist.playlistId);
        intent.putExtra("PLAYLIST_NAME", playlist.name);
        startActivity(intent);
        // Adiciona animação ao ABRIR a próxima tela
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onDeletePlaylistClick(PlaylistEntity playlist) {
        new AlertDialog.Builder(this)
                .setTitle("Deletar Playlist")
                .setMessage("Tem certeza que deseja deletar a playlist '" + playlist.name + "'?")
                .setPositiveButton("Deletar", (dialog, which) -> {
                    databaseExecutor.execute(() -> {
                        database.playlistDao().deletePlaylistById(playlist.playlistId);
                        loadPlaylists(); // Recarrega a lista
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- CORREÇÃO: Adiciona overridePendingTransition ao voltar ---
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Fecha a activity atual
        // Aplica a animação de deslizar para a direita (voltando)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        return true; // Indica que o evento foi tratado
    }
    // --- FIM DA CORREÇÃO ---

    // Adiciona tratamento para o botão voltar físico do Android (opcional, mas bom ter)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}