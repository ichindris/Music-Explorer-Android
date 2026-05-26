package com.musicapp.mymusicapp;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {

    // Executor Service to safely run network tasks off the main UI thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }

    // Method 1: Fetch Global Top Charts
    public void fetchTopArtists(String apiKey, Callback callback) {
        executor.execute(() -> {
            try {
                String urlString = "https://ws.audioscrobbler.com/2.0/?method=chart.gettopartists&api_key="
                        + apiKey + "&format=json";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String result = response.toString();
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("Server error code: " + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network connection failed. Check your internet."));
            }
        });
    }

    // Method 2: Search for an Artist (Fixes the missing method error!)
    public void searchArtist(String query, String apiKey, Callback callback) {
        executor.execute(() -> {
            try {
                // Safely encode spaces and special symbols in the user's search query
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.search&artist="
                        + encodedQuery + "&api_key=" + apiKey + "&format=json";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String result = response.toString();
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    mainHandler.post(() -> callback.onError("Server search error: " + responseCode));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Search query failed."));
            }
        });
    }
}