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

// Ensure this import is correct for your UserSession class
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LoginDialog.LoginDialogListener, RegisterDialog.RegisterDialogListener, LogoutDialog.LogoutDialogListener {

    private static final String TAG = "MainActivity"; // General MainActivity Log tag
    // Specific tag for detailed user state debugging in onCreate
    private static final String USER_STATE_DEBUG_TAG = "USER_STATE_DEBUG";
    // Centralize SharedPreferences names and keys (must match MyApplication)
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
                        dbHelper.updateUser(currentUser); // Changed from updateUserProfileImage
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
                        dbHelper.updateUser(currentUser); // Changed from updateUserProfileImage
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
        Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: UserSession.getInstance() CALLED. UserSession.isGuest() now reports: " + currentSession.isGuest());

        SharedPreferences mainActivityPrefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String userIdFromPrefsInMain = mainActivityPrefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: '" + LOGGED_IN_USER_ID_KEY + "' from SharedPreferences ('" + USER_PREFS_NAME + "') AFTER UserSession.getInstance(): " + (userIdFromPrefsInMain == null ? "IS NULL" : userIdFromPrefsInMain));

        setContentView(R.layout.activity_main);

        dbHelper = new UserDatabaseHelper(this);
        mainProfileImageView = findViewById(R.id.mainProfileImageView);
        mainUsernameTextView = findViewById(R.id.mainUsernameTextView);
        menuButton = findViewById(R.id.menuButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        Log.d(TAG, "onCreate: Views initialized."); // General log

        setupMenuButton();
        setupBottomNavigationView();
        updateUserStatusAndImage(); // Update top bar UI based on UserSession state

        if (userIdFromPrefsInMain == null) {
            Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: GUEST PATH chosen (userIdFromPrefsInMain is NULL). UI: No BottomNav, Empty Fragment Area.");
            bottomNavigationView.setVisibility(View.GONE);
            Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (existingFragment != null) {
                Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: GUEST PATH - Removing existing fragment: " + existingFragment.getClass().getSimpleName());
                getSupportFragmentManager().beginTransaction().remove(existingFragment).commitNow();
            } else {
                Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: GUEST PATH - No existing fragment to remove.");
            }
            if (!currentSession.isGuest()) {
                Log.w(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: GUEST PATH (Prefs NULL) but UserSession reports NOT GUEST. Forcing UserSession.logout() to sync.");
                currentSession.logout();
                updateUserStatusAndImage();
            }
        } else {
            Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: LOGGED-IN PATH chosen (userIdFromPrefsInMain: " + userIdFromPrefsInMain + "). UI: Show BottomNav, Load TodoFragment.");
            bottomNavigationView.setVisibility(View.VISIBLE);
            Fragment currentFragmentInContainer = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragmentInContainer == null) {
                Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: LOGGED-IN PATH - No fragment in container, loading TodoFragment.");
                loadFragment(new TodoFragment(), false);
                bottomNavigationView.setSelectedItemId(R.id.nav_todo);
            } else {
                Log.i(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: LOGGED-IN PATH - Fragment " + currentFragmentInContainer.getClass().getSimpleName() + " already in container. Syncing BottomNav.");
                if (currentFragmentInContainer instanceof TodoFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_todo);
                } else if (currentFragmentInContainer instanceof StatsFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_stats);
                } else if (currentFragmentInContainer instanceof ProfileFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                } else {
                    Log.w(USER_STATE_DEBUG_TAG, "MainActivity.onCreate: LOGGED-IN PATH - Unexpected fragment " + currentFragmentInContainer.getClass().getSimpleName() + ". Loading TodoFragment.");
                    loadFragment(new TodoFragment(), false);
                    bottomNavigationView.setSelectedItemId(R.id.nav_todo);
                }
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
                Log.d(TAG, "BottomNav: Home selected.");
                if (session.isGuest()) {
                    Log.d(TAG, "BottomNav: Home selected by GUEST. Clearing fragment area.");
                    Fragment existingFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (existingFragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(existingFragment).commit();
                    }
                    return true;
                } else {
                    Log.d(TAG, "BottomNav: Home selected by LOGGED-IN USER. Loading TodoFragment as default home.");
                    selectedFragment = new TodoFragment();
                }
            } else if (itemId == R.id.nav_todo) {
                Log.d(TAG, "BottomNav: Todo selected.");
                if (session.isGuest()) {
                    Log.d(TAG, "BottomNav: Todo selected by GUEST. Showing login dialog.");
                    showLoginDialog("Access To-Do List");
                    return false;
                }
                selectedFragment = new TodoFragment();
            } else if (itemId == R.id.nav_stats) {
                Log.d(TAG, "BottomNav: Stats selected.");
                if (session.isGuest()) {
                    Log.d(TAG, "BottomNav: Stats selected by GUEST. Showing login dialog.");
                    showLoginDialog("Access Stats");
                    return false;
                }
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                Log.d(TAG, "BottomNav: Profile selected.");
                if (session.isGuest()) {
                    Log.d(TAG, "BottomNav: Profile selected by GUEST. Showing login dialog.");
                    showLoginDialog("Access Profile");
                    return false;
                }
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                Log.d(TAG, "BottomNav: Loading fragment: " + selectedFragment.getClass().getSimpleName());
                loadFragment(selectedFragment, true);
                return true;
            }
            Log.d(TAG, "BottomNav: No fragment selected or action taken for itemId: " + itemId);
            return false;
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName() + ", addToBackStack: " + addToBackStack);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }

    private void showLoginDialog(String message) {
        Log.d(TAG, "Showing login dialog with message: " + message);
        LoginDialog dialog = LoginDialog.newInstance(message);
        dialog.show(getSupportFragmentManager(), "LoginDialog");
    }

    @Override
    public void onLoginSuccess(String username) {
        Log.i(TAG, "onLoginSuccess: Callback received for username: " + username);
        User user = dbHelper.getUserByUsername(username);

        if (user != null) {
            Log.i(TAG, "onLoginSuccess: User '" + user.getUsername() + "' found in database. Proceeding with login.");
            UserSession.getInstance().login(user); // Added this line
            
            // Also update SharedPreferences so fragments like TodoFragment can find it
            getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(LOGGED_IN_USER_ID_KEY, user.getUsername())
                    .apply();

            updateUserStatusAndImage();
            bottomNavigationView.setVisibility(View.VISIBLE);
            Log.i(TAG, "onLoginSuccess: Loading TodoFragment and selecting nav_todo.");
            loadFragment(new TodoFragment(), false);
            bottomNavigationView.setSelectedItemId(R.id.nav_todo);
            Toast.makeText(this, "Login Successful: " + user.getUsername(), Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "onLoginSuccess: User with username '" + username + "' not found in database. Login failed.");
            Toast.makeText(this, "Login failed: User data not found.", Toast.LENGTH_LONG).show();
            UserSession.getInstance().logout();
            updateUserStatusAndImage();
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRegisterSuccess(User user) {
        Log.i(TAG, "onRegisterSuccess: User '" + user.getUsername() + "' registered and logged in.");
        UserSession.getInstance().login(user);

        // Also update SharedPreferences
        getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(LOGGED_IN_USER_ID_KEY, user.getUsername())
                .apply();

        updateUserStatusAndImage();
        bottomNavigationView.setVisibility(View.VISIBLE);
        Log.i(TAG, "onRegisterSuccess: Loading TodoFragment and selecting nav_todo.");
        loadFragment(new TodoFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_todo);
        Toast.makeText(this, "Registration Successful: " + user.getUsername(), Toast.LENGTH_SHORT).show();
    }

    // THIS IS THE NEWLY ADDED METHOD
    @Override
    public void onSwitchToLogin() {
        Log.d(TAG, "onSwitchToLogin: User requested to switch from Register to Login.");
        // Assuming RegisterDialog dismisses itself.
        // Show the LoginDialog.
        showLoginDialog("Login to your account");
    }

    @Override
    public void onLogoutConfirmed() {
        Log.i(TAG, "onLogoutConfirmed: Logging out user.");
        UserSession.getInstance().logout();

        // Also clear SharedPreferences
        getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(LOGGED_IN_USER_ID_KEY)
                .apply();

        updateUserStatusAndImage();
        bottomNavigationView.setVisibility(View.GONE);

        Log.i(TAG, "onLogoutConfirmed: Clearing fragment container.");
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
            Log.d(TAG, "updateUserStatusAndImage: User is GUEST or currentUser is null. Setting guest UI.");
            mainUsernameTextView.setText("Guest");
            mainProfileImageView.setImageResource(R.drawable.ic_profile);
        } else {
            User currentUser = session.getCurrentUser();
            if (showUsernamePref) {
                Log.d(TAG, "updateUserStatusAndImage: User is " + currentUser.getUsername() + " (username shown). Setting user UI.");
                mainUsernameTextView.setText(currentUser.getUsername());
            } else {
                Log.d(TAG, "updateUserStatusAndImage: User is " + currentUser.getUsername() + " (username hidden). Setting user UI to 'User'.");
                mainUsernameTextView.setText("User");
            }

            if (currentUser.getProfileImagePath() != null && !currentUser.getProfileImagePath().isEmpty()) {
                Log.d(TAG, "updateUserStatusAndImage: Loading profile image from URI: " + currentUser.getProfileImagePath());
                mainProfileImageView.setImageURI(Uri.parse(currentUser.getProfileImagePath()));
            } else {
                Log.d(TAG, "updateUserStatusAndImage: No profile image path. Setting default profile image.");
                mainProfileImageView.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory for images: " + storageDir.getAbsolutePath());
                throw new IOException("Failed to create directory for images: " + storageDir.getAbsolutePath());
            }
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoUri = FileProvider.getUriForFile(this, "com.example.assiproject.fileprovider", image);
        Log.d(TAG, "createImageFile: Created image file at " + currentPhotoUri.toString());
        return image;
    }

    public void showImageSourceDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Set Profile Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                Log.d(TAG, "showImageSourceDialog: 'Take Photo' selected.");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    File photoFile = createImageFile();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    takePictureLauncher.launch(takePictureIntent);
                } catch (IOException ex) {
                    Log.e(TAG, "showImageSourceDialog: Error creating image file", ex);
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                }
            } else if (options[item].equals("Choose from Gallery")) {
                Log.d(TAG, "showImageSourceDialog: 'Choose from Gallery' selected.");
                selectPictureLauncher.launch("image/*");
            } else if (options[item].equals("Cancel")) {
                Log.d(TAG, "showImageSourceDialog: 'Cancel' selected.");
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
