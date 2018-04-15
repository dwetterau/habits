package com.dwett.habits;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;

public class HabitList extends RecyclerView.Adapter<HabitList.HabitHolder> {

    private LinkedList<Habit> habits;
    private HabitDatabase db;
    private Consumer<Pair<Habit, Event[]>> editHabitCallback;

    public HabitList(Habit[] habits, HabitDatabase db, Consumer<Pair<Habit, Event[]>> editHabitCallback) {
        this.habits = new LinkedList<>();
        this.habits.addAll(Arrays.asList(habits));
        this.db = db;
        this.editHabitCallback = editHabitCallback;
    }

    @Override
    public HabitHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
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
        holder.description.setText(HabitLogic.getDescription(thisHabit, events));

        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Habit thisHabit = thisList.habits.get(holder.getAdapterPosition());
                Event[] events = db.habitDao().loadEventsForHabit(thisHabit.id);

                // Go to the tab to edit the habit
                editHabitCallback.accept(new Pair<>(thisHabit, events));
            }
        });

        if (HabitLogic.shouldAllowDone(thisHabit, events)) {
            holder.doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Event event = new Event();

                    Habit thisHabit = thisList.habits.get(holder.getAdapterPosition());
                    event.habitId = thisHabit.id;

                    event.timestamp = System.currentTimeMillis();
                    db.habitDao().insertNewEvent(event);
                    thisList.notifyHabitUpdated(thisHabit);
                }
            });
            holder.doneButton.setEnabled(true);
        } else {
            holder.doneButton.setEnabled(false);
        }
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

    public int getHabitIndex(Habit h) {
        int i = 0;
        for (Habit cur : this.habits) {
            if (cur.id == h.id) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Unexpected habit passed to getHabitIndex " + h.id);
    }

    public void notifyHabitUpdated(Habit h) {
         this.notifyItemChanged(this.getHabitIndex(h));
    }

    @Override
    public int getItemCount() {
        return this.habits.size();
    }

    static class HabitHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView description;
        private Button editButton;
        private Button doneButton;

        HabitHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habit_title);
            description = itemView.findViewById(R.id.habit_description);
            editButton = itemView.findViewById(R.id.habit_edit_button);
            doneButton = itemView.findViewById(R.id.habit_done_button);
        }

        // TODO: Add listeners?
    }
}
