package com.example.meumusicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meumusicplayer.R;
import com.example.meumusicplayer.model.Song;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private final Context context;
    private List<Song> songList;
    // *** Interface CORRETA ***
    private final OnSongInteractionListener listener;

    // *** Interface CORRETA ***
    public interface OnSongInteractionListener {
        void onSongClick(Song song);
        void onSongMenuClick(Song song, View anchorView); // Assinatura CORRETA

        void showCreatePlaylistDialog();
    }

    // *** Construtor CORRETO ***
    public SongAdapter(Context context, List<Song> initialSongList, OnSongInteractionListener listener) {
        this.context = context;
        this.songList = new ArrayList<>(initialSongList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() { return songList.size(); }

    public void updateSongs(List<Song> newSongList) {
        this.songList.clear();
        this.songList.addAll(newSongList);
        notifyDataSetChanged();
    }

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        ImageButton menuButton;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvItemSongTitle);
            artist = itemView.findViewById(R.id.tvItemSongArtist);
            menuButton = itemView.findViewById(R.id.btnItemMenu);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onSongClick(songList.get(position));
                }
            });

            // *** Listener do Menu CORRETO ***
            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // Passa a MÚSICA e a VIEW (o botão 'v')
                    listener.onSongMenuClick(songList.get(position), v);
                }
            });
        }

        public void bind(Song song) {
            title.setText(song.getTitle());
            artist.setText(song.getArtist());
        }
    }
}