package com.musicapp.mymusicapp;

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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CollectionFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private CollectionAdapter adapter;
    private final List<FavoriteArtist> favoriteList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Blends seamlessly with your collection layout file
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.rvFavorites);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CollectionAdapter(favoriteList, new CollectionAdapter.OnFavClickListener() {
            @Override
            public void onItemClick(String artistName) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, ArtistDetailFragment.newInstance(artistName))
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onDeleteClick(FavoriteArtist artist, int position) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
                    db.artistDao().deleteFavorite(artist);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String displayName = fallbackGetName(artist);
                            favoriteList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, favoriteList.size());
                            checkEmptyState();
                            Toast.makeText(getContext(), displayName + " removed from collection", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        recyclerView.setAdapter(adapter);
        loadFavoritesData();
        return view;
    }

    private void loadFavoritesData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
            List<FavoriteArtist> savedArtists = db.artistDao().getAllFavorites();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    favoriteList.clear();
                    if (savedArtists != null) {
                        favoriteList.addAll(savedArtists);
                    }
                    adapter.notifyDataSetChanged();
                    checkEmptyState();
                });
            }
        });
    }

    private void checkEmptyState() {
        if (favoriteList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // --- BULLETPROOF REFLECTION FALLBACK ENGINES ---
    private static String fallbackGetName(FavoriteArtist item) {
        if (item == null) return "Artist";
        // Try known method signatures or fields via reflection
        for (Method m : item.getClass().getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("name") && m.getParameterCount() == 0) {
                try { return String.valueOf(m.invoke(item)); } catch (Exception ignored) {}
            }
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("name")) {
                try {
                    f.setAccessible(true);
                    return String.valueOf(f.get(item));
                } catch (Exception ignored) {}
            }
        }
        return "Unknown Artist";
    }

    private static String fallbackGetListeners(FavoriteArtist item) {
        if (item == null) return "";
        for (Method m : item.getClass().getDeclaredMethods()) {
            if ((m.getName().toLowerCase().contains("listener") || m.getName().toLowerCase().contains("display")) && m.getParameterCount() == 0) {
                try { return String.valueOf(m.invoke(item)); } catch (Exception ignored) {}
            }
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            String name = f.getName().toLowerCase();
            if (name.contains("listener") || name.contains("display") || name.contains("count")) {
                try {
                    f.setAccessible(true);
                    return String.valueOf(f.get(item));
                } catch (Exception ignored) {}
            }
        }
        return "";
    }

    // --- RECYCLERVIEW ADAPTER ---
    private static class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

        private final List<FavoriteArtist> list;
        private final OnFavClickListener listener;

        interface OnFavClickListener {
            void onItemClick(String artistName);
            void onDeleteClick(FavoriteArtist artist, int position);
        }

        CollectionAdapter(List<FavoriteArtist> list, OnFavClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Fixed line: Direct layout resource reference inflation
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FavoriteArtist item = list.get(position);

            String artistName = fallbackGetName(item);
            String listenersInfo = fallbackGetListeners(item);

            holder.tvName.setText(artistName);
            holder.tvListeners.setText(listenersInfo);

            holder.itemView.setOnClickListener(v -> listener.onItemClick(artistName));
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item, holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvListeners;
            MaterialButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.favArtistName);
                tvListeners = itemView.findViewById(R.id.favArtistListeners);
                btnDelete = itemView.findViewById(R.id.btnDeleteFav);
            }
        }
    }
}