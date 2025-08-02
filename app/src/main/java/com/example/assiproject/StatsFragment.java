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

import java.text.DecimalFormat;
import java.util.List;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragmentDebug";

    public static final String USER_PREFS_NAME = "UserPrefs";
    public static final String LOGGED_IN_USER_ID_KEY = "logged_in_user_id";

    // TextViews for displaying the *values*
    private TextView tvTotalTasksValue, tvCompletedTasksValue, tvPendingTasksValue, tvCompletionPercentageValue;
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
        // Initialize TextViews for values using the IDs from ConstraintLayout
        tvTotalTasksValue = view.findViewById(R.id.tvTotalTasksValue);
        tvCompletedTasksValue = view.findViewById(R.id.tvCompletedTasksValue);
        tvPendingTasksValue = view.findViewById(R.id.tvPendingTasksValue);
        tvCompletionPercentageValue = view.findViewById(R.id.tvCompletionPercentageValue);
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
        if (tvTotalTasksValue == null || tvCompletedTasksValue == null || tvPendingTasksValue == null || tvCompletionPercentageValue == null) {
            Log.e(TAG, "Value TextViews not initialized in loadStats. One or more are null.");
            if (getView() != null) { // Try to re-acquire views if they are null
                 tvTotalTasksValue = getView().findViewById(R.id.tvTotalTasksValue);
                 tvCompletedTasksValue = getView().findViewById(R.id.tvCompletedTasksValue);
                 tvPendingTasksValue = getView().findViewById(R.id.tvPendingTasksValue);
                 tvCompletionPercentageValue = getView().findViewById(R.id.tvCompletionPercentageValue);
                 if (tvTotalTasksValue == null || tvCompletedTasksValue == null || tvPendingTasksValue == null || tvCompletionPercentageValue == null) {
                    Log.e(TAG, "Failed to re-acquire Value TextViews in loadStats. Cannot update UI.");
                    return; // Exit if views are still null
                 }
            } else {
                 Log.e(TAG, "getView() is null in loadStats. Cannot re-acquire Value TextViews or update UI.");
                 return; // Exit if view is null
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
                DecimalFormat df = new DecimalFormat("#.#");

                // Set only the values to the respective TextViews
                tvTotalTasksValue.setText(String.valueOf(totalTasks));
                tvCompletedTasksValue.setText(String.valueOf(completedTasks));
                tvPendingTasksValue.setText(String.valueOf(pendingTasks));
                tvCompletionPercentageValue.setText(df.format(completionPercentage) + "%");
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
        String na = "N/A";
        if (tvTotalTasksValue != null) tvTotalTasksValue.setText(na);
        if (tvCompletedTasksValue != null) tvCompletedTasksValue.setText(na);
        if (tvPendingTasksValue != null) tvPendingTasksValue.setText(na);
        if (tvCompletionPercentageValue != null) tvCompletionPercentageValue.setText(na);
    }
}
