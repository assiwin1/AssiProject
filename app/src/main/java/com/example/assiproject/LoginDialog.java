package com.example.assiproject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast; // Added for Toast messages

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

// Ensure UserSession and UserDatabaseHelper are correctly imported if not in the same package
// import com.example.assiproject.UserSession; // Assuming UserSession is in this package
// import com.example.assiproject.UserDatabaseHelper; // Assuming UserDatabaseHelper is in this package
// import com.example.assiproject.User; // Assuming User is in this package


public class LoginDialog extends DialogFragment {

    public interface LoginDialogListener {
        void onLoginSuccess(String username);
    }

    private LoginDialogListener listener;
    private EditText editTextUsername;
    private EditText editTextPassword;

    public static LoginDialog newInstance(String message) {
        LoginDialog dialog = new LoginDialog();
        Bundle args = new Bundle();
        args.putString("message", message);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (LoginDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement LoginDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_login, null); // You'll need to create dialog_login.xml

        editTextUsername = view.findViewById(R.id.editTextUsernameDialog); // Ensure this ID is in dialog_login.xml
        editTextPassword = view.findViewById(R.id.editTextPasswordDialog); // Ensure this ID is in dialog_login.xml

        String dialogMessage = getArguments() != null ? getArguments().getString("message") : "Login";

        builder.setView(view)
                .setTitle(dialogMessage)
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String username = editTextUsername.getText().toString().trim();
                        String password = editTextPassword.getText().toString().trim();

                        if (username.isEmpty() || password.isEmpty()) {
                            Toast.makeText(getContext(), "Please enter username and password", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        UserDatabaseHelper dbHelper = new UserDatabaseHelper(getContext());
                        User user = dbHelper.checkUser(username, password);

                        if (user != null) {
                            UserSession.getInstance().login(user); // Make sure UserSession.login is correctly implemented
                            if (listener != null) {
                                listener.onLoginSuccess(username);
                            }
                        } else {
                            Toast.makeText(getContext(), "Invalid username or password", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        LoginDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
