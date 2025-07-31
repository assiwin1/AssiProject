package com.example.assiproject;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.CheckBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private String currentPhotoPath;
    ImageView currentDialogProfileImageView;
    private int currentDialogRequestCode = -1;
    TextView tvUserStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(this);

        findViewById(R.id.btnLogin).setOnClickListener(v -> showLoginDialog(dbHelper));
        findViewById(R.id.btnSubscribe).setOnClickListener(v -> showSubscribeDialog(dbHelper));
        currentDialogProfileImageView = findViewById(R.id.mainProfileImageView);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        if (UserSession.getInstance().isGuest()) {
            tvUserStatus.setText("Please login or subscribe");
        } else {
            tvUserStatus.setText("Welcome, " + UserSession.getInstance().getUsername());
        }
        updateUserStatusAndImage();
    }
    private void updateUserStatusAndImage() {
        if (UserSession.getInstance().isGuest()) {
            tvUserStatus.setText("Please login or subscribe");
            currentDialogProfileImageView.setImageResource(R.drawable.profile); // Default image
            currentDialogProfileImageView.setVisibility(View.VISIBLE); // Or View.GONE if you prefer no image for guests
        } else {
            String username = UserSession.getInstance().getUsername();
            boolean showUserDetailsPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .getBoolean("show_username_" + username, true); // User-specific key

            if (showUserDetailsPref) {
                tvUserStatus.setText("Welcome, " + username);
                loadProfileImageForUser(username, currentDialogProfileImageView); // Load the specific user's image
                currentDialogProfileImageView.setVisibility(View.VISIBLE);     // Make sure image view is visible
            } else {
                tvUserStatus.setText("Welcome (User details hidden)");
                currentDialogProfileImageView.setImageResource(R.drawable.profile); // Set to a generic/default image
                // Or hide it completely if preferred when details are hidden:
                // mainProfileImageView.setVisibility(View.GONE);
                // If you choose to hide it, make sure to set it to VISIBLE when showUserDetailsPref is true.
            }
        }
    }
    private void loadProfileImageForUser(String username, ImageView imageViewToUpdate) {
        if (username == null || imageViewToUpdate == null) {
            if (imageViewToUpdate != null) {
                imageViewToUpdate.setImageResource(R.drawable.profile);
            }
            return;
        }
        File photoFile = new File(getFilesDir(), username + ".jpg");
        if (photoFile.exists()) {
            imageViewToUpdate.setImageURI(Uri.fromFile(photoFile));
            return;
        }
        String path = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("profile_image_path_" + username, null); // User-specific
        if (path != null) {
            imageViewToUpdate.setImageURI(Uri.parse(path));
        } else {
            imageViewToUpdate.setImageResource(R.drawable.profile);
        }
    }
    private void showPictureDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Set Profile Picture")
                .setItems(options, (d, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }
    private void openCamera() {
        String username = UserSession.getInstance().getUsername();
        if (username == null) {
            Toast.makeText(this, "Login required for camera photo", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getFilesDir(), username + ".jpg");
        Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        currentPhotoPath = photoFile.getAbsolutePath();
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (currentDialogProfileImageView != null && currentDialogRequestCode == REQUEST_CAMERA) {
                    currentDialogProfileImageView.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
                } else if (currentDialogProfileImageView != null) {
                    currentDialogProfileImageView.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
                    getSharedPreferences("user_prefs", MODE_PRIVATE)
                            .edit()
                            .remove("profile_image_path")
                            .apply();
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (currentDialogProfileImageView != null && currentDialogRequestCode == REQUEST_GALLERY) {
                    currentDialogProfileImageView.setImageURI(selectedImage);
                } else if (currentDialogProfileImageView != null) {
                    currentDialogProfileImageView.setImageURI(selectedImage);
                    getSharedPreferences("user_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("profile_image_path", selectedImage.toString())
                            .apply();
                }
            }
            // Reset dialog tracking
            currentDialogProfileImageView = null;
            currentDialogRequestCode = -1;
        }
    }
    private void loadProfileImage() {
        String username = UserSession.getInstance().getUsername();
        if (username != null) {
            File photoFile = new File(getFilesDir(), username + ".jpg");
            if (photoFile.exists()) {
                currentDialogProfileImageView.setImageURI(Uri.fromFile(photoFile));
                return;
            }
        }
        String path = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("profile_image_path", null);
        if (path != null) {
            currentDialogProfileImageView.setImageURI(Uri.parse(path));
        } else {
            currentDialogProfileImageView.setImageResource(R.drawable.profile);
        }
    }
    private void showLoginDialog(UserDatabaseHelper dbHelper) {
        // --- First Dialog (Username) ---
        // Create a container for the username dialog if you want more than just the EditText
        LinearLayout usernameDialogLayout = new LinearLayout(this);
        usernameDialogLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingInDp = 16; // Add some padding
        float scale = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f);
        usernameDialogLayout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        // Optional: Add profile image and "Change Picture" to this specific dialog's layout
        ImageView usernameDialogProfileImage = new ImageView(this);
        usernameDialogProfileImage.setImageResource(R.drawable.profile);
        // Add LayoutParams for usernameDialogProfileImage if needed
        usernameDialogLayout.addView(usernameDialogProfileImage);

        Button btnChangeUsernameDialogPicture = new Button(this);
        btnChangeUsernameDialogPicture.setText("Change Picture");
        usernameDialogLayout.addView(btnChangeUsernameDialogPicture);

        final EditText usernameEditText = new EditText(this);
        usernameEditText.setHint("Username");
        usernameDialogLayout.addView(usernameEditText);

        btnChangeUsernameDialogPicture.setOnClickListener(v -> {
            currentDialogProfileImageView = usernameDialogProfileImage; // Set the target for onActivityResult
            showPictureDialog(); // Your existing method to show camera/gallery options
        });


        new AlertDialog.Builder(this)
                .setTitle("Login - Step 1: Username")
                .setView(usernameDialogLayout) // Set the whole layout for this dialog
                .setPositiveButton("Next", (dialog, which) -> {
                    // --- Second Dialog (Password) ---
                    LinearLayout passwordDialogLayout = new LinearLayout(this);
                    passwordDialogLayout.setOrientation(LinearLayout.VERTICAL);
                    passwordDialogLayout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx); // Optional padding

                    final EditText passwordEditText = new EditText(this); // Create a NEW EditText for password
                    passwordEditText.setHint("Password");
                    passwordEditText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
                    passwordDialogLayout.addView(passwordEditText);

                    new AlertDialog.Builder(MainActivity.this) // Use MainActivity.this for context
                            .setTitle("Login - Step 2: Password")
                            .setView(passwordDialogLayout) // Set the password layout
                            .setPositiveButton("Login", (d2, w2) -> {
                                String username = usernameEditText.getText().toString().trim();
                                String password = passwordEditText.getText().toString().trim();

                                if (username.isEmpty() || password.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (authenticate(dbHelper, username, password)) {
                                    UserSession.getInstance().login(username);
                                    updateUserStatusAndImage(); // Your method to update UI
                                    Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show(); // Show the password dialog
                })
                .setNegativeButton("Cancel", null)
                .show(); // Show the username dialog
    }




    private void showSubscribeDialog(UserDatabaseHelper dbHelper) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Add some padding to the layout
        int paddingInDp = 16;
        float scale = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f);
        layout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        // 1. Profile Image for this dialog
        ImageView dialogProfileImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        imageParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        imageParams.width = 200; // Example size
        imageParams.height = 200; // Example size
        imageParams.bottomMargin = paddingInPx / 2;
        dialogProfileImage.setLayoutParams(imageParams);
        dialogProfileImage.setImageResource(R.drawable.profile); // Default image
        layout.addView(dialogProfileImage);

        // 2. "Change Picture" Button for this dialog
        Button btnChangePicture = new Button(this);
        btnChangePicture.setText("Change Picture");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        buttonParams.bottomMargin = paddingInPx;
        btnChangePicture.setLayoutParams(buttonParams);
        layout.addView(btnChangePicture);

        // 3. Username EditText
        EditText usernameEditText = new EditText(this); // Renamed for clarity
        usernameEditText.setHint("Username");
        layout.addView(usernameEditText);

        // 4. Password EditText
        EditText passwordEditText = new EditText(this); // Renamed for clarity
        passwordEditText.setHint("Password");
        passwordEditText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordEditText);

        // 5. Email EditText
        EditText emailEditText = new EditText(this); // Renamed for clarity
        emailEditText.setHint("Email");
        layout.addView(emailEditText);

        // 6. Show Username CheckBox
        CheckBox showUsernameCheckbox = new CheckBox(this);
        showUsernameCheckbox.setText("Show username in all application windows");
        layout.addView(showUsernameCheckbox);

        // >>>>>>> FIX: ADD OnClickListener for the Change Picture button <<<<<<<
        btnChangePicture.setOnClickListener(v -> {
            // When "Change Picture" in THIS dialog is clicked,
            // set currentDialogProfileImageView to THIS dialog's ImageView.
            currentDialogProfileImageView = dialogProfileImage;
            currentDialogRequestCode = -1; // Reset or set a specific code if needed for subscribe dialog context
            showPictureDialog(); // Your existing method to show camera/gallery options
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Subscribe")
                .setView(layout)
                .setPositiveButton("Register", (d, w) -> {
                    String usernameStr = usernameEditText.getText().toString().trim();
                    String passwordStr = passwordEditText.getText().toString().trim();
                    String emailStr = emailEditText.getText().toString().trim();

                    if (usernameStr.isEmpty() || passwordStr.isEmpty() || emailStr.isEmpty()) {
                        Toast.makeText(MainActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return; // Don't dismiss
                    }

                    if (register(dbHelper, usernameStr, passwordStr, emailStr)) {
                        UserSession.getInstance().login(usernameStr);

                        // Save the "show username" preference
                        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                .putBoolean("show_username_" + usernameStr, showUsernameCheckbox.isChecked())
                                .apply();

                        // Save profile image path (if one was set in this dialog)
                        // This logic assumes currentPhotoPath (camera) or dialogProfileImage.getTag() (gallery)
                        // was set by onActivityResult targeting this dialog's dialogProfileImage.
                        if (currentPhotoPath != null && currentDialogProfileImageView == dialogProfileImage) {
                            // Camera was used for THIS subscribe dialog instance.
                            // Rename the temp photo file to be associated with the new username.
                            File photoFile = new File(currentPhotoPath);
                            File newFile = new File(getFilesDir(), usernameStr + ".jpg");
                            if (newFile.exists()) {
                                newFile.delete(); // Delete if an old one exists (shouldn't for new user, but good practice)
                            }
                            if (photoFile.renameTo(newFile)) {
                                // Successfully saved camera image
                                getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                        .remove("profile_image_path_" + usernameStr) // Clear any gallery path
                                        .apply();
                            } else {
                                Toast.makeText(this, "Failed to save camera image.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (dialogProfileImage.getTag() != null && dialogProfileImage.getTag() instanceof String && currentDialogProfileImageView == dialogProfileImage) {
                            // Gallery image was selected for THIS subscribe dialog instance.
                            String imageUriString = (String) dialogProfileImage.getTag();
                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                    .putString("profile_image_path_" + usernameStr, imageUriString)
                                    .apply();
                            // If there was a temporary camera file for this session, clean it up
                            // (though with current logic, camera path leads to rename, not simultaneous gallery)
                            File oldCameraFile = new File(getFilesDir(), usernameStr + ".jpg");
                            if (oldCameraFile.exists() && !oldCameraFile.getPath().equals(currentPhotoPath)) {
                                // Only delete if it's not the one we just saved (unlikely scenario here, but defensive)
                            }
                        }

                        updateUserStatusAndImage();
                        Toast.makeText(MainActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        // Dialog will dismiss automatically
                    } else {
                        Toast.makeText(MainActivity.this, "Registration failed. Username might already exist.", Toast.LENGTH_SHORT).show();
                        // Don't dismiss
                    }
                    // Reset dialog-specific state
                    currentDialogProfileImageView = null;
                    currentPhotoPath = null;
                    currentDialogRequestCode = -1;
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    // Reset dialog-specific state on cancel
                    currentDialogProfileImageView = null;
                    currentPhotoPath = null;
                    currentDialogRequestCode = -1;
                })
                .create(); // Create the dialog first
        dialog.show();
    }


    private boolean authenticate(UserDatabaseHelper dbHelper, String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("users", null, "username=? AND password=?", new String[]{username, password}, null, null, null);
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    private boolean register(UserDatabaseHelper dbHelper, String username, String password, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("email", email);
        try {
            db.insertOrThrow("users", null, values);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}