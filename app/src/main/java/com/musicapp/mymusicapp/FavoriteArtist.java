package com.musicapp.mymusicapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_artists")
public class FavoriteArtist {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String listeners;

    public FavoriteArtist(String name, String listeners) {
        this.name = name;
        this.listeners = listeners;
    }
}