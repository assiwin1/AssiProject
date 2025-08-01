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

public class LandingActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottom_navigation); 

        // Welcome message
        String username = UserSession.getInstance().getUserName();
        if (username != null && !username.isEmpty()) {
            // Use "user_prefs" to match MainActivity where it's saved
            boolean showUserDetailsPref = getSharedPreferences("user_prefs", MODE_PRIVATE) 
                    .getBoolean("show_username_" + username, true); // User-specific key
            if(showUserDetailsPref)
                tvWelcome.setText("Welcome, " + username + "!");
            else
                tvWelcome.setText("Welcome!"); // Hidden state
        } else {
            // If no username, potentially redirect to login or handle as guest
            tvWelcome.setText("Welcome!");
            // Consider redirecting if username is essential here
            // Intent intent = new Intent(LandingActivity.this, MainActivity.class);
            // startActivity(intent);
            // finish();
            // return; // Prevent rest of onCreate if redirecting
        }

        // ViewPager2 setup with FragmentStateAdapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);

        // Link ViewPager2 with BottomNavigationView
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_todo) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_stats) {
                viewPager.setCurrentItem(2, true);
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

        // Set default selection
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        // Clear user session information
        UserSession.getInstance().logout(); // Assuming UserSession has a logout method

        // Clear the stored user ID from SharedPreferences (used by TodoFragment)
        SharedPreferences prefs = getSharedPreferences(TodoFragment.USER_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(TodoFragment.LOGGED_IN_USER_ID_KEY);
        editor.apply();

        // Navigate back to MainActivity (login screen)
        Intent intent = new Intent(LandingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish LandingActivity
    }

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
