package com.musicapp.mymusicapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ArtistDetailFragment extends Fragment {

    private static final String ARG_ARTIST_NAME = "artist_name";
    private static final String API_KEY = "d419eb5921550293b690b18c178b3556"; // Your active assignment key

    private String artistName;
    private String artistProfileUrl = "https://www.last.fm";
    private String currentListeners = "0";

    private TextView tvName, tvListeners, tvBio;
    private MaterialButton btnSave, btnShare;
    private RecyclerView rvTracks, rvAlbums, rvSimilar;
    private SimpleStringAdapter tracksAdapter, albumsAdapter, similarAdapter;

    private final List<String> tracksList = new ArrayList<>();
    private final List<String> albumsList = new ArrayList<>();
    private final List<String> similarList = new ArrayList<>();

    public static ArtistDetailFragment newInstance(String artistName) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTIST_NAME, artistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            artistName = getArguments().getString(ARG_ARTIST_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_detail, container, false);

        tvName = view.findViewById(R.id.detailArtistName);
        tvListeners = view.findViewById(R.id.detailListenerCount);
        tvBio = view.findViewById(R.id.detailBiography);
        btnSave = view.findViewById(R.id.btnSaveCollection);
        btnShare = view.findViewById(R.id.btnShareArtist);
        rvTracks = view.findViewById(R.id.rvTopTracks);
        rvAlbums = view.findViewById(R.id.rvTopAlbums);
        rvSimilar = view.findViewById(R.id.rvSimilarArtists);

        tvName.setText(artistName);

        // Setup tracks data list binding
        rvTracks.setLayoutManager(new LinearLayoutManager(getContext()));
        tracksAdapter = new SimpleStringAdapter(tracksList, null);
        rvTracks.setAdapter(tracksAdapter);

        // Setup albums data list binding
        rvAlbums.setLayoutManager(new LinearLayoutManager(getContext()));
        albumsAdapter = new SimpleStringAdapter(albumsList, null);
        rvAlbums.setAdapter(albumsAdapter);

        // Setup similar profiles handling navigation link
        rvSimilar.setLayoutManager(new LinearLayoutManager(getContext()));
        similarAdapter = new SimpleStringAdapter(similarList, name -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ArtistDetailFragment.newInstance(name))
                    .addToBackStack(null)
                    .commit();
        });
        rvSimilar.setAdapter(similarAdapter);

        // Native platform share implementation
        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this artist!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Explore " + artistName + " on Last.fm: " + artistProfileUrl);
            startActivity(Intent.createChooser(shareIntent, "Share Artist Via"));
        });

        // Room background data insertion execution block
        btnSave.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
                String listenerDisplay = "0 listeners";
                try {
                    listenerDisplay = String.format("%,d", Long.parseLong(currentListeners)) + " listeners";
                } catch (Exception ignored) {}

                db.artistDao().insertFavorite(new FavoriteArtist(artistName, listenerDisplay));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), artistName + " saved to local database!", Toast.LENGTH_SHORT).show());
                }
            });
        });

        loadData();
        return view;
    }

    private void loadData() {
        NetworkManager.fetchArtistDetails(artistName, API_KEY, new NetworkManager.ArtistDetailCallback() {
            @Override
            public void onSuccess(String bio, String listeners, String url, List<String> topTracks, List<String> topAlbums, List<String> similarArtists) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentListeners = listeners;
                        artistProfileUrl = url;

                        try {
                            tvListeners.setText("Global Listeners: " + String.format("%,d", Long.parseLong(listeners)));
                        } catch (Exception e) {
                            tvListeners.setText("Global Listeners: " + listeners);
                        }

                        tvBio.setText(bio.isEmpty() ? "No biography available for this artist profile." : bio);

                        tracksList.clear();
                        tracksList.addAll(topTracks);
                        tracksAdapter.notifyDataSetChanged();

                        albumsList.clear();
                        albumsList.addAll(topAlbums);
                        albumsAdapter.notifyDataSetChanged();

                        similarList.clear();
                        similarList.addAll(similarArtists);
                        similarAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvBio.setText("Error retrieving extra artist insights info: " + error));
                }
            }
        });
    }

    private static class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.ViewHolder> {
        private final List<String> data;
        private final OnItemClickListener listener;

        interface OnItemClickListener { void onItemClick(String name); }

        SimpleStringAdapter(List<String> data, OnItemClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = data.get(position);
            holder.text.setText(item);
            holder.text.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            if (listener != null) {
                holder.itemView.setOnClickListener(view -> listener.onItemClick(item));
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View itemView) { super(itemView); text = itemView.findViewById(android.R.id.text1); }
        }
    }
}