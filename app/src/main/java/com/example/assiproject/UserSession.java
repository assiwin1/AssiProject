// UserSession.java
package com.example.assiproject;

public class UserSession {
    private static UserSession instance;

    // Field to hold the complete User object
    private User currentUserObject; // <<<< ADDED THIS

    // Existing fields - will be kept in sync with currentUserObject
    private String username;
    private String userEmail;
    private boolean isGuest;
    private String userImageUri;
    private boolean showNameInDialogs; // This field seems independent of the User object

    private UserSession() {
        isGuest = true; // Default to guest, will be updated on login
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // <<<< ADDED THIS METHOD
    public User getCurrentUser() {
        return this.currentUserObject;
    }

    // <<<< MODIFIED THIS METHOD: parameter changed, and logic updated
    public void login(User user) {
        if (user != null) {
            this.currentUserObject = user;
            // Sync individual fields from the User object
            this.username = user.getUsername();
            this.userEmail = user.getEmail();
            this.userImageUri = user.getProfileImagePath();
            this.isGuest = false; // A logged-in user is not a guest
        } else {
            // If a null user is passed, effectively log out
            logout();
        }
    }

    // <<<< MODIFIED THIS METHOD: clears currentUserObject
    public void logout() {
        this.currentUserObject = null;
        // Clear individual fields
        this.username = null;
        this.userEmail = null;
        this.userImageUri = null;
        this.isGuest = true;
    }

    public String getUserName() {
        // Prefer data from currentUserObject if available
        return (currentUserObject != null) ? currentUserObject.getUsername() : username;
    }

    public boolean isGuest() {
        // Primarily determined by whether currentUserObject is null
        return (currentUserObject == null) || isGuest;
    }

    public boolean isShowNameInDialogs() {
        return showNameInDialogs;
    }

    public void setShowNameInDialogs(boolean showNameInDialogs) {
        this.showNameInDialogs = showNameInDialogs;
    }

    public String getUserEmail() {
        // Prefer data from currentUserObject if available
        return (currentUserObject != null) ? currentUserObject.getEmail() : userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        // If there's a logged-in user, update their object too
        if (this.currentUserObject != null) {
            this.currentUserObject.setEmail(userEmail);
        }
    }

    public String getUserImageUri() {
        // Prefer data from currentUserObject if available
        return (currentUserObject != null) ? currentUserObject.getProfileImagePath() : userImageUri;
    }

    public void setUserImageUri(String uri) {
        this.userImageUri = uri;
        // If there's a logged-in user, update their object too
        if (this.currentUserObject != null) {
            this.currentUserObject.setProfileImagePath(uri);
        }
    }

    // Optional: A method to explicitly update the whole User object in the session
    public void updateUserObject(User user) {
        if (user != null) {
            this.login(user); // Re-use login logic to set and sync
        }
    }
}