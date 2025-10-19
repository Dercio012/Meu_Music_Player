package com.example.meumusicplayer.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface PlaylistDao {

    // --- Operações de Playlist ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    List<PlaylistEntity> getAllPlaylists();

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    void deletePlaylistById(long playlistId);


    // --- Operações de Associação (Música <-> Playlist) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    List<Long> getSongIdsForPlaylist(long playlistId);

    // --- ADIÇÃO: Método para deletar uma música de uma playlist específica ---
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void deleteSongFromPlaylist(long playlistId, long songId);

}
