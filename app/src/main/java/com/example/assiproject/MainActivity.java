package com.example.assiproject;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(this);

        findViewById(R.id.btnLogin).setOnClickListener(v -> showLoginDialog(dbHelper));
        findViewById(R.id.btnSubscribe).setOnClickListener(v -> showSubscribeDialog(dbHelper));
        TextView tvUserStatus = findViewById(R.id.tvUserStatus);
        if (UserSession.getInstance().isGuest()) {
            tvUserStatus.setText("You are connected as Guest");
        } else {
            tvUserStatus.setText("Welcome, " + UserSession.getInstance().getUsername());
        }
    }
    private void showLoginDialog(UserDatabaseHelper dbHelper) {
        EditText username = new EditText(this);
        username.setHint("Username");
        EditText password = new EditText(this);
        password.setHint("Password");
        password.setInputType(0x00000081); // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Login")
                .setView(username)
                .setPositiveButton("Next", (d, w) -> {
                    TextView tvUserStatus = findViewById(R.id.tvUserStatus);
                    AlertDialog passDialog = new AlertDialog.Builder(this)
                            .setTitle("Password")
                            .setView(password)
                            .setPositiveButton("Login", (d2, w2) -> {
                                if (authenticate(dbHelper, username.getText().toString(), password.getText().toString())) {
                                    UserSession.getInstance().login(username.getText().toString());
                                    tvUserStatus.setText("Welcome, " + UserSession.getInstance().getUsername());
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();
                    passDialog.show();
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void showSubscribeDialog(UserDatabaseHelper dbHelper) {
        EditText username = new EditText(this);
        username.setHint("Username");
        EditText password = new EditText(this);
        password.setHint("Password");
        password.setInputType(0x00000081);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Subscribe")
                .setView(username)
                .setPositiveButton("Next", (d, w) -> {
                    AlertDialog passDialog = new AlertDialog.Builder(this)
                            .setTitle("Password")
                            .setView(password)
                            .setPositiveButton("Register", (d2, w2) -> {
                                if (register(dbHelper, username.getText().toString(), password.getText().toString())) {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "User exists", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();
                    passDialog.show();
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private boolean authenticate(UserDatabaseHelper dbHelper, String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("users", null, "username=? AND password=?", new String[]{username, password}, null, null, null);
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    private boolean register(UserDatabaseHelper dbHelper, String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        try {
            db.insertOrThrow("users", null, values);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}