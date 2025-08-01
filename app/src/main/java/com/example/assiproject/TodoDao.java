package com.example.assiproject;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface TodoDao {
    @Insert
    long insert(TodoItem todo); // Stays the same, TodoItem now has userId

    @Update
    void update(TodoItem todo); // Stays the same, TodoItem now has userId

    @Delete
    void delete(TodoItem todo);

    // Modified to fetch todos by userId
    @Query("SELECT * FROM todos WHERE userId = :userId")
    List<TodoItem> getAllByUser(String userId);

    // It might be useful to have a method to get a single item by ID and user,
    // for example, if you were to implement an edit feature or for verification.
    // This is optional for the current request.
    // @Query("SELECT * FROM todos WHERE id = :id AND userId = :userId")
    // TodoItem getItemByIdAndUser(int id, String userId);
}
