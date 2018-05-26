package com.dwett.habits;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Summary extends RecyclerView.Adapter<Summary.SummaryHolder> {

    private HabitDatabase db;
    private LinkedList<WeeklySummary> summaries;

    public Summary(HabitDatabase db) {
        this.summaries = computeSummaries(db);
    }

    @Override
    public SummaryHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.habit_summary, parent, false);

        return new SummaryHolder(view);
    }

    @Override
    public void onBindViewHolder(final SummaryHolder holder, int position) {
        final WeeklySummary thisSummary = this.summaries.get(position);
        holder.title.setText("Week of " + thisSummary.weekTitle);
        holder.listView.setAdapter(
                new SummaryListViewAdapter(
                        holder.listView.getContext(),
                        thisSummary.habitSummaries.toArray(
                                new HabitSummary[thisSummary.habitSummaries.size()]
                        )
                )
        );

        // Set the listView's height properly
        ViewGroup vg = holder.listView;
        int totalHeight = 0;
        for (int i = 0; i < holder.listView.getAdapter().getCount(); i++) {
            View listItem = holder.listView.getAdapter().getView(i, null, vg);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = holder.listView.getLayoutParams();
        params.height = totalHeight + (
                holder.listView.getDividerHeight() * (holder.listView.getAdapter().getCount() - 1)
        );
        holder.listView.setLayoutParams(params);
        holder.listView.requestLayout();
    }

    @Override
    public int getItemCount() {
        return this.summaries.size();
    }

    static class SummaryHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ListView listView;

        SummaryHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.summary_week_title);
            listView = itemView.findViewById(R.id.summary_week_list_view);
        }
    }

    private static LinkedList<WeeklySummary> computeSummaries(HabitDatabase db) {
        Map<Long, Event[]> habitIdToEvent = new HashMap<>();
        List<Habit> allHabits = new ArrayList<>();
        List<Event> allEvents = new ArrayList<>();
        for (Habit h : db.habitDao().loadAllHabits()) {
            Event[] events = db.habitDao().loadEventsForHabit(h.id);
            habitIdToEvent.put(h.id, events);
            allEvents.addAll(Arrays.asList(events));
            allHabits.add(h);
        }

        // Sort all the events by time
        Collections.sort(allEvents, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                if (o1.timestamp > o2.timestamp) {
                    return 1;
                }
                if (o1.timestamp < o2.timestamp) {
                    return -1;
                }
                return 0;
            }
        });

        // Sort all the habits so that higher frequency ones are first
        Collections.sort(allHabits, new Comparator<Habit>() {
            @Override
            public int compare(Habit o1, Habit o2) {
                return o2.frequency - o1.frequency;
            }
        });

        // Iterate through the events, compute the summary week
        List<String> summaryWeeks = new LinkedList<>();
        for (Event e : allEvents) {
            String summaryWeek = summaryWeekFromEvent(e);
            if (summaryWeeks.size() == 0 ||
                    !summaryWeeks.get(summaryWeeks.size() - 1).equals(summaryWeek)) {
                summaryWeeks.add(summaryWeek);
            }
        }

        // Now we finish populating everything
        LinkedList<WeeklySummary> summaries = new LinkedList<>();
        for (String summaryWeek : summaryWeeks) {
            WeeklySummary toAdd = new WeeklySummary(summaryWeek);
            for (Habit h: allHabits) {
                toAdd.addSummaryForHabit(h, habitIdToEvent.get(h.id));
            }
            summaries.addFirst(toAdd);
        }
        return summaries;
    }

    private static String summaryWeekFromEvent(Event e) {
        LocalDateTime time = Instant.ofEpochMilli(e.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        DayOfWeek today = time.getDayOfWeek();
        while (today != DayOfWeek.MONDAY) {
            time = time.minusDays(1);
            today = time.getDayOfWeek();
        }
        time = time.minusHours(time.getHour());
        time = time.minusMinutes(time.getMinute());
        time = time.minusSeconds(time.getSecond());
        return time.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    static class WeeklySummary {
        private String weekTitle;
        private LinkedList<HabitSummary> habitSummaries;

        public WeeklySummary(String weekTitle) {
            this.weekTitle = weekTitle;
            this.habitSummaries = new LinkedList<>();
        }

        public void addSummaryForHabit(Habit h, Event[] events) {
            this.habitSummaries.addLast(new HabitSummary(h, events, this.weekTitle));
        }
    }

    static class HabitSummary {
        private String title;
        private float progress;

        HabitSummary(Habit h, Event[] events, String curWeek) {
            this.progress = 0.0f;
            for (Event e : events) {
                String week = summaryWeekFromEvent(e);
                if (week.equals(curWeek)) {
                    progress += 1.0f;
                }
            }
            // Make progress a proportion of "done-ness" (0 means not, 1.0+ means done)
            progress /= h.frequency;

            this.title = h.title;
        }
    }

    private class SummaryListViewAdapter extends ArrayAdapter<HabitSummary> implements ListAdapter {

        private final Context context;
        private final HabitSummary[] summaries;

        public SummaryListViewAdapter(Context context,  HabitSummary[] summaries) {
            super(context, -1, summaries);
            this.context = context;
            this.summaries = summaries;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.habit_summary_list_element, parent, false);
            TextView habitTitle = rowView.findViewById(R.id.summary_habit_title);
            habitTitle.setText(summaries[position].title);

            ProgressBar progressBar = rowView.findViewById(R.id.summary_habit_progress);
            int progress = Math.round(summaries[position].progress * 100);
            progressBar.setProgress(progress);

            return rowView;
        }
    }
}
