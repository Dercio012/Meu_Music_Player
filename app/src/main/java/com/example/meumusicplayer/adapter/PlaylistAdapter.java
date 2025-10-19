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
import com.example.meumusicplayer.database.PlaylistEntity;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private List<PlaylistEntity> playlistList;
    private final OnPlaylistInteractionListener listener;

    public interface OnPlaylistInteractionListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onDeletePlaylistClick(PlaylistEntity playlist);
    }

    public PlaylistAdapter(Context context, List<PlaylistEntity> initialList, OnPlaylistInteractionListener listener) {
        this.context = context;
        this.playlistList = new ArrayList<>(initialList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistEntity playlist = playlistList.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public void updatePlaylists(List<PlaylistEntity> newPlaylists) {
        this.playlistList.clear();
        this.playlistList.addAll(newPlaylists);
        notifyDataSetChanged();
    }

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView playlistName;
        ImageButton deleteButton;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.tv_playlist_name);
            deleteButton = itemView.findViewById(R.id.btn_delete_playlist);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onPlaylistClick(playlistList.get(pos));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDeletePlaylistClick(playlistList.get(pos));
                }
            });
        }

        public void bind(PlaylistEntity playlist) {
            playlistName.setText(playlist.name);
        }
    }
}