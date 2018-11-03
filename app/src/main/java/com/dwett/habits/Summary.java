package com.dwett.habits;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Summary extends RecyclerView.Adapter<Summary.SummaryHolder> {

    private HabitDatabase db;
    private Consumer<Pair<Habit, Event[]>> editHabitCallback;
    private LinkedList<WeeklySummary> summaries;

    public Summary(HabitDatabase db,  Consumer<Pair<Habit, Event[]>> editHabitCallback) {
        this.db = db;
        this.summaries = computeSummaries(db);
        this.editHabitCallback = editHabitCallback;
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

    private static long getEventStartTimestampMillis() {
        LocalDateTime start = LocalDateTime.now();
        DayOfWeek today = start.getDayOfWeek();
        while (today != DayOfWeek.MONDAY) {
            start = start.minusDays(1);
            today = start.getDayOfWeek();
        }
        start = start.minusHours(start.getHour());
        start = start.minusMinutes(start.getMinute());
        start = start.minusSeconds(start.getSecond());

        // Only return 6 weeks back?
        // TODO: add in some pagination
        start = start.minusWeeks(6);
        return start.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
    }

    private static LinkedList<WeeklySummary> computeSummaries(HabitDatabase db) {
        Map<Long, List<Event>> habitIDToEvent = new HashMap<>();
        Map<Long, Habit> habitIDToHabit = new HashMap<>();
        List<Event> allEvents = new ArrayList<>();
        for (Habit h : db.habitDao().loadAllHabits()) {
            habitIDToHabit.put(h.id, h);
        }

        for (Event e : db.habitDao().loadAllEventsSince(getEventStartTimestampMillis())) {
            if (!habitIDToEvent.containsKey(e.habitId)) {
                habitIDToEvent.put(e.habitId, new LinkedList<>());
            }
            habitIDToEvent.get(e.habitId).add(e);
            allEvents.add(e);
        }

        // Sort all the events by time
        Collections.sort(allEvents, Comparator.comparingLong(o -> o.timestamp));

        // Iterate through the events, compute the summary week and the start and end week for each
        // habit
        Map<Long, String> habitIdToStartWeek = new HashMap<>(habitIDToHabit.size());
        Map<Long, String> habitIdToEndWeek = new HashMap<>(habitIDToHabit.size());
        Map<Long, String> eventIdToSummaryWeek = new HashMap<>(allEvents.size());
        LinkedList<Habit> habitsWithEventsInPage = new LinkedList<>();
        LinkedList<String> summaryWeeks = new LinkedList<>();

        // We assume all non-archived events are still active
        String currentSummaryWeek = currentSummaryWeek();
        for (Habit h : habitIDToHabit.values()) {
            if (h.archived) {
                continue;
            }
            habitIdToEndWeek.put(h.id, currentSummaryWeek);
        }

        for (Event e : allEvents) {
            String summaryWeek = summaryWeekFromEvent(e);
            eventIdToSummaryWeek.put(e.id, summaryWeek);

            if (!habitIdToStartWeek.containsKey(e.habitId)) {
                habitsWithEventsInPage.add(habitIDToHabit.get(e.habitId));
                habitIdToStartWeek.put(e.habitId, summaryWeek);
            }
            if (habitIDToHabit.get(e.habitId).archived) {
                habitIdToEndWeek.put(e.habitId, summaryWeek);
            }

            if (summaryWeeks.size() == 0 || !summaryWeeks.getLast().equals(summaryWeek)) {
                summaryWeeks.add(summaryWeek);
            }
        }
        // Sort all the habits so that higher frequency ones are first
        Collections.sort(habitsWithEventsInPage, (o1, o2) -> o2.frequency - o1.frequency);

        // Now we finish populating everything
        Set<Long> startedHabits = new HashSet<>(habitsWithEventsInPage.size());
        Set<Long> endedHabits = new HashSet<>(habitsWithEventsInPage.size());
        LinkedList<WeeklySummary> summaries = new LinkedList<>();
        for (String summaryWeek : summaryWeeks) {
            WeeklySummary toAdd = new WeeklySummary(summaryWeek);
            for (Habit h: habitsWithEventsInPage) {
                if (habitIdToStartWeek.get(h.id).equals(summaryWeek)) {
                    startedHabits.add(h.id);
                }
                boolean justEnded = false;
                if (habitIdToEndWeek.get(h.id).equals(summaryWeek)) {
                    justEnded = true;
                    endedHabits.add(h.id);
                }

                // Only add a summary if this is after the start week or before or equal the end week
                if (startedHabits.contains(h.id) && (!endedHabits.contains(h.id) || justEnded)) {
                    // Omit entries for archived habits if they have no events for the week.
                    List<Event> eventsInWeek = habitIDToEvent
                            .getOrDefault(h.id, new LinkedList<>())
                            .stream()
                            .filter(
                                (event) -> eventIdToSummaryWeek.get(event.id).equals(summaryWeek)
                            )
                            .collect(Collectors.toList());
                    if (eventsInWeek.size() == 0 && h.archived) {
                        continue;
                    }
                    toAdd.addSummaryForHabit(h, eventsInWeek);
                }
            }
            summaries.addFirst(toAdd);
        }
        return summaries;
    }

    private static String currentSummaryWeek() {
        LocalDateTime time = LocalDateTime.now();
        return summaryWeekFromTime(time);
    }

    private static String summaryWeekFromEvent(Event e) {
        LocalDateTime time = Instant.ofEpochMilli(e.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return summaryWeekFromTime(time);
    }

    private static String summaryWeekFromTime(LocalDateTime time) {
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

        public void addSummaryForHabit(Habit h, List<Event> events) {
            this.habitSummaries.addLast(new HabitSummary(h, events));
        }
    }

    static class HabitSummary {
        private long id;
        private String title;
        private float progress;

        HabitSummary(Habit h, List<Event> events) {
            this.id = h.id;
            this.progress = (float) events.size();

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

            final long id = summaries[position].id;
            rowView.setOnClickListener(v -> {
                Habit habit = db.habitDao().loadHabit(id);
                Event[] events = db.habitDao().loadAllEventsForHabit(id);

                // Go to the tab to edit the habit.timestamp
                editHabitCallback.accept(new Pair<>(habit, events));
            });

            return rowView;
        }
    }
}
