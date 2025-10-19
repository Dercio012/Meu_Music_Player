package com.example.meumusicplayer.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PlaylistEntity.class, PlaylistSongCrossRef.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PlaylistDao playlistDao(); // Método para acessar o DAO

    private static volatile AppDatabase INSTANCE; // Singleton para garantir uma única instância

    // Método para obter a instância do banco de dados
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_player_database")
                            // TODO: Em produção, migrações são necessárias ao mudar o schema
                            .fallbackToDestructiveMigration() // Recria o banco se a versão mudar (perde dados!)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}