package com.musicapp.mymusicapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ArtistDao {
    @Query("SELECT * FROM favorite_artists")
    List<FavoriteArtist> getAllFavorites();

    @Insert
    void insertFavorite(FavoriteArtist artist);

    @Delete
    void deleteFavorite(FavoriteArtist artist);
}