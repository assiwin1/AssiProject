package com.example.assiproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.assiproject.R;
import com.example.assiproject.TodoFragment; // Added this import

// Import Log and Toast if not already present
import android.util.Log;
import android.widget.Toast;

// Make LandingActivity implement the interface from ProfileFragment
public class LandingActivity extends AppCompatActivity implements ProfileFragment.OnProfileFragmentInteractionListener {

    private TextView tvWelcome;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    // Define a TAG for logging
    private static final String TAG = "LandingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Log.d(TAG, "onCreate: LandingActivity started."); // Logging

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Welcome message
        String username = UserSession.getInstance().getUserName();
        if (username != null && !username.isEmpty()) {
            boolean showUserDetailsPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .getBoolean("show_username_" + username, true);
            if(showUserDetailsPref)
                tvWelcome.setText("Welcome, " + username + "!");
            else
                tvWelcome.setText("Welcome!");
        } else {
            tvWelcome.setText("Welcome!");
        }

        // ViewPager2 setup with FragmentStateAdapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // Link ViewPager2 with BottomNavigationView
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(0, true);
                Log.d(TAG, "Navigating to ProfileFragment (index 0)"); // Logging
                return true;
            } else if (itemId == R.id.nav_todo) {
                viewPager.setCurrentItem(1, true);
                Log.d(TAG, "Navigating to TodoFragment (index 1)"); // Logging
                return true;
            } else if (itemId == R.id.nav_stats) {
                viewPager.setCurrentItem(2, true);
                Log.d(TAG, "Navigating to StatsFragment (index 2)"); // Logging
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.nav_profile); break;
                    case 1: bottomNav.setSelectedItemId(R.id.nav_todo); break;
                    case 2: bottomNav.setSelectedItemId(R.id.nav_stats); break;
                }
            }
        });
        Log.d(TAG, "ViewPager and BottomNavigationView setup complete."); // Logging

        // Set default selection
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_fragment_menu, menu); // Changed to profile_fragment_menu
        Log.d(TAG, "LandingActivity: onCreateOptionsMenu inflated R.menu.profile_fragment_menu");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Log.d(TAG, "LandingActivity: onOptionsItemSelected, item ID: " + itemId);

        if (itemId == R.id.action_logout) {
            Log.d(TAG, "LandingActivity: R.id.action_logout selected.");
            performLogout(); // Existing logout logic
            return true;
        } else if (itemId == R.id.action_exit) { 
            Log.d(TAG, "LandingActivity: R.id.action_exit selected, calling onAppExitRequested()");
            onAppExitRequested(); // Call the centralized exit method
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        UserSession.getInstance().logout();
        SharedPreferences prefs = getSharedPreferences(TodoFragment.USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(TodoFragment.LOGGED_IN_USER_ID_KEY);
        editor.apply();

        Intent intent = new Intent(LandingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Log.d(TAG, "LandingActivity: performLogout completed.");
    }

    // --- START: Implementation of ProfileFragment's Listener ---
    @Override
    public void onAppExitRequested() {
        Log.d(TAG, "onAppExitRequested called (from ProfileFragment or self). Attempting to close application.");
        Toast.makeText(this, "Exiting application...", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Calling finishAffinity()...");
        finishAffinity();

        Log.d(TAG, "finishAffinity() called. As a fallback, calling System.exit(0).");
        System.exit(0);
    }
    // --- END: Implementation of ProfileFragment's Listener ---

    // FragmentStateAdapter class
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new ProfileFragment();
                case 1: return new TodoFragment();
                case 2: return new StatsFragment();
                default: return new ProfileFragment(); // Fallback
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs
        }
    }
}
