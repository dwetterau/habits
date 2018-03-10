package com.dwett.habits;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;

public class HabitList extends RecyclerView.Adapter<HabitList.HabitHolder> {

    private LinkedList<Habit> habits;

    public HabitList(Habit[] habits) {
        this.habits = new LinkedList<>();
        Log.i("HABIT_HOLDER", "Number of habits: " + habits.length);
        this.habits.addAll(Arrays.asList(habits));
    }

    @Override
    public HabitHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.habit_list, parent, false);

        return new HabitHolder(view);
    }

    @Override
    public void onBindViewHolder(HabitHolder holder, int position) {
        Log.i("HABIT_HOLDER", "Binding habit at position" + position);
        holder.title.setText(this.habits.get(position).title);
        holder.description.setText("A cool description...");
    }

    public void addHabit(Habit h) {
        this.habits.add(h);
    }

    public void removeHabit(int index) {
        this.habits.remove(index);
    }

    @Override
    public int getItemCount() {
        return this.habits.size();
    }

    static class HabitHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView description;

        HabitHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habit_title);
            description = itemView.findViewById(R.id.habit_description);
        }

        // TODO: Add listeners?
    }
}
