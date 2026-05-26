package com.musicapp.mymusicapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CollectionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private CollectionAdapter adapter;
    private final List<FavoriteArtist> favoriteList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection, container, false);

        recyclerView = view.findViewById(R.id.collectionRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CollectionAdapter(favoriteList);
        recyclerView.setAdapter(adapter);

        // Setup fluid modern swipe-to-delete behavior configuration
        new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
                androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We only need swipe handling, no drag reordering
            }

            // FIXED: Changed method name from swiped to onSwiped to correctly override the abstract method
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                FavoriteArtist artistToDelete = favoriteList.get(position);

                // Run data removal completely off the main thread via ExecutorService to prevent application stutter
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
                    db.artistDao().deleteFavorite(artistToDelete);

                    // Sync changes back to the UI layout safely on the main thread loop
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            favoriteList.remove(position);
                            adapter.notifyItemRemoved(position);

                            // Re-check empty state conditions to draw friendly layout messages if records reach 0
                            if (favoriteList.isEmpty()) {
                                emptyStateText.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            }
        }).attachToRecyclerView(recyclerView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always refresh database contents when navigating back to this tab screen workspace
        loadSavedDatabaseData();
    }

    private void loadSavedDatabaseData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
            List<FavoriteArtist> savedArtists = db.artistDao().getAllFavorites();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    favoriteList.clear();
                    favoriteList.addAll(savedArtists);
                    adapter.notifyDataSetChanged();

                    // Display friendly empty message state check if records count evaluates to zero
                    if (favoriteList.isEmpty()) {
                        emptyStateText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    // ADAPTER ENGINE INNER CLASS FOR DESIGN RENDERING
    class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {
        private final List<FavoriteArtist> list;
        CollectionAdapter(List<FavoriteArtist> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FavoriteArtist fav = list.get(position);
            holder.nameText.setText(fav.name);
            holder.listenersText.setText(fav.listeners);
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, listenersText;
            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.artistName);
                listenersText = itemView.findViewById(R.id.artistListeners);
            }
        }
    }
}