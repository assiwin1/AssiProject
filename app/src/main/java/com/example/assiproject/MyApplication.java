package com.example.assiproject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// Ensure this import is correct for your UserSession class
import com.example.assiproject.UserSession;

public class MyApplication extends Application {

    private static final String DEBUG_TAG = "MY_APP_INIT";
    // Centralize SharedPreferences names and keys
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(DEBUG_TAG, "MyApplication.onCreate: --- START ---");

        SharedPreferences prefs = getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);

        // Log initial state of the specific key we care about
        String initialUserId = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.i(DEBUG_TAG, "MyApplication.onCreate: User ID in SharedPreferences ('" + LOGGED_IN_USER_ID_KEY + "') BEFORE any action: " + (initialUserId == null ? "IS NULL" : initialUserId));

        // Attempt explicit clear by MyApplication for the specific key
        Log.i(DEBUG_TAG, "MyApplication.onCreate: Attempting explicit clear of '" + LOGGED_IN_USER_ID_KEY + "' by MyApplication.");
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(LOGGED_IN_USER_ID_KEY);
        boolean committed = editor.commit(); // Using commit() for synchronous behavior and to check success
        Log.i(DEBUG_TAG, "MyApplication.onCreate: SharedPreferences.commit() for removal " + (committed ? "succeeded." : "FAILED."));

        String userIdAfterMyAppClear = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.i(DEBUG_TAG, "MyApplication.onCreate: User ID in SharedPreferences ('" + LOGGED_IN_USER_ID_KEY + "') AFTER MyApplication's explicit clear: " + (userIdAfterMyAppClear == null ? "IS NULL (Correct!)" : "STILL PRESENT: " + userIdAfterMyAppClear + " (Problem!)"));

        // Interact with UserSession
        Log.i(DEBUG_TAG, "MyApplication.onCreate: Getting UserSession instance and calling logout().");
        UserSession session = UserSession.getInstance();
        if (session != null) {
            // Assuming session.logout() ALSO handles clearing the relevant SharedPreferences from UserSession's perspective.
            // If UserSession uses a different key or prefs file, that's a separate issue.
            session.logout();
            Log.i(DEBUG_TAG, "MyApplication.onCreate: UserSession.logout() CALLED. UserSession.isGuest() now reports: " + session.isGuest());
        } else {
            Log.e(DEBUG_TAG, "MyApplication.onCreate: UserSession.getInstance() returned null.");
        }

        // Final check of SharedPreferences for our specific key AFTER UserSession.logout()
        // This tells us if session.logout() re-populated it or if it remained clear.
        String userIdAfterSessionLogout = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.i(DEBUG_TAG, "MyApplication.onCreate: User ID in SharedPreferences ('" + LOGGED_IN_USER_ID_KEY + "') AFTER UserSession.logout(): " + (userIdAfterSessionLogout == null ? "IS NULL (Correct!)" : "STILL PRESENT: " + userIdAfterSessionLogout + " (Problem!)"));

        // Final check of UserSession's state for broader context
        Log.i(DEBUG_TAG, "MyApplication.onCreate: UserSession state post-initialization (and potential forced logout) - isGuest: " + UserSession.getInstance().isGuest() + ", UserName: " + UserSession.getInstance().getUserName());
        Log.i(DEBUG_TAG, "MyApplication.onCreate: --- END ---");
    }
}
