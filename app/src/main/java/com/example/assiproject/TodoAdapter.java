package com.example.assiproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView; // Added
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private final List<TodoItem> items;
    private OnTodoItemStateChangeListener listener;

    // Updated Listener interface
    public interface OnTodoItemStateChangeListener {
        void onItemCheckedChanged(TodoItem item, boolean isChecked);
        void onItemEditClicked(TodoItem item);     // New method for edit
        void onItemDeleteClicked(TodoItem item);   // New method for delete
    }

    public TodoAdapter(List<TodoItem> items, OnTodoItemStateChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        final TodoItem item = items.get(position);
        holder.text.setText(item.text);
        holder.description.setText(item.description);
        holder.date.setText(item.date);
        holder.checkBox.setChecked(item.completed);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onItemCheckedChanged(item, isChecked);
            }
        });

        // Set click listeners for edit and delete buttons
        holder.btnEditTodo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemEditClicked(item);
            }
        });

        holder.btnDeleteTodo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemDeleteClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // Updated ViewHolder
    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView text, description, date;
        CheckBox checkBox;
        ImageView btnEditTodo, btnDeleteTodo; // Added ImageViews for buttons

        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.todoText);
            description = itemView.findViewById(R.id.todoDescription);
            date = itemView.findViewById(R.id.todoDate);
            checkBox = itemView.findViewById(R.id.todoCheck);
            btnEditTodo = itemView.findViewById(R.id.btnEditTodo);     // Initialize edit button
            btnDeleteTodo = itemView.findViewById(R.id.btnDeleteTodo); // Initialize delete button
        }
    }
}
