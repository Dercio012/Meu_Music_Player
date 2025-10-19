package com.example.meumusicplayer.mvp;

import android.content.Context;

import com.example.meumusicplayer.model.Song;

import java.util.List;

// CONTRATO APENAS PARA O PLAYER (MainActivity)
public interface MainContract {

    interface View {
        // --- Métodos da Interface MainContract.View ---
        void setSongList(List<Song> songList) // Null check
        ;

        // Métodos do Player
        void updatePlaybackUI(boolean isPlaying);
        void updateCurrentSong(Song song);
        void updateSeekBar(int position, int max);
        void updateShuffleButton(boolean isEnabled);
        void updateRepeatButton(int repeatMode);

        // Métodos de Navegação/Ação (Menu)
        void navigateToLibrary(); // Abre a LibraryActivity
        void navigateToManagePlaylists(); // Abre a PlaylistsActivity
        void startMusicRecognition(); // Inicia o processo ACRCloud

        void showSongDetails(Song song);

        void showLibraryMessage();

        void showAddToPlaylistDialog(Song song);

        // Métodos ACRCloud (Resultados)
        void showRecognitionResult(String title, String artist);
        void showRecognitionError(String message);
        void showRecognizingProgress(boolean show);

        void showPermissionDeniedError();

        Context getContext();
    }

    interface Presenter {
        // Ciclo de Vida
        void onViewCreated();
        void onViewDestroyed();

        void onResult(String result);

        // Métodos do Player
        void onPlayPauseClicked();
        void onNextClicked();
        void onPreviousClicked();
        void onShuffleClicked();
        void onRepeatClicked();
        void onSeekBarMoved(int position);

        // Métodos do Menu Principal da MainActivity
        void onLibraryClicked(); // Usuário clicou em "Biblioteca"
        void onManagePlaylistsClicked(); // Usuário clicou em "Gerenciar Playlists"
        void onRecognizeMusicClicked(); // Usuário clicou em "Reconhecer Música"

        void onSongAddedToPlaylist(Song song, long playlistId);

        void onNewPlaylistCreated(String name);

        void onSearchQueryChanged(String newText);

        void onSongMenuClicked(Song song, android.view.View anchorView);

        void onSongClicked(Song song);
    }
}