package com.example.assiproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "UserDatabaseHelper";
    private static final String DATABASE_NAME = "UserDatabase.db";
    private static final int DATABASE_VERSION = 2; // Incremented for schema change

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password"; // In a real app, store hashed passwords
    public static final String COLUMN_EMAIL = "email"; // Added email column
    public static final String COLUMN_PROFILE_IMAGE_PATH = "profile_image_path";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT UNIQUE, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_EMAIL + " TEXT UNIQUE, " +
                    COLUMN_PROFILE_IMAGE_PATH + " TEXT);";

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.i(TAG, "Database table " + TABLE_USERS + " created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Check if the column exists before trying to add it, to make onUpgrade idempotent
            if (!isColumnExists(db, TABLE_USERS, COLUMN_EMAIL)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_EMAIL + " TEXT;");
            }
            Log.i(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion + ". Email column ensured.");
        }
        // Add more upgrade logic for future versions if needed
    }

    private boolean isColumnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            // PRAGMA table_info returns one row for each column in the table
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameColumnIndex = cursor.getColumnIndex("name");
                if (nameColumnIndex == -1) {
                    // 'name' column not found in PRAGMA result, should not happen for valid table_info
                    return false; 
                }
                while (cursor.moveToNext()) {
                    if (columnName.equalsIgnoreCase(cursor.getString(nameColumnIndex))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if column " + columnName + " exists in " + tableName, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    public long addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_PASSWORD, user.getPassword()); // Remember: HASH PASSWORDS in production
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_PROFILE_IMAGE_PATH, user.getProfileImagePath());

        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_USERS, null, values);
            Log.i(TAG, "User " + user.getUsername() + " added with ID: " + id);
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            Log.e(TAG, "Failed to add user. Username or Email likely already exists: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error adding user " + user.getUsername(), e);
        }
        return id;
    }

    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        User user = null;
        try {
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD, COLUMN_EMAIL, COLUMN_PROFILE_IMAGE_PATH},
                    COLUMN_USERNAME + "=?",
                    new String[]{username}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                );
                int profileImagePathIndex = cursor.getColumnIndex(COLUMN_PROFILE_IMAGE_PATH);
                if (profileImagePathIndex != -1 && !cursor.isNull(profileImagePathIndex)) {
                    user.setProfileImagePath(cursor.getString(profileImagePathIndex));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get user by username: " + username, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return user;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        User user = null;
        try {
            cursor = db.query(TABLE_USERS,
                    new String[]{COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD, COLUMN_EMAIL, COLUMN_PROFILE_IMAGE_PATH},
                    COLUMN_EMAIL + "=?",
                    new String[]{email}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                );
                int profileImagePathIndex = cursor.getColumnIndex(COLUMN_PROFILE_IMAGE_PATH);
                if (profileImagePathIndex != -1 && !cursor.isNull(profileImagePathIndex)) {
                    user.setProfileImagePath(cursor.getString(profileImagePathIndex));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get user by email: " + email, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return user;
    }

    public int updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_PROFILE_IMAGE_PATH, user.getProfileImagePath());
        // Note: Password is not updated here. Handle password changes separately and securely.

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_USERS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(user.getId())});
            if (rowsAffected > 0) {
                Log.i(TAG, "User " + user.getUsername() + " (ID: " + user.getId() + ") updated successfully.");
            } else {
                Log.w(TAG, "Attempted to update user " + user.getUsername() + " (ID: " + user.getId() + "), but no rows were affected.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating user " + user.getUsername() + " (ID: " + user.getId() + ")", e);
        }
        return rowsAffected;
    }

    public User checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        Cursor cursor = null;

        String[] columns = {
                COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD,
                COLUMN_EMAIL, COLUMN_PROFILE_IMAGE_PATH
        };
        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        // SECURITY WARNING: Storing and comparing plain text passwords is not secure!
        // Use hashed passwords in a real application.
        String[] selectionArgs = {username, password};

        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                );
                int profileImagePathIndex = cursor.getColumnIndex(COLUMN_PROFILE_IMAGE_PATH);
                if (profileImagePathIndex != -1 && !cursor.isNull(profileImagePathIndex)) {
                    user.setProfileImagePath(cursor.getString(profileImagePathIndex));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to check user credentials for " + username, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return user;
    }
}
