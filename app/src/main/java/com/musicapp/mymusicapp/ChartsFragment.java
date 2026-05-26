package com.musicapp.mymusicapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChartsFragment extends Fragment {

    // Ensure your real Last.fm API Key string is added here
    private static final String API_KEY = "d419eb5921550293b690b18c178b3556";

    private RecyclerView recyclerView;
    private LinearLayout errorLayout;
    private TextView errorMessageText;

    private ArtistAdapter adapter;
    private final List<Artist> artistList = new ArrayList<>();
    private com.google.android.material.progressindicator.CircularProgressIndicator loadingIndicator;
    private com.google.android.material.textfield.TextInputEditText searchEditText;

    static class Artist {
        String name;
        String listeners;
        Artist(String name, String listeners) {
            this.name = name;
            this.listeners = listeners;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        searchEditText = view.findViewById(R.id.searchEditText);
        errorLayout = view.findViewById(R.id.errorLayout);
        errorMessageText = view.findViewById(R.id.errorMessage);
        searchEditText = view.findViewById(R.id.searchEditText);

        // Bind layout structures explicitly
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ArtistAdapter(artistList);
        recyclerView.setAdapter(adapter);

        if (searchEditText != null) {
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    executeSearch(query);
                } else {
                    fetchData();
                }
                return false;
            });
        }

        View retryBtn = view.findViewById(R.id.btnRetry);
        if (retryBtn != null) {
            retryBtn.setOnClickListener(v -> fetchData());
        }

        fetchData();
        return view;
    }

    private void fetchData() {
        showLoadingState();

        new NetworkManager().fetchTopArtists(API_KEY, new NetworkManager.Callback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject root = new JSONObject(jsonResponse);
                    JSONObject artistsObj = root.getJSONObject("artists");
                    JSONArray artistArray = artistsObj.getJSONArray("artist");

                    artistList.clear();
                    for (int i = 0; i < artistArray.length(); i++) {
                        JSONObject obj = artistArray.getJSONObject(i);
                        String name = obj.getString("name");
                        String listeners = obj.getString("listeners");
                        artistList.add(new Artist(name, "Listeners: " + listeners));
                    }

                    showSuccessState();
                } catch (Exception e) {
                    showErrorState("Parsing data results failed.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                showErrorState(errorMessage);
            }
        });
    }

    private void executeSearch(String query) {
        showLoadingState();

        new NetworkManager().searchArtist(query, API_KEY, new NetworkManager.Callback() {
            @Override
            public void onSuccess(String jsonResponse) {
                try {
                    JSONObject root = new JSONObject(jsonResponse);
                    JSONObject resultsObj = root.getJSONObject("results");
                    JSONObject artistMatchesObj = resultsObj.getJSONObject("artistmatches");
                    JSONArray artistArray = artistMatchesObj.getJSONArray("artist");

                    artistList.clear();
                    for (int i = 0; i < artistArray.length(); i++) {
                        JSONObject obj = artistArray.getJSONObject(i);
                        String name = obj.getString("name");
                        String listeners = obj.has("listeners") ? obj.getString("listeners") : "N/A";
                        artistList.add(new Artist(name, "Listeners: " + listeners));
                    }

                    showSuccessState();
                } catch (Exception e) {
                    showErrorState("Parsing search results failed.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                showErrorState(errorMessage);
            }
        });
    }

    private void showLoadingState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                if (errorLayout != null) errorLayout.setVisibility(View.GONE);
            });
        }
    }

    private void showSuccessState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                if (errorLayout != null) errorLayout.setVisibility(View.GONE);
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
                Toast.makeText(getContext(), "List elements linked: " + artistList.size(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showErrorState(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                if (errorLayout != null) {
                    errorLayout.setVisibility(View.VISIBLE);
                    if (errorMessageText != null) errorMessageText.setText(message);
                }
            });
        }
    }

    class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {
        private final List<Artist> list;
        ArtistAdapter(List<Artist> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Artist artist = list.get(position);
            holder.nameText.setText(artist.name);
            holder.listenersText.setText(artist.listeners);

            // Tapping an item saves it to the local Room storage database
            holder.itemView.setOnClickListener(view -> {
                // Transaction routing to open the complete MD3 Artist Detail view panel
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, ArtistDetailFragment.newInstance(artist.name))
                            .addToBackStack(null) // Allows users to hit the back button to return to the charts!
                            .commit();
                }
            });
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