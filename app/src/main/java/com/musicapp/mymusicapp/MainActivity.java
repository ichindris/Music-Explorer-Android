package com.musicapp.mymusicapp;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Set default fragment on startup (Charts Screen)
        // Set default fragment on startup (Charts Screen)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChartsFragment())
                    .commit();

            // ADD THIS LINE BELOW TO FORCE THE NAVBAR TO SELECT BROWSE
            bottomNav.setSelectedItemId(R.id.nav_charts);
        }

        // Set up click listener for the menu items
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_charts) {
                    selectedFragment = new ChartsFragment();
                } else if (item.getItemId() == R.id.nav_collection) {
                    selectedFragment = new CollectionFragment(); // Points seamlessly to your database screen!
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }
}