package com.example.meumusicplayer.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

// Tabela de Junção para relação Muitos-para-Muitos
@Entity(tableName = "playlist_songs",
        primaryKeys = {"playlistId", "songId"}, // Chave primária composta
        indices = {@Index("songId")}, // Índice para busca rápida por songId
        foreignKeys = { // Chaves estrangeiras para garantir integridade
                @ForeignKey(entity = PlaylistEntity.class,
                        parentColumns = "playlistId",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE), // Se deletar playlist, remove as associações
                // Não criamos ForeignKey para Song, pois usamos apenas o ID do MediaStore
        })
public class PlaylistSongCrossRef {
    public long playlistId;
    public long songId; // ID da música vindo do MediaStore (Song.getId())

    public PlaylistSongCrossRef(long playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}