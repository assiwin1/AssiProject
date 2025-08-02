package com.example.assiproject;

import android.app.Activity;
import android.content.Context; // Added for onAttach
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull; // Added for onAttach
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log; // Added for logging
import android.view.LayoutInflater;
import android.view.Menu;         // Added for menu
import android.view.MenuInflater; // Added for menu
import android.view.MenuItem;     // Added for menu
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

    // ---- START: INTERFACE FOR EXIT ----
    private OnProfileFragmentInteractionListener mListener;

    public interface OnProfileFragmentInteractionListener {
        void onAppExitRequested();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileFragmentInteractionListener) {
            mListener = (OnProfileFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnProfileFragmentInteractionListener");
        }
        Log.d("ProfileFragment", "Attached to activity, listener set.");
    }
    // ---- END: INTERFACE FOR EXIT ----

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true); // Tell the Fragment it has an options menu
        Log.d("ProfileFragment", "onCreate: setHasOptionsMenu(true)");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.d("ProfileFragment", "onCreateView called");

        imgProfile = view.findViewById(R.id.imgProfile);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        Button btnChangePicture = view.findViewById(R.id.btnChangePicture);

        // Populate user info
        String username = UserSession.getInstance().getUserName();
        String email = UserSession.getInstance().getUserEmail();
        tvUserName.setText(username != null ? username : "N/A");
        tvUserEmail.setText(email != null ? email : "N/A");
        Log.d("ProfileFragment", "User info set: " + username);


        String imageUriString = UserSession.getInstance().getUserImageUri();
        if (imageUriString != null && !imageUriString.isEmpty()) {
            imgProfile.setImageURI(Uri.parse(imageUriString));
        } else {
            imgProfile.setImageResource(R.drawable.profile);
        }

        btnChangePicture.setOnClickListener(v -> {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        });

        // Example: If you had a dedicated "Exit" button in fragment_profile.xml
        // Button btnExitApp = view.findViewById(R.id.btnExitAppFromProfile);
        // if (btnExitApp != null) {
        //    btnExitApp.setOnClickListener(v -> {
        //        if (mListener != null) {
        //            Log.d("ProfileFragment", "Exit button clicked, calling listener.onAppExitRequested()");
        //            mListener.onAppExitRequested();
        //        }
        //    });
        // }

        return view;
    }

   /* @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.profile_fragment_menu, menu); // Use a new menu for the fragment
        Log.d("ProfileFragment", "onCreateOptionsMenu: Inflated profile_fragment_menu");
    }*/

   /* @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Log.d("ProfileFragment", "onOptionsItemSelected: Item ID: " + itemId);
        if (itemId == R.id.action_exit_fragment) { // ID for exit in fragment's menu
            if (mListener != null) {
                Log.d("ProfileFragment", "Exit menu item selected, calling listener.onAppExitRequested()");
                mListener.onAppExitRequested();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProfile.setImageURI(imageUri);
            UserSession.getInstance().setUserImageUri(imageUri.toString());
            Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
            Log.d("ProfileFragment", "Profile picture updated.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        Log.d("ProfileFragment", "Detached from activity, listener nulled.");
    }
}
