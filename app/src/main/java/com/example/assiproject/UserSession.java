// UserSession.java
package com.example.assiproject;

public class UserSession {
    private boolean showNameInDialogs;
    private static UserSession instance;
    private String username;
    private String userEmail;
    private boolean isGuest;
    private String userImageUri;
    private UserSession() {
        isGuest = true; // Default to guest
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(String username) {
        this.username = username;
        this.isGuest = false;
    }
    public void logout() {
        this.username = null;
        this.isGuest = true;
    }

    public String getUserName() {
        return username;
    }

    public boolean isGuest() {
        return isGuest;
    }
    public boolean isShowNameInDialogs() {
        return showNameInDialogs;
    }

    public void setShowNameInDialogs(boolean showNameInDialogs) {
        this.showNameInDialogs = showNameInDialogs;
    }
    public String getUserEmail() {
        return userEmail;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    public String getUserImageUri() {
        return userImageUri;
    }
    public void setUserImageUri(String uri) {
        this.userImageUri = uri;
    }
}