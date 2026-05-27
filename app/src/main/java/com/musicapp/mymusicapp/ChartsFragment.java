package com.musicapp.mymusicapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChartsFragment extends Fragment {

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
        String imageUrl; // Safely added data instance property

        Artist(String name, String listeners, String imageUrl) {
            this.name = name;
            this.listeners = listeners;
            this.imageUrl = imageUrl;
        }
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) return false;
        ConnectivityManager cm = (ConnectivityManager)
                getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            );
        }
        return false;
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

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new ArtistAdapter(artistList);
            recyclerView.setAdapter(adapter);
        }

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
            retryBtn.setOnClickListener(v -> {
                String query = searchEditText != null ? searchEditText.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    executeSearch(query);
                } else {
                    fetchData();
                }
            });
        }

        fetchData();
        return view;
    }

    private void fetchData() {
        if (!isNetworkAvailable()) {
            showErrorState("You are currently offline. Please check your internet connection and try again.");
            return;
        }

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

                        // Parse extra-large image URL path node out of the nested array structure
                        String imageUrl = "";
                        if (obj.has("image")) {
                            JSONArray imgArray = obj.getJSONArray("image");
                            if (imgArray.length() > 3) {
                                imageUrl = imgArray.getJSONObject(3).getString("#text");
                            } else if (imgArray.length() > 0) {
                                imageUrl = imgArray.getJSONObject(imgArray.length() - 1).getString("#text");
                            }
                        }

                        artistList.add(new Artist(name, "Listeners: " + listeners, imageUrl));
                    }

                    showSuccessState();
                } catch (Exception e) {
                    showErrorState("Parsing data results failed.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                showErrorState("Failed to retrieve top charts data from the server.");
            }
        });
    }

    private void executeSearch(String query) {
        if (!isNetworkAvailable()) {
            showErrorState("You are currently offline. Cannot perform search query.");
            return;
        }

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

                        // Parse image URL asset path node out of search payload structure
                        String imageUrl = "";
                        if (obj.has("image")) {
                            JSONArray imgArray = obj.getJSONArray("image");
                            if (imgArray.length() > 3) {
                                imageUrl = imgArray.getJSONObject(3).getString("#text");
                            } else if (imgArray.length() > 0) {
                                imageUrl = imgArray.getJSONObject(imgArray.length() - 1).getString("#text");
                            }
                        }

                        artistList.add(new Artist(name, "Listeners: " + listeners, imageUrl));
                    }

                    if (artistList.isEmpty()) {
                        showErrorState("No artists found matching your search term.");
                    } else {
                        showSuccessState();
                    }
                } catch (Exception e) {
                    showErrorState("Parsing search results failed.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                showErrorState("Failed to complete search request. Please try again.");
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
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
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
                }
                if (errorMessageText != null) {
                    errorMessageText.setText(message);
                }
            });
        }
    }

    class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {
        private final List<Artist> list;

        ArtistAdapter(List<Artist> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Artist artist = list.get(position);

            if (holder.nameText != null) {
                holder.nameText.setText(artist.name);
            }
            if (holder.listenersText != null) {
                holder.listenersText.setText(artist.listeners);
            }

            if (holder.artistImage != null) {
                // Check if the API returned an empty URL or Last.fm's broken star placeholder asset link
                boolean isPlaceholder = artist.imageUrl == null ||
                        artist.imageUrl.isEmpty() ||
                        artist.imageUrl.contains("2a96cbd8b46e442fc41c2b86b821562f") || // Common Last.fm star asset hash
                        artist.imageUrl.contains("star");

                if (!isPlaceholder) {
                    // If Last.fm actually gives us a real unique picture, display it!
                    Glide.with(holder.itemView.getContext())
                            .load(artist.imageUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(holder.artistImage);
                } else {
                    // DYNAMIC ARTIST INITIAL AVATAR: Generates beautiful colorful cards automatically
                    holder.artistImage.setImageDrawable(null); // Clear old image

                    // 1. Determine a stable background color matching the artist's name string hash
                    int[] materialColors = {
                            0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
                            0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFFFF5722
                    };
                    int colorIndex = Math.abs(artist.name.hashCode()) % materialColors.length;
                    int pickedColor = materialColors[colorIndex];

                    // 2. Extract the first letter of the artist's name safely
                    String initial = !artist.name.isEmpty() ? artist.name.substring(0, 1).toUpperCase() : "?";

                    // 3. Create a clean shape layer programmatically
                    android.graphics.drawable.GradientDrawable backgroundShape = new android.graphics.drawable.GradientDrawable();
                    backgroundShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                    backgroundShape.setCornerRadius(16f); // Beautiful rounded corners matching M3 specifications
                    backgroundShape.setColor(pickedColor);

                    // 4. Combine the background shape with the uppercase initial text letter
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

                    backgroundShape.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    backgroundShape.draw(canvas);

                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.WHITE);
                    paint.setTextSize(54f);
                    paint.setAntiAlias(true);
                    paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                    paint.setTextAlign(android.graphics.Paint.Align.CENTER);

                    float yPos = (canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2);
                    canvas.drawText(initial, canvas.getWidth() / 2, yPos, paint);

                    // Assign the newly generated custom initial avatar drawable back into the view item row
                    holder.artistImage.setImageBitmap(bitmap);
                }
            }

            holder.itemView.setOnClickListener(view -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, ArtistDetailFragment.newInstance(artist.name))
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        // Correctly defined inner ViewHolder class matching all accessed symbols
        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView nameText;
            public TextView listenersText;
            public ImageView artistImage;

            public ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.artistName);
                listenersText = itemView.findViewById(R.id.artistListeners);
                artistImage = itemView.findViewById(R.id.artistImageView);
            }
        }
    }
}