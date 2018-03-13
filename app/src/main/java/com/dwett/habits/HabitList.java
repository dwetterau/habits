package com.dwett.habits;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;

public class HabitList extends RecyclerView.Adapter<HabitList.HabitHolder> {

    private LinkedList<Habit> habits;
    private HabitDatabase db;

    public HabitList(Habit[] habits, HabitDatabase db) {
        this.habits = new LinkedList<>();
        this.habits.addAll(Arrays.asList(habits));
        this.db = db;
    }

    @Override
    public HabitHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.habit_list, parent, false);

        return new HabitHolder(view);
    }

    @Override
    public void onBindViewHolder(final HabitHolder holder, int position) {
        final HabitList thisList = this;
        final Habit thisHabit = this.habits.get(position);
        holder.title.setText(thisHabit.title);
        Event[] events = db.habitDao().loadEventsForHabit(thisHabit.id);
        holder.description.setText("Done " + events.length + " times.");

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Habit thisHabit = thisList.habits.get(holder.getAdapterPosition());
                db.habitDao().deleteHabit(thisHabit);
                thisList.removeHabit(holder.getAdapterPosition());
            }
        });

        holder.doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Event event = new Event();

                Habit thisHabit = thisList.habits.get(holder.getAdapterPosition());
                event.habitId = thisHabit.id;

                event.timestamp = System.currentTimeMillis();
                db.habitDao().insertNewEvent(event);
                thisList.notifyHabitUpdated(holder.getAdapterPosition());
            }
        });
    }

    public void addHabit(Habit h) {
        this.habits.add(h);
        this.notifyItemInserted(this.getItemCount() - 1);
    }

    public void removeHabit(int index) {
        this.habits.remove(index);
        this.notifyItemRangeRemoved(index, 1);
        this.notifyItemRangeChanged(index, this.getItemCount());
    }

    public void notifyHabitUpdated(int index) {
        this.notifyItemChanged(index);
    }

    @Override
    public int getItemCount() {
        return this.habits.size();
    }

    static class HabitHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView description;
        private Button deleteButton;
        private Button doneButton;

        HabitHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habit_title);
            description = itemView.findViewById(R.id.habit_description);
            deleteButton = itemView.findViewById(R.id.habit_delete_button);
            doneButton = itemView.findViewById(R.id.habit_done_button);
        }

        // TODO: Add listeners?
    }
}
