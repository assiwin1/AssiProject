package com.example.assiproject;

public class User {
    private int id;
    private String username;
    private String password; // Added password field
    private String email;
    private String userType; // e.g., "Guest", "Registered"
    private String profileImagePath;
    // Add other relevant fields like registration date, etc.

    // Main Constructor - Modified to include password
    public User(int id, String username, String password, String email, String profileImagePath, String userType) {
        this.id = id;
        this.username = username;
        this.password = password; // Assign password
        this.email = email;
        this.profileImagePath = profileImagePath;
        this.userType = userType;
    }

    // Constructor for UserDatabaseHelper's getUserByUsername
    public User(int id, String username, String password, String email) {
        this(id, username, password, email, null, "Registered"); // Call main constructor, profileImagePath can be null, userType to Registered
    }

    // Constructor for creating a new user during registration
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.userType = "Registered"; // Default userType for new registrations
        // id will be set after inserting into the database
        // profileImagePath can be set separately
    }

    // Default constructor (optional, but can be useful)
    public User() {
        this.userType = "Guest"; // Default to Guest if no info provided
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; } // Added getPassword
    public String getEmail() { return email; }
    public String getUserType() { return userType; }
    public String getProfileImagePath() { return profileImagePath; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; } // Added setPassword
    public void setEmail(String email) { this.email = email; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    // It's also good practice to override equals(), hashCode(), and toString()
    // For brevity, I'll omit them here but you should consider adding them for robust object comparison and logging.
}