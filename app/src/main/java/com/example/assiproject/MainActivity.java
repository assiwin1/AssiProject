package com.example.assiproject;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context; // Added for SharedPreferences in login
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.CheckBox;
import android.util.Log; // This was already present
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UserTodoDebug"; // <<< ADDED TAG DEFINITION

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
        currentDialogProfileImageView = findViewById(R.id.mainProfileImageView); // This is for the main view initial setup
        tvUserStatus = findViewById(R.id.tvUserStatus); // This is for the main view initial setup
        
        updateUserStatusAndImage(); 
    }

private void updateUserStatusAndImage() {
    ImageView mainActivityProfileImageView = findViewById(R.id.mainProfileImageView);
    TextView mainTvUserStatus = findViewById(R.id.tvUserStatus);

    if (mainActivityProfileImageView == null || mainTvUserStatus == null) {
        if (!UserSession.getInstance().isGuest()) {
            String username = UserSession.getInstance().getUserName();
            // <<< ADDED Log.d
            Log.d(TAG, "MainActivity - updateUserStatusAndImage (views missing): Setting LOGGED_IN_USER_ID_KEY to: " + username); 
            getSharedPreferences(TodoFragment.USER_PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(TodoFragment.LOGGED_IN_USER_ID_KEY, username)
                    .apply();
            Intent intent = new Intent(MainActivity.this, LandingActivity.class);
            startActivity(intent);
            finish();
        }
        return; 
    }

    if (UserSession.getInstance().isGuest()) {
        mainTvUserStatus.setText("Please login or subscribe");
        mainActivityProfileImageView.setImageResource(R.drawable.profile); 
        mainActivityProfileImageView.setVisibility(View.VISIBLE);
    } else {
        String username = UserSession.getInstance().getUserName();
        boolean showUserDetailsPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getBoolean("show_username_" + username, true);

        if (showUserDetailsPref) {
            mainTvUserStatus.setText("Welcome, " + username);
            loadProfileImageForUser(username, mainActivityProfileImageView);
            mainActivityProfileImageView.setVisibility(View.VISIBLE);
        } else {
            mainTvUserStatus.setText("Welcome (User details hidden)");
            mainActivityProfileImageView.setImageResource(R.drawable.profile);
            mainActivityProfileImageView.setVisibility(View.VISIBLE); 
        }
        
        // <<< ADDED Log.d
        Log.d(TAG, "MainActivity - updateUserStatusAndImage: Setting LOGGED_IN_USER_ID_KEY to: " + username);
        getSharedPreferences(TodoFragment.USER_PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(TodoFragment.LOGGED_IN_USER_ID_KEY, username)
                .apply();

        Intent intent = new Intent(MainActivity.this, LandingActivity.class);
        startActivity(intent);
        finish(); 
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
                .getString("profile_image_path_" + username, null); 
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
        String username = UserSession.getInstance().getUserName(); 
        if (username == null && currentDialogProfileImageView == findViewById(R.id.mainProfileImageView) ) { 
            Toast.makeText(this, "Login required for camera photo", Toast.LENGTH_SHORT).show();
            return; 
        }
        String tempFileName = (username != null) ? username + ".jpg" : "temp_profile_pic.jpg";
        File photoFile = new File(getFilesDir(), tempFileName);

        Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
            Uri imageUri = null;
            if (requestCode == REQUEST_CAMERA) {
                imageUri = Uri.fromFile(new File(currentPhotoPath));
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                imageUri = data.getData();
            }

            if (imageUri != null && currentDialogProfileImageView != null) {
                currentDialogProfileImageView.setImageURI(imageUri);
                if (currentDialogProfileImageView != findViewById(R.id.mainProfileImageView)) {
                    currentDialogProfileImageView.setTag(imageUri.toString());
                    if (requestCode == REQUEST_CAMERA) {
                        currentDialogProfileImageView.setTag(R.id.tag_camera_path, currentPhotoPath);
                    }
                }
            }
            if (requestCode == REQUEST_CAMERA && currentDialogProfileImageView == findViewById(R.id.mainProfileImageView)) {
                 getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                         .remove("profile_image_path_" + UserSession.getInstance().getUserName()) 
                         .apply(); 
            } else if (requestCode == REQUEST_GALLERY && data != null && currentDialogProfileImageView == findViewById(R.id.mainProfileImageView)) {
                 getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                         .putString("profile_image_path_" + UserSession.getInstance().getUserName(), data.getData().toString())
                         .apply();
            }

            if(currentDialogProfileImageView != findViewById(R.id.mainProfileImageView)){
            }
        }
    }

    private void showLoginDialog(UserDatabaseHelper dbHelper) {
        LinearLayout usernameDialogLayout = new LinearLayout(this);
        usernameDialogLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingInDp = 16;
        float scale = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f);
        usernameDialogLayout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        ImageView usernameDialogProfileImage = new ImageView(this);
        usernameDialogProfileImage.setImageResource(R.drawable.profile);
        LinearLayout.LayoutParams imageParamsDialog = new LinearLayout.LayoutParams(
                200, 200 
        );
        imageParamsDialog.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        imageParamsDialog.bottomMargin = paddingInPx / 2;
        usernameDialogProfileImage.setLayoutParams(imageParamsDialog);
        usernameDialogLayout.addView(usernameDialogProfileImage);

        Button btnChangeUsernameDialogPicture = new Button(this);
        btnChangeUsernameDialogPicture.setText("Change Picture");
        LinearLayout.LayoutParams buttonParamsDialog = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParamsDialog.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        buttonParamsDialog.bottomMargin = paddingInPx;
        btnChangeUsernameDialogPicture.setLayoutParams(buttonParamsDialog);
        usernameDialogLayout.addView(btnChangeUsernameDialogPicture);


        final EditText usernameEditText = new EditText(this);
        usernameEditText.setHint("Username");
        usernameDialogLayout.addView(usernameEditText);

        btnChangeUsernameDialogPicture.setOnClickListener(v -> {
            currentDialogProfileImageView = usernameDialogProfileImage;
            currentDialogRequestCode = REQUEST_CAMERA; 
            showPictureDialog();
        });


        new AlertDialog.Builder(this)
                .setTitle("Login - Step 1: Username")
                .setView(usernameDialogLayout)
                .setPositiveButton("Next", (dialog, which) -> {
                    String usernameFromDialog = usernameEditText.getText().toString().trim(); // Renamed to avoid conflict

                    LinearLayout passwordDialogLayout = new LinearLayout(this);
                    passwordDialogLayout.setOrientation(LinearLayout.VERTICAL);
                    passwordDialogLayout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

                    final EditText passwordEditText = new EditText(this);
                    passwordEditText.setHint("Password");
                    passwordEditText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
                    passwordDialogLayout.addView(passwordEditText);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Login - Step 2: Password")
                            .setView(passwordDialogLayout)
                            .setPositiveButton("Login", (d2, w2) -> {
                                String enteredUsername = usernameEditText.getText().toString().trim(); 
                                String enteredPassword = passwordEditText.getText().toString().trim();

                                if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (authenticate(dbHelper, enteredUsername, enteredPassword)) {
                                    UserSession.getInstance().login(enteredUsername);
                                    String email = dbHelper.getEmailByUsername(enteredUsername);
                                    UserSession.getInstance().setUserEmail(email);

                                    // <<< ADDED Log.d
                                    Log.d(TAG, "MainActivity - Login: Setting LOGGED_IN_USER_ID_KEY to: " + enteredUsername);
                                    getSharedPreferences(TodoFragment.USER_PREFS_NAME, MODE_PRIVATE) 
                                            .edit()
                                            .putString(TodoFragment.LOGGED_IN_USER_ID_KEY, enteredUsername) 
                                            .apply();
                                    
                                    if (usernameDialogProfileImage.getTag() != null && currentDialogProfileImageView == usernameDialogProfileImage) {
                                         if (usernameDialogProfileImage.getTag(R.id.tag_camera_path) != null) {
                                            String cameraPath = (String) usernameDialogProfileImage.getTag(R.id.tag_camera_path);
                                            File photoFile = new File(cameraPath);
                                            File newFile = new File(getFilesDir(), enteredUsername + ".jpg");
                                            if (newFile.exists()) newFile.delete();
                                            photoFile.renameTo(newFile);
                                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                                    .remove("profile_image_path_" + enteredUsername).apply();
                                        } else {
                                            String imageUriString = (String) usernameDialogProfileImage.getTag();
                                             getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                                    .putString("profile_image_path_" + enteredUsername, imageUriString).apply();
                                        }
                                    }
                                    updateUserStatusAndImage();
                                    Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                }
                                currentDialogProfileImageView = null;
                                currentPhotoPath = null;
                                currentDialogRequestCode = -1;
                            })
                            .setNegativeButton("Cancel", (d,w) -> {
                                currentDialogProfileImageView = null;
                                currentPhotoPath = null;
                                currentDialogRequestCode = -1;
                            })
                            .show();
                })
                .setNegativeButton("Cancel", (d,w) -> {
                    currentDialogProfileImageView = null;
                    currentPhotoPath = null;
                    currentDialogRequestCode = -1;
                })
                .show();
    }

    private void showSubscribeDialog(UserDatabaseHelper dbHelper) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int paddingInDp = 16;
        float scale = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f);
        layout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

        ImageView dialogProfileImage = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(200, 200);
        imageParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        imageParams.bottomMargin = paddingInPx / 2;
        dialogProfileImage.setLayoutParams(imageParams);
        dialogProfileImage.setImageResource(R.drawable.profile);
        layout.addView(dialogProfileImage);

        Button btnChangePicture = new Button(this);
        btnChangePicture.setText("Change Picture");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        buttonParams.bottomMargin = paddingInPx;
        btnChangePicture.setLayoutParams(buttonParams);
        layout.addView(btnChangePicture);

        EditText usernameEditText = new EditText(this);
        usernameEditText.setHint("Username");
        layout.addView(usernameEditText);

        EditText passwordEditText = new EditText(this);
        passwordEditText.setHint("Password");
        passwordEditText.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordEditText);

        EditText emailEditText = new EditText(this);
        emailEditText.setHint("Email");
        layout.addView(emailEditText);

        CheckBox showUsernameCheckbox = new CheckBox(this);
        showUsernameCheckbox.setText("Show username in all application windows");
        layout.addView(showUsernameCheckbox);

        btnChangePicture.setOnClickListener(v -> {
            currentDialogProfileImageView = dialogProfileImage;
            currentDialogRequestCode = -1; 
            showPictureDialog();
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
                        return;
                    }

                    if (register(dbHelper, usernameStr, passwordStr, emailStr)) {
                        UserSession.getInstance().login(usernameStr); 
                        UserSession.getInstance().setUserEmail(emailStr); // <<< Added this for email session fix

                        // <<< ADDED Log.d
                        Log.d(TAG, "MainActivity - Register: Setting LOGGED_IN_USER_ID_KEY to: " + usernameStr);
                        getSharedPreferences(TodoFragment.USER_PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(TodoFragment.LOGGED_IN_USER_ID_KEY, usernameStr)
                                .apply();

                        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                .putBoolean("show_username_" + usernameStr, showUsernameCheckbox.isChecked())
                                .apply();
                        
                        if (dialogProfileImage.getTag() != null && currentDialogProfileImageView == dialogProfileImage) {
                            if (dialogProfileImage.getTag(R.id.tag_camera_path) != null) {
                                String cameraPath = (String) dialogProfileImage.getTag(R.id.tag_camera_path);
                                File photoFile = new File(cameraPath);
                                File newFile = new File(getFilesDir(), usernameStr + ".jpg");
                                if (newFile.exists()) newFile.delete(); 
                                photoFile.renameTo(newFile);
                            } else {
                                String imageUriString = (String) dialogProfileImage.getTag();
                                getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                                        .putString("profile_image_path_" + usernameStr, imageUriString).apply();
                            }
                        }
                        updateUserStatusAndImage();
                        Toast.makeText(MainActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Registration failed. Username might already exist.", Toast.LENGTH_SHORT).show();
                    }
                    currentDialogProfileImageView = null;
                    currentPhotoPath = null;
                    currentDialogRequestCode = -1;
                })
                .setNegativeButton("Cancel", (d,w) -> {
                    currentDialogProfileImageView = null;
                    currentPhotoPath = null;
                    currentDialogRequestCode = -1;
                })
                .create();
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
