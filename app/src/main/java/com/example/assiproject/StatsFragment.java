package com.example.assiproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat; // Added for percentage formatting
import java.util.List;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragmentDebug";

    public static final String USER_PREFS_NAME = "UserPrefs";
    public static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";

    private TextView tvTotalTasks, tvCompletedTasks, tvPendingTasks, tvCompletionPercentage; // Added tvCompletionPercentage
    private TodoDatabase db;
    private String currentUserId;

    private String getCurrentUserId() {
        if (getActivity() == null) {
            Log.e(TAG, "Activity is null, cannot get SharedPreferences.");
            return null;
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(LOGGED_IN_USER_ID_KEY, null);
        Log.d(TAG, "getCurrentUserId: Read from SharedPreferences: " + userId);
        return userId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = TodoDatabase.getInstance(requireContext());
        currentUserId = getCurrentUserId();
        Log.d(TAG, "onCreate: currentUserId initialized to: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);
        tvCompletionPercentage = view.findViewById(R.id.tvCompletionPercentage); // Initialize new TextView
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Started. Current currentUserId before update: " + currentUserId);
        String newUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(newUserId)) {
            Log.d(TAG, "onResume: User ID changed or was null. Old: " + currentUserId + ", New: " + newUserId);
            currentUserId = newUserId;
        }
        Log.d(TAG, "onResume: Calling loadStats. currentUserId is now: " + currentUserId);
        loadStats();
    }

    private void loadStats() {
        if (tvTotalTasks == null || tvCompletedTasks == null || tvPendingTasks == null || tvCompletionPercentage == null) { // Check new TextView
            Log.e(TAG, "TextViews not initialized in loadStats. One or more are null.");
             if (getView() != null) {
                 tvTotalTasks = getView().findViewById(R.id.tvTotalTasks);
                 tvCompletedTasks = getView().findViewById(R.id.tvCompletedTasks);
                 tvPendingTasks = getView().findViewById(R.id.tvPendingTasks);
                 tvCompletionPercentage = getView().findViewById(R.id.tvCompletionPercentage); // Re-acquire new TextView
                 if (tvTotalTasks == null || tvCompletedTasks == null || tvPendingTasks == null || tvCompletionPercentage == null) {
                    Log.e(TAG, "Failed to re-acquire TextViews in loadStats. Cannot update UI.");
                    return; 
                 }
            } else {
                 Log.e(TAG, "getView() is null in loadStats. Cannot re-acquire TextViews or update UI.");
                 return;
            }
        }

        if (currentUserId != null && !currentUserId.isEmpty() && db != null) {
            Log.d(TAG, "loadStats: Attempting to load items for currentUserId: " + currentUserId);
            List<TodoItem> userTodos = db.todoDao().getAllByUser(currentUserId);

            if (userTodos != null) {
                int totalTasks = userTodos.size();
                int completedTasks = 0;
                for (TodoItem item : userTodos) {
                    if (item.completed) {
                        completedTasks++;
                    }
                }
                int pendingTasks = totalTasks - completedTasks;
                double completionPercentage = (totalTasks == 0) ? 0.0 : (completedTasks * 100.0) / totalTasks;
                DecimalFormat df = new DecimalFormat("#.#"); // Format to one decimal place

                tvTotalTasks.setText("Total Tasks: " + totalTasks);
                tvCompletedTasks.setText("Completed Tasks: " + completedTasks);
                tvPendingTasks.setText("Pending Tasks: " + pendingTasks);
                tvCompletionPercentage.setText("Completion Rate: " + df.format(completionPercentage) + "%");
                Log.d(TAG, "loadStats: DAO returned " + totalTasks + " items. Completed: " + completedTasks + ", Pending: " + pendingTasks + ", Percentage: " + df.format(completionPercentage) + "%");
            } else {
                Log.d(TAG, "loadStats: userTodos list is null for user: " + currentUserId);
                setStatsToDefault();
            }
        } else {
            Log.d(TAG, "loadStats: Clearing stats because currentUserId is null/empty or db is null. UserID: " + currentUserId);
            setStatsToDefault();
        }
    }

    private void setStatsToDefault() {
        if (tvTotalTasks != null) tvTotalTasks.setText("Total Tasks: N/A");
        if (tvCompletedTasks != null) tvCompletedTasks.setText("Completed Tasks: N/A");
        if (tvPendingTasks != null) tvPendingTasks.setText("Pending Tasks: N/A");
        if (tvCompletionPercentage != null) tvCompletionPercentage.setText("Completion Rate: N/A"); // Reset percentage
    }
}
