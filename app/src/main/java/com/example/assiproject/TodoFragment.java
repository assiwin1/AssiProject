package com.example.assiproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Existing import
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast; // Kept for your existing Toasts, can be removed if not needed

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TodoFragment extends Fragment implements TodoAdapter.OnTodoItemStateChangeListener {

    private static final String TAG = "UserTodoDebug"; // <<< ADDED TAG DEFINITION

    // SharedPreferences constants
    public static final String USER_PREFS_NAME = "UserPrefs";
    public static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";

    private TodoAdapter adapter;
    private List<TodoItem> todoList = new ArrayList<>();
    private TodoDatabase db;
    private String currentUserId;

    private String getCurrentUserId() {
        if (getActivity() == null) { // Guard clause
            Log.e(TAG, "TodoFragment - getCurrentUserId: Activity is null, cannot get SharedPreferences.");
            return null;
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.d(TAG, "TodoFragment - getCurrentUserId: Read from SharedPreferences: " + userId); // <<< ADDED Log.d
        return userId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = TodoDatabase.getInstance(requireContext());
        currentUserId = getCurrentUserId(); // This will now log
        Log.d(TAG, "TodoFragment - onCreate: currentUserId initialized to: " + currentUserId); // <<< ADDED Log.d
        // Your existing Toast for debugging:
        // Toast.makeText(getContext(), "TodoFragment UserID (onCreate): " + currentUserId, Toast.LENGTH_LONG).show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.todoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // loadTodoItems() is called in onResume, which is called after onCreateView
        // and also when the fragment becomes visible again.

        adapter = new TodoAdapter(todoList, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddTodo);
        fab.setOnClickListener(v -> showAddDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "TodoFragment - onResume: Started. Current currentUserId before update: " + currentUserId); // <<< ADDED Log.d
        String newUserId = getCurrentUserId(); // This will log the read value
        if (currentUserId == null || !currentUserId.equals(newUserId)) {
            Log.d(TAG, "TodoFragment - onResume: User ID changed or was null. Old: " + currentUserId + ", New: " + newUserId); // <<< ADDED Log.d
            currentUserId = newUserId;
        }
        Log.d(TAG, "TodoFragment - onResume: Calling loadTodoItems. currentUserId is now: " + currentUserId); // <<< ADDED Log.d
        loadTodoItems();
    }

    private void loadTodoItems() {
        Log.d(TAG, "TodoFragment - loadTodoItems: Attempting to load items for currentUserId: " + currentUserId); // <<< ADDED Log.d
        if (currentUserId != null && !currentUserId.isEmpty() && db != null) {
            todoList.clear();
            List<TodoItem> userTodos = db.todoDao().getAllByUser(currentUserId);
            Log.d(TAG, "TodoFragment - loadTodoItems: DAO returned " + (userTodos != null ? userTodos.size() : "null list") + " items for user: " + currentUserId); // <<< ADDED Log.d
            if (userTodos != null) {
                todoList.addAll(userTodos);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d(TAG, "TodoFragment - loadTodoItems: Clearing list because currentUserId is null/empty or db is null. UserID: " + currentUserId); // <<< ADDED Log.d
            todoList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showAddDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_todo, null);

        EditText inputTitle = dialogView.findViewById(R.id.inputTodoTitle);
        EditText inputDescription = dialogView.findViewById(R.id.inputTodoDescription);

        Log.d(TAG, "TodoFragment - showAddDialog: Dialog created. currentUserId: " + currentUserId); // <<< ADDED Log.d for context

        new AlertDialog.Builder(getContext())
                .setTitle("Add Todo")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = inputTitle.getText().toString().trim();
                    String description = inputDescription.getText().toString().trim();
                    String date = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

                    Log.d(TAG, "TodoFragment - showAddDialog positive button: Attempting to add item. currentUserId: " + currentUserId); // <<< ADDED Log.d

                    if (!title.isEmpty() && currentUserId != null && !currentUserId.isEmpty()) {
                        Log.d(TAG, "TodoFragment - showAddDialog positive button: Saving item with userId: " + currentUserId); // <<< ADDED Log.d
                        TodoItem item = new TodoItem(title, description, date, currentUserId);
                        long id = db.todoDao().insert(item);
                        // item.id = (int) id; // This line might not be needed if you reload
                        loadTodoItems(); // Reload the list from DB to ensure consistency
                    } else {
                        Log.d(TAG, "TodoFragment - showAddDialog positive button: Not saving item. Title empty or currentUserId invalid. UserID: " + currentUserId); // <<< ADDED Log.d
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Title cannot be empty and user must be logged in.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onItemCheckedChanged(TodoItem item, boolean isChecked) {
        if (item.userId == null || !item.userId.equals(currentUserId)) {
            Log.w(TAG, "TodoFragment - onItemCheckedChanged: Item's userId (" + item.userId + ") does not match currentUserId (" + currentUserId + "). Item: " + item.text);
            return;
        }
        item.completed = isChecked;
        if (db != null) {
            db.todoDao().update(item);
        }
    }
}
