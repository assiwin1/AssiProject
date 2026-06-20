package com.example.assiproject;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide; // Assuming you use Glide

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class RegisterDialog extends DialogFragment {

    private static final String TAG = "RegisterDialog";
    public interface RegisterDialogListener {
        void onRegisterSuccess(User user);
        void onSwitchToLogin();
    }

    private RegisterDialogListener listener;
    private EditText editTextUsername, editTextPassword, editTextConfirmPassword, editTextEmail;
    private ImageView registerProfileImageView;
    private Uri selectedImageUri; // Will store URI from Gallery or Camera
    private Uri cameraImageUri;   // Specifically for storing image URI taken by camera

    // Launcher for selecting picture from gallery
    private ActivityResultLauncher<String> galleryLauncher;
    // Launcher for taking picture with camera
    private ActivityResultLauncher<Uri> cameraLauncher;
    // Launcher for requesting camera permission
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    public static RegisterDialog newInstance() {
        return new RegisterDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (RegisterDialogListener) getParentFragment(); // Or context, depending on how it's hosted
            if (listener == null && context instanceof RegisterDialogListener) {
                listener = (RegisterDialogListener) context;
            }
            if (listener == null) {
                 throw new ClassCastException("Calling context/fragment must implement RegisterDialogListener");
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "Host must implement RegisterDialogListener", e);
            throw new ClassCastException(context.toString() + " or parent fragment must implement RegisterDialogListener");
        }

        // Initialize permission launcher
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Log.d(TAG, "Image selected from gallery: " + selectedImageUri);
                        Glide.with(this).load(selectedImageUri).circleCrop().into(registerProfileImageView);
                    }
                });

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        selectedImageUri = cameraImageUri; // The URI we provided to the camera intent
                        Log.d(TAG, "Image taken with camera: " + selectedImageUri);
                        Glide.with(this).load(selectedImageUri).circleCrop().into(registerProfileImageView);
                    } else {
                        Log.e(TAG, "Failed to take picture with camera.");
                        Toast.makeText(getContext(), "Failed to take picture.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_register, null);

        editTextUsername = view.findViewById(R.id.editTextUsernameRegister);
        editTextPassword = view.findViewById(R.id.editTextPasswordRegister);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPasswordRegister);
        editTextEmail = view.findViewById(R.id.editTextEmailRegister);
        registerProfileImageView = view.findViewById(R.id.registerProfileImageView);

        if (registerProfileImageView != null) {
            Glide.with(this).load(R.drawable.ic_profile).circleCrop().into(registerProfileImageView); // Default
            registerProfileImageView.setOnClickListener(v -> showImageSourceDialog());
        }


        builder.setView(view)
                .setTitle("Register")
                .setPositiveButton("Register", null) // Set to null, we'''ll override for validation
                .setNegativeButton("Cancel", (dialog, id) -> dismiss())
                .setNeutralButton("Already have an account? Login", (dialog, id) -> {
                    if (listener != null) {
                        listener.onSwitchToLogin();
                    }
                    dismiss();
                });

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim();

                if (TextUtils.isEmpty(username)) {
                    editTextUsername.setError("Username cannot be empty");
                    return;
                }
                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editTextEmail.setError("Enter a valid email");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    editTextPassword.setError("Password cannot be empty");
                    return;
                }
                if (password.length() < 6) {
                    editTextPassword.setError("Password must be at least 6 characters");
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    editTextConfirmPassword.setError("Passwords do not match");
                    return;
                }

                UserDatabaseHelper dbHelper = new UserDatabaseHelper(getContext());
                if (dbHelper.getUserByUsername(username) != null) {
                    editTextUsername.setError("Username already exists");
                    Toast.makeText(getContext(), "Username already exists. Please choose another.", Toast.LENGTH_LONG).show();
                    return;
                }
                 if (dbHelper.getUserByEmail(email) != null) {
                    editTextEmail.setError("Email already registered");
                    Toast.makeText(getContext(), "This email is already registered. Please use another or login.", Toast.LENGTH_LONG).show();
                    return;
                }


                User newUser = new User(username, password, email);
                if (selectedImageUri != null) {
                    newUser.setProfileImagePath(selectedImageUri.toString());
                }

                long userId = dbHelper.addUser(newUser);
                if (userId != -1) {
                    newUser.setId((int)userId); // Set the ID from the database
                    Toast.makeText(getContext(), "Registration successful!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onRegisterSuccess(newUser);
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Registration failed. Username or Email might already exist.", Toast.LENGTH_LONG).show();
                }
            });
        });

        return dialog;
    }

    private void showImageSourceDialog() {
        final CharSequence[] options = {"Camera", "Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Profile Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Camera")) {
                checkCameraPermissionAndLaunch();
            } else if (options[item].equals("Gallery")) {
                galleryLauncher.launch("image/*");
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Show rationale if needed (e.g., in a separate dialog)
            // For simplicity, directly requesting again or informing user.
            Toast.makeText(getContext(), "Camera permission is needed to take a picture.", Toast.LENGTH_LONG).show();
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA); // Or show a dialog explaining why
        }
        else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there'''s a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "IOException in createImageFile()", ex);
                Toast.makeText(getContext(), "Error creating image file.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.assiproject.fileprovider", // Make sure this matches AndroidManifest
                        photoFile);
                cameraLauncher.launch(cameraImageUri);
            }
        } else {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Ensure the directory exists.
        if (storageDir != null && !storageDir.exists()){
            if(!storageDir.mkdirs()){
                 Log.e(TAG, "failed to create directory for pictures");
                 // Handle the error, maybe throw an exception or return null
                 // For now, if directory creation fails, let it proceed, but an error will likely occur later.
            }
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }
}
