package com.example.assiproject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private ImageView profileImageView;
    private TextView tvUsername, tvEmail;
    private Button btnChangePicture; // MODIFIED: btnEditProfile and btnDeleteProfile removed
    private User currentUser;
    private UserDatabaseHelper dbHelper;

    private Uri currentPhotoUri; // To store URI for camera photo

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new UserDatabaseHelper(getContext());
        currentUser = UserSession.getInstance().getCurrentUser();

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            updateProfilePicture(imageUri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (currentPhotoUri != null) {
                        updateProfilePicture(currentPhotoUri);
                    } else {
                        Log.e(TAG, "currentPhotoUri is null after camera activity.");
                        Toast.makeText(getContext(), "Failed to get photo from camera.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "Camera activity cancelled or failed. Result code: " + result.getResultCode());
                    if (currentPhotoUri != null) {
                        File photoFile = new File(currentPhotoUri.getPath());
                        if (photoFile.exists()) {
                           // photoFile.delete(); // Be cautious with direct path from URI for deletion
                        }
                    }
                }
            });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImageView = view.findViewById(R.id.imgProfile);
        tvUsername = view.findViewById(R.id.tvUserName);
        tvEmail = view.findViewById(R.id.tvUserEmail);
        btnChangePicture = view.findViewById(R.id.btnChangePicture);
        // MODIFIED: btnEditProfile related findViewById and listener removed
        // MODIFIED: btnDeleteProfile related findViewById and listener removed

        loadUserProfile();

        btnChangePicture.setOnClickListener(v -> showChangePictureDialog());

        // MODIFIED: btnEditProfile.setOnClickListener removed
        // MODIFIED: btnDeleteProfile.setOnClickListener removed

        return view;
    }

    private void loadUserProfile() {
        currentUser = UserSession.getInstance().getCurrentUser(); // Refresh current user
        if (currentUser != null) {
            tvUsername.setText(currentUser.getUsername());
            tvEmail.setText(currentUser.getEmail());

            if (currentUser.getProfileImagePath() != null && !currentUser.getProfileImagePath().isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(currentUser.getProfileImagePath()))
                        .placeholder(R.drawable.ic_profile) // Default placeholder
                        .error(R.drawable.ic_profile) // Error placeholder
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile); // Default if no image path
            }
        } else {
            // Handle case where user might be null (e.g., after deletion or error)
            tvUsername.setText("N/A");
            tvEmail.setText("N/A");
            profileImageView.setImageResource(R.drawable.ic_profile);
            // Disable buttons if no user
            btnChangePicture.setEnabled(false);
            // MODIFIED: btnEditProfile.setEnabled(false); removed
            // MODIFIED: btnDeleteProfile.setEnabled(false); removed
        }
    }

    private void showChangePictureDialog() {
        final CharSequence[] options = {"Take Photo", "Select Photo from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                checkCameraPermissionAndLaunch();
            } else if (options[item].equals("Select Photo from Gallery")) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error occurred while creating the File", ex);
            Toast.makeText(getContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfilePicture(Uri imageUri) {
        if (imageUri == null) {
            Log.e(TAG, "Cannot update profile picture, imageUri is null");
            Toast.makeText(getContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
            return;
        }
        String imagePath = imageUri.toString();
        currentUser.setProfileImagePath(imagePath);
        dbHelper.updateUser(currentUser); // Save to DB
        UserSession.getInstance().updateUserObject(currentUser); // Update session

        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageView);
        Toast.makeText(getContext(), "Profile picture updated.", Toast.LENGTH_SHORT).show();
    }

    // MODIFIED: showEditProfileDialog() method removed

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile(); // Refresh user data when fragment becomes visible
    }
    public interface OnProfileFragmentInteractionListener {
        void onAppExitRequested(); // Or whatever methods your interface needs
        // Add other interaction methods here if necessary
    }
    // private OnPro // Incomplete line from original code, left as is
}
