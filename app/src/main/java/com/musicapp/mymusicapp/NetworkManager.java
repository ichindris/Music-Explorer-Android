package com.musicapp.mymusicapp;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String jsonResponse);
        void onError(String errorMessage);
    }

    // EXPANDED CALLBACK: Now handles biography, stats, sharing link, tracks, similar artists AND albums!
    public interface ArtistDetailCallback {
        void onSuccess(String bio, String listeners, String url, List<String> topTracks, List<String> topAlbums, List<String> similarArtists);
        void onFailure(String error);
    }

    public static void fetchArtistDetails(String artistName, String apiKey, ArtistDetailCallback callback) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedArtist = java.net.URLEncoder.encode(artistName, "UTF-8");

                // 1. Fetch main info (Bio, Listeners, Share Link, Similar Artists)
                String infoUrlStr = "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist="
                        + encodedArtist + "&api_key=" + apiKey + "&format=json";
                String infoResponse = makeHttpRequest(infoUrlStr);

                // 2. Fetch top tracks dynamically to replace placeholders
                String tracksUrlStr = "https://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist="
                        + encodedArtist + "&api_key=" + apiKey + "&limit=4&format=json";
                String tracksResponse = makeHttpRequest(tracksUrlStr);

                // 3. Fetch top albums dynamically to fulfill the missing handout criteria
                String albumsUrlStr = "https://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist="
                        + encodedArtist + "&api_key=" + apiKey + "&limit=4&format=json";
                String albumsResponse = makeHttpRequest(albumsUrlStr);

                // --- PARSE MAIN INFO ---
                JSONObject infoJson = new JSONObject(infoResponse);
                JSONObject artistObj = infoJson.getJSONObject("artist");
                String listeners = artistObj.getJSONObject("stats").getString("listeners");
                String profileUrl = artistObj.getString("url");
                String bioSummary = artistObj.getJSONObject("bio").getString("summary").replaceAll("<[^>]*>", "");

                List<String> similarList = new ArrayList<>();
                JSONArray similarArray = artistObj.getJSONObject("similar").getJSONArray("artist");
                for (int i = 0; i < Math.min(similarArray.length(), 4); i++) {
                    similarList.add(similarArray.getJSONObject(i).getString("name"));
                }

                // --- PARSE REAL TOP TRACKS ---
                List<String> tracksList = new ArrayList<>();
                try {
                    JSONObject tracksJson = new JSONObject(tracksResponse);
                    JSONArray tracksArray = tracksJson.getJSONObject("toptracks").getJSONArray("track");
                    for (int i = 0; i < Math.min(tracksArray.length(), 4); i++) {
                        tracksList.add(tracksArray.getJSONObject(i).getString("name"));
                    }
                } catch (Exception ignored) {
                    tracksList.add("No top tracks available");
                }

                // --- PARSE REAL TOP ALBUMS ---
                List<String> albumsList = new ArrayList<>();
                try {
                    JSONObject albumsJson = new JSONObject(albumsResponse);
                    JSONArray albumsArray = albumsJson.getJSONObject("topalbums").getJSONArray("album");
                    for (int i = 0; i < Math.min(albumsArray.length(), 4); i++) {
                        albumsList.add(albumsArray.getJSONObject(i).getString("name"));
                    }
                } catch (Exception ignored) {
                    albumsList.add("No top albums available");
                }

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onSuccess(bioSummary, listeners, profileUrl, tracksList, albumsList, similarList)
                );

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    // Helper method to keep detail requests modular and prevent duplication
    private static String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

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

    public void searchArtist(String query, String apiKey, Callback callback) {
        executor.execute(() -> {
            try {
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

    public static boolean isNetworkAvailable(android.content.Context context) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }
}