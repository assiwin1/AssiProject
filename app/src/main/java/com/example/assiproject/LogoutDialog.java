package com.example.assiproject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class LogoutDialog extends DialogFragment {

    public interface LogoutDialogListener {
        void onLogoutConfirmed(); // To match MainActivity
    }

    private LogoutDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (LogoutDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement LogoutDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // UserSession.getInstance().logout(getContext()); // Call logout from UserSession
                        // The actual logout logic including SharedPreferences clearing
                        // is now primarily handled in MainActivity's onLogoutConfirmed
                        // after this dialog's listener callback.
                        if (listener != null) {
                            listener.onLogoutConfirmed();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        LogoutDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
