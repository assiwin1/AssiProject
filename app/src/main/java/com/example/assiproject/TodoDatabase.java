package com.example.assiproject;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

// Increment the version number from 1 to 2
@Database(entities = {TodoItem.class}, version = 2)
public abstract class TodoDatabase extends RoomDatabase {
    private static TodoDatabase instance;

    public abstract TodoDao todoDao();

    // Define the migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // SQL to add the new 'userId' column to the 'todos' table
            // Make sure 'TEXT' is appropriate for your userId. If it's an INTEGER, use that.
            // You can also specify a default value if needed, e.g., using DEFAULT 'some_default_user'
            database.execSQL("ALTER TABLE todos ADD COLUMN userId TEXT");
        }
    };

    public static synchronized TodoDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            TodoDatabase.class, "todo_database")
                    .allowMainThreadQueries() // For demo only; use async in real apps!
                    // Add the migration to the builder
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return instance;
    }
}
