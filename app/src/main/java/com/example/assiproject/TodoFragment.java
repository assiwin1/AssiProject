package com.example.assiproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.text.DateFormat; // For date formatting
import java.util.Date;     // For current date

public class TodoFragment extends Fragment implements TodoAdapter.OnTodoItemStateChangeListener {

    private static final String TAG = "UserTodoDebug";

    // SharedPreferences constants
    public static final String USER_PREFS_NAME = "UserPrefs";
    public static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";

    private TodoAdapter adapter;
    private List<TodoItem> todoList = new ArrayList<>();
    private TodoDatabase db;
    private String currentUserId;

    private String getCurrentUserId() {
        if (getActivity() == null) {
            Log.e(TAG, "TodoFragment - getCurrentUserId: Activity is null, cannot get SharedPreferences.");
            return null;
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.d(TAG, "TodoFragment - getCurrentUserId: Read from SharedPreferences: " + userId);
        return userId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = TodoDatabase.getInstance(requireContext());
        currentUserId = getCurrentUserId();
        Log.d(TAG, "TodoFragment - onCreate: currentUserId initialized to: " + currentUserId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.todoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TodoAdapter(todoList, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddTodo);
        fab.setOnClickListener(v -> showAddDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "TodoFragment - onResume: Started. Current currentUserId before update: " + currentUserId);
        String newUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(newUserId)) {
            Log.d(TAG, "TodoFragment - onResume: User ID changed or was null. Old: " + currentUserId + ", New: " + newUserId);
            currentUserId = newUserId;
        }
        Log.d(TAG, "TodoFragment - onResume: Calling loadTodoItems. currentUserId is now: " + currentUserId);
        loadTodoItems();
    }

    private void loadTodoItems() {
        Log.d(TAG, "TodoFragment - loadTodoItems: Attempting to load items for currentUserId: " + currentUserId);
        if (currentUserId != null && !currentUserId.isEmpty() && db != null) {
            todoList.clear();
            List<TodoItem> userTodos = db.todoDao().getAllByUser(currentUserId);
            Log.d(TAG, "TodoFragment - loadTodoItems: DAO returned " + (userTodos != null ? userTodos.size() : "null list") + " items for user: " + currentUserId);
            if (userTodos != null) {
                todoList.addAll(userTodos);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else {
            Log.d(TAG, "TodoFragment - loadTodoItems: Clearing list because currentUserId is null/empty or db is null. UserID: " + currentUserId);
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

        Log.d(TAG, "TodoFragment - showAddDialog: Dialog created. currentUserId: " + currentUserId);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Todo")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = inputTitle.getText().toString().trim();
                    String description = inputDescription.getText().toString().trim();
                    String date = DateFormat.getDateTimeInstance().format(new Date());

                    Log.d(TAG, "TodoFragment - showAddDialog positive button: Attempting to add item. currentUserId: " + currentUserId);

                    if (!title.isEmpty() && currentUserId != null && !currentUserId.isEmpty()) {
                        Log.d(TAG, "TodoFragment - showAddDialog positive button: Saving item with userId: " + currentUserId);
                        TodoItem item = new TodoItem(title, description, date, currentUserId);
                        db.todoDao().insert(item);
                        loadTodoItems();
                    } else {
                        Log.d(TAG, "TodoFragment - showAddDialog positive button: Not saving item. Title empty or currentUserId invalid. UserID: " + currentUserId);
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

    @Override
    public void onItemEditClicked(TodoItem item) {
        if (item.userId == null || !item.userId.equals(currentUserId)) {
            Log.w(TAG, "TodoFragment - onItemEditClicked: Item's userId (" + item.userId + ") does not match currentUserId (" + currentUserId + "). Cannot edit.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Cannot edit item: not owned by current user.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        showEditDialog(item);
    }

    @Override
    public void onItemDeleteClicked(TodoItem item) {
        if (item.userId == null || !item.userId.equals(currentUserId)) {
            Log.w(TAG, "TodoFragment - onItemDeleteClicked: Item's userId (" + item.userId + ") does not match currentUserId (" + currentUserId + "). Cannot delete.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Cannot delete item: not owned by current user.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        showDeleteConfirmationDialog(item);
    }

    private void showEditDialog(final TodoItem itemToEdit) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_todo, null); // Reuse dialog_add_todo

        final EditText inputTitle = dialogView.findViewById(R.id.inputTodoTitle);
        final EditText inputDescription = dialogView.findViewById(R.id.inputTodoDescription);

        inputTitle.setText(itemToEdit.text);
        inputDescription.setText(itemToEdit.description);

        Log.d(TAG, "TodoFragment - showEditDialog: Editing item. currentUserId: " + currentUserId);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Todo")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = inputTitle.getText().toString().trim();
                    String description = inputDescription.getText().toString().trim();

                    if (!title.isEmpty()) {
                        itemToEdit.text = title;
                        itemToEdit.description = description;
                        // itemToEdit.date = DateFormat.getDateTimeInstance().format(new Date()); // Optionally update date
                        db.todoDao().update(itemToEdit);
                        loadTodoItems();
                        Log.d(TAG, "TodoFragment - showEditDialog: Item updated. Title: " + title);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Title cannot be empty.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog(final TodoItem itemToDelete) {
        Log.d(TAG, "TodoFragment - showDeleteConfirmationDialog: Confirming delete for item. currentUserId: " + currentUserId);
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this task: '" + itemToDelete.text + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.todoDao().delete(itemToDelete);
                    loadTodoItems();
                    Log.d(TAG, "TodoFragment - showDeleteConfirmationDialog: Item deleted. Title: " + itemToDelete.text);
                    if (getContext() != null) {
                         Toast.makeText(getContext(), "Task deleted.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
