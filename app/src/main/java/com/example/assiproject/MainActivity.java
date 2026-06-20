package com.example.assiproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LoginDialog.LoginDialogListener, RegisterDialog.RegisterDialogListener, LogoutDialog.LogoutDialogListener, ProfileFragment.OnProfileFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private static final String USER_STATE_DEBUG_TAG = "USER_STATE_DEBUG";
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";
    public static final String KEY_SHOW_USERNAME = "KEY_SHOW_USERNAME";

    private UserDatabaseHelper dbHelper;
    private ImageView mainProfileImageView;
    private TextView mainUsernameTextView;
    private ImageButton menuButton;
    private BottomNavigationView bottomNavigationView;
    private Uri currentPhotoUri;

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    mainProfileImageView.setImageURI(currentPhotoUri);
                    User currentUser = UserSession.getInstance().getCurrentUser();
                    if (currentUser != null && currentPhotoUri != null) {
                        currentUser.setProfileImagePath(currentPhotoUri.toString());
                        dbHelper.updateUser(currentUser);
                        Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> selectPictureLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    mainProfileImageView.setImageURI(uri);
                    User currentUser = UserSession.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        currentUser.setProfileImagePath(uri.toString());
                        dbHelper.updateUser(currentUser);
                        Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: --- START ---");

        UserSession currentSession = UserSession.getInstance();
        SharedPreferences mainActivityPrefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String userIdFromPrefsInMain = mainActivityPrefs.getString(LOGGED_IN_USER_ID_KEY, null);

        setContentView(R.layout.activity_main);

        dbHelper = new UserDatabaseHelper(this);
        mainProfileImageView = findViewById(R.id.mainProfileImageView);
        mainUsernameTextView = findViewById(R.id.mainUsernameTextView);
        menuButton = findViewById(R.id.menuButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupMenuButton();
        setupBottomNavigationView();
        updateUserStatusAndImage();

        if (userIdFromPrefsInMain == null) {
            bottomNavigationView.setVisibility(View.GONE);
            Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (existingFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(existingFragment).commitNow();
            }
            if (!currentSession.isGuest()) {
                currentSession.logout();
                updateUserStatusAndImage();
            }
        } else {
            bottomNavigationView.setVisibility(View.VISIBLE);
            Fragment currentFragmentInContainer = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragmentInContainer == null) {
                loadFragment(new TodoFragment(), false);
                bottomNavigationView.setSelectedItemId(R.id.nav_todo);
            }
        }
        Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: --- END ---");
    }

    private void setupMenuButton() {
        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            if (UserSession.getInstance().isGuest()) {
                popupMenu.getMenu().add("Login");
                popupMenu.getMenu().add("Register");
            } else {
                popupMenu.getMenu().add("Logout");
            }
            popupMenu.setOnMenuItemClickListener(item -> {
                CharSequence title = item.getTitle();
                if ("Login".equals(title)) {
                    showLoginDialog("Login to your account");
                } else if ("Register".equals(title)) {
                    RegisterDialog.newInstance().show(getSupportFragmentManager(), "RegisterDialog");
                } else if ("Logout".equals(title)) {
                    new LogoutDialog().show(getSupportFragmentManager(), "LogoutDialog");
                }
                return true;
            });
            popupMenu.show();
        });
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            UserSession session = UserSession.getInstance();
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (session.isGuest()) {
                    Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (existingFragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(existingFragment).commit();
                    }
                    return true;
                } else {
                    selectedFragment = new TodoFragment();
                }
            } else if (itemId == R.id.nav_todo) {
                if (session.isGuest()) {
                    showLoginDialog("Access To-Do List");
                    return false;
                }
                selectedFragment = new TodoFragment();
            } else if (itemId == R.id.nav_stats) {
                if (session.isGuest()) {
                    showLoginDialog("Access Stats");
                    return false;
                }
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                if (session.isGuest()) {
                    showLoginDialog("Access Profile");
                    return false;
                }
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, true);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }

    private void showLoginDialog(String message) {
        LoginDialog dialog = LoginDialog.newInstance(message);
        dialog.show(getSupportFragmentManager(), "LoginDialog");
    }

    @Override
    public void onLoginSuccess(String username) {
        User user = dbHelper.getUserByUsername(username);
        if (user != null) {
            UserSession.getInstance().login(user);
            getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(LOGGED_IN_USER_ID_KEY, user.getUsername())
                    .apply();
            updateUserStatusAndImage();
            bottomNavigationView.setVisibility(View.VISIBLE);
            loadFragment(new TodoFragment(), false);
            bottomNavigationView.setSelectedItemId(R.id.nav_todo);
            Toast.makeText(this, "Login Successful: " + user.getUsername(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Login failed: User data not found.", Toast.LENGTH_LONG).show();
            UserSession.getInstance().logout();
            updateUserStatusAndImage();
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRegisterSuccess(User user) {
        UserSession.getInstance().login(user);
        getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(LOGGED_IN_USER_ID_KEY, user.getUsername())
                .apply();
        updateUserStatusAndImage();
        bottomNavigationView.setVisibility(View.VISIBLE);
        loadFragment(new TodoFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_todo);
        Toast.makeText(this, "Registration Successful: " + user.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSwitchToLogin() {
        showLoginDialog("Login to your account");
    }

    @Override
    public void onLogoutConfirmed() {
        UserSession.getInstance().logout();
        getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(LOGGED_IN_USER_ID_KEY)
                .apply();
        updateUserStatusAndImage();
        bottomNavigationView.setVisibility(View.GONE);
        Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (existingFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(existingFragment).commitNow();
        }
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
    }

    private void updateUserStatusAndImage() {
        UserSession session = UserSession.getInstance();
        SharedPreferences prefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        boolean showUsernamePref = prefs.getBoolean(KEY_SHOW_USERNAME, true);

        if (session.isGuest() || session.getCurrentUser() == null) {
            mainUsernameTextView.setText("Guest");
            mainProfileImageView.setImageResource(R.drawable.ic_profile);
        } else {
            User currentUser = session.getCurrentUser();
            if (showUsernamePref) {
                mainUsernameTextView.setText(currentUser.getUsername());
            } else {
                mainUsernameTextView.setText("User");
            }

            if (currentUser.getProfileImagePath() != null && !currentUser.getProfileImagePath().isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(currentUser.getProfileImagePath()))
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(mainProfileImageView);
            } else {
                mainProfileImageView.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoUri = FileProvider.getUriForFile(this, "com.example.assiproject.fileprovider", image);
        return image;
    }

    public void showImageSourceDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Set Profile Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File photoFile = createImageFile();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    takePictureLauncher.launch(takePictureIntent);
                } catch (IOException ex) {
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                }
            } else if (options[item].equals("Choose from Gallery")) {
                selectPictureLauncher.launch("image/*");
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onAppExitRequested() {
        finishAffinity();
        System.exit(0);
    }

    @Override
    public void onProfileImageUpdated() {
        updateUserStatusAndImage();
    }
}
