package com.example.assiproject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileFragment extends Fragment {
    private static final int PICK_IMAGE = 100;
    private ImageView imgProfile;
    private TextView tvUserName, tvUserEmail;
    private Uri imageUri;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgProfile = view.findViewById(R.id.imgProfile);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        Button btnChangePicture = view.findViewById(R.id.btnChangePicture);

        // TODO: Replace with real session/user info
        tvUserName.setText(UserSession.getInstance().getUserName());
        tvUserEmail.setText(UserSession.getInstance().getUserEmail());

        // Load a saved image if exists (for now, just use default)
        String imageUriString = UserSession.getInstance().getUserImageUri();
        if (imageUriString != null && !imageUriString.isEmpty()) {
            imgProfile.setImageURI(Uri.parse(imageUriString));
        } else {
            imgProfile.setImageResource(R.drawable.profile); // fallback avatar
        }

        btnChangePicture.setOnClickListener(v -> {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProfile.setImageURI(imageUri);
            // Save URI in session (implement setter in UserSession if you want persistence)
            UserSession.getInstance().setUserImageUri(imageUri.toString());
            Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
        }
    }
}
