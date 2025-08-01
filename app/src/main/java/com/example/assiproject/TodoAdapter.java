package com.example.assiproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private final List<TodoItem> items;
    private OnTodoItemStateChangeListener listener;

    public interface OnTodoItemStateChangeListener {
        void onItemCheckedChanged(TodoItem item, boolean isChecked);
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
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView text, description, date;
        CheckBox checkBox;
        TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.todoText);
            description = itemView.findViewById(R.id.todoDescription);
            date = itemView.findViewById(R.id.todoDate);
            checkBox = itemView.findViewById(R.id.todoCheck);
        }
    }
}
