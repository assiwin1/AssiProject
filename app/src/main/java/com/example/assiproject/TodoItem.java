package com.example.assiproject;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "todos")
public class TodoItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public String description;
    public String date;
    public boolean completed;
    public String userId; // New field for user ID

    // Constructor updated to include userId
    public TodoItem(String text, String description, String date, String userId) {
        this.text = text;
        this.description = description;
        this.date = date;
        this.userId = userId; // Initialize userId
        this.completed = false;
    }

    // Room requires a no-arg constructor
    public TodoItem() {}
}
