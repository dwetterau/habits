package com.dwett.habits;

import android.content.res.ColorStateList;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
        Event[] events = HabitLogic.loadEventsInCurrentPeriod(db.habitDao(), thisHabit);
        holder.description.setText(HabitLogic.getDescription(thisHabit, events));


        holder.progressBar.setProgress(HabitLogic.currentProgress(thisHabit, events));
        if (HabitLogic.onTrack(thisHabit, events, 50)) {
            holder.progressBar.setProgressTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.colorAccent)));
        } else if (HabitLogic.onTrack(thisHabit, events, 30)) {
            holder.progressBar.setProgressTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.yellow)));
        } else {
            holder.progressBar.setProgressTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.colorPrimary)));
        }

        holder.itemView.setOnLongClickListener(v -> {
            Event[] events1 = db.habitDao().loadAllEventsForHabit(thisHabit.id);

            editHabitCallback.accept(new Pair<>(thisHabit, events1));
            return true;
        });

        if (!HabitLogic.isDone(thisHabit, events)) {
            holder.doneButton.setOnClickListener(v -> {
                Event event = new Event();

                event.habitId = thisHabit.id;

                event.timestamp = System.currentTimeMillis();
                if (event.maybeAdjustTimestampToPreviousDay()) {
                    Toast.makeText(
                            v.getContext(),
                            "Recorded event to yesterday.",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                db.habitDao().insertNewEvent(event);

                Event[] events1 = HabitLogic.loadEventsInCurrentPeriod(db.habitDao(), thisHabit);
                if (HabitLogic.isDone(thisHabit, events1)) {
                    // Habit is finished, re-sort!
                    if (!thisList.sort()) {
                        thisList.notifyHabitUpdated(thisHabit);
                    }
                } else {
                    thisList.notifyHabitUpdated(thisHabit);
                }
            });
            holder.doneButton.setBackgroundResource(R.drawable.ic_check_black_24dp);
            holder.doneButton.setEnabled(true);
        } else {
            holder.doneButton.setBackgroundResource(R.drawable.ic_check_primary_24dp);
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
        try {
            this.getHabitIndex(h);
        } catch (RuntimeException e) {
            // Whatever, just don't blow up.
            return;
        }
        this.notifyItemChanged(this.getHabitIndex(h));
    }

    @Override
    public int getItemCount() {
        return this.habits.size();
    }

    // Returns whether or not the list was refreshed
    public boolean sort() {
        long[] originalIDOrder = new long[habits.size()];
        final Map<Long, Boolean> idToIsDone = new HashMap<>(habits.size());
        int i = 0;
        for (Habit h : habits) {
            Event[] events = HabitLogic.loadEventsInCurrentPeriod(db.habitDao(), h);
            idToIsDone.put(h.id, HabitLogic.isDone(h, events));
            originalIDOrder[i++] = h.id;
        }
        Collections.sort(this.habits, (o1, o2) -> {
            boolean o1IsDone = idToIsDone.get(o1.id);
            boolean o2IsDone = idToIsDone.get(o2.id);
            if (o1IsDone && !o2IsDone) {
                return 1;
            }
            if (!o1IsDone && o2IsDone) {
                return -1;
            }

            // Sort events that have a lower period first (e.g. daily before weekly)
            if (o1.period < o2.period) {
                return -1;
            }
            if (o1.period > o2.period) {
                return 1;
            }

            // Sort events that must be done more often first (e.g. twice daily before once)
            return o2.frequency - o1.frequency;
        });

        // Only notify on change if we changed something...
        i = 0;
        for (Habit h : habits) {
            if (h.id != originalIDOrder[i++]) {
                this.notifyItemRangeChanged(0, this.habits.size());
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.habits.size() == 0;
    }

    static class HabitHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView description;
        private Button doneButton;
        private ProgressBar progressBar;

        HabitHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.habit_title);
            description = itemView.findViewById(R.id.habit_description);
            doneButton = itemView.findViewById(R.id.habit_done_button);
            progressBar = itemView.findViewById(R.id.habit_progress);
        }
    }
}
