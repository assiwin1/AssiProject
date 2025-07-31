// UserSession.java
package com.example.assiproject;

public class UserSession {
    private boolean showNameInDialogs;
    private static UserSession instance;
    private String username;
    private boolean isGuest;

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

    public String getUsername() {
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
}