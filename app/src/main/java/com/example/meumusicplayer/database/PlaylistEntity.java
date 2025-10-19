package com.example.meumusicplayer.database;

import androidx.room.Entity;
import androidx.room.Ignore; // IMPORTAÇÃO NECESSÁRIA
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {

    @PrimaryKey(autoGenerate = true)
    public long playlistId;

    public String name;

    // Construtor vazio (Room usa este)
    public PlaylistEntity() {}

    // --- CORREÇÃO: Adiciona @Ignore ---
    // Diz ao Room para ignorar este construtor,
    // pois ele já escolheu o construtor vazio.
    @Ignore
    public PlaylistEntity(String name) {
        this.name = name;
    }
    // --- FIM DA CORREÇÃO ---
}