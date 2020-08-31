package com.dwett.habits;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private HabitList habitList;
    private RecyclerView habitListRecyclerView;
    private RecyclerView summaryRecyclerView;
    private TextView noHabitsWarningView;
    private HabitDatabase db;

    private View manageHabitView;

    private Habit habitToEdit;
    private Event[] eventsForHabitToEdit;
    private habitEditor habitEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(
                getApplicationContext(),
                HabitDatabase.class,
                "habits"
        )
                .addMigrations(Migrations.MIGRATION_1_2)
                // TODO(davidw): Remove this!
                .allowMainThreadQueries()
                .build();

        this.habitEditor = new habitEditor(this);
        Habit[] allHabits = db.habitDao().loadNonArchivedHabits();
        habitList = new HabitList(allHabits, db, this.habitEditor);
        habitList.sort();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(item -> inflateBasedOffMenuItem(item.getItemId()));
        this.inflateBasedOffMenuItem(navigation.getSelectedItemId());

        // Set up the reminder
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
        @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(
                NotificationScheduler.CHANNEL_ID,
                name,
                importance);
        channel.setDescription(description);
        // Register the channel with the system
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        NotificationScheduler.scheduleAlarm(this, AlarmReceiver.class);

        // Clear out any notifications that already exist
        notificationManager.cancel(NotificationScheduler.REMINDER_REQUEST_CODE);
    }

    private void setHabitToEdit(Habit h, Event[] events) {
        this.habitToEdit = h;
        this.eventsForHabitToEdit = events;
    }

    private class habitEditor implements Consumer<Pair<Habit, Event[]>> {
        MainActivity a;

        public habitEditor(MainActivity a) {
            this.a = a;
        }

        @Override
        public void accept(Pair<Habit, Event[]> pair) {
            a.setHabitToEdit(pair.first, pair.second);
            BottomNavigationView navigation = findViewById(R.id.navigation);
            navigation.setSelectedItemId(R.id.navigation_manage);
            a.inflateBasedOffMenuItem(R.id.navigation_manage);
        }
    }

    /*
     * Returns if a known type was selected
     */
    private boolean inflateBasedOffMenuItem(int item) {
        switch (item) {
            case R.id.navigation_summary:
                this.hideManageHabits();
                this.hideHabitList();
                this.inflateSummaryView();
                break;
            case R.id.navigation_habits:
                this.hideManageHabits();
                this.hideSummaryView();
                // Inflate the habit list view
                this.inflateHabitList();
                break;
            case R.id.navigation_manage:
                this.hideHabitList();
                this.hideSummaryView();
                // Inflate the view to create habits
                this.inflateManageHabits();
                break;
            default:
                return false;
        }
        return true;
    }

    private void maybeShowNoHabitWarning(boolean isEmpty) {
        if (noHabitsWarningView == null) {
            noHabitsWarningView = findViewById(R.id.no_habit_warning);
        }
        if (isEmpty) {
            noHabitsWarningView.setVisibility(View.VISIBLE);
        } else {
            noHabitsWarningView.setVisibility(View.INVISIBLE);
        }
    }

    private void maybeHideNoHabitWarning() {
        if (noHabitsWarningView == null) {
            noHabitsWarningView = findViewById(R.id.no_habit_warning);
        }
        noHabitsWarningView.setVisibility(View.INVISIBLE);
    }

    private void inflateHabitList() {
        if (habitListRecyclerView == null) {
            habitListRecyclerView = findViewById(R.id.habit_list_recycler_view);
            habitListRecyclerView.setHasFixedSize(true);
            RecyclerView.Adapter habitListRecyclerViewAdapter = habitList;
            habitListRecyclerView.setAdapter(habitListRecyclerViewAdapter);
            RecyclerView.LayoutManager habitListRecyclerViewLayoutManager = new LinearLayoutManager(this);
            habitListRecyclerView.setLayoutManager(habitListRecyclerViewLayoutManager);
        }
        habitList.sort();
        habitListRecyclerView.setVisibility(View.VISIBLE);
        maybeShowNoHabitWarning(habitList.isEmpty());
    }

    private void hideHabitList() {
        habitListRecyclerView.setVisibility(View.INVISIBLE);
        maybeHideNoHabitWarning();
    }

    private void inflateSummaryView() {
        summaryRecyclerView = findViewById(R.id.summary_recycler_view);
        RecyclerView.Adapter summaryViewAdapter = new Summary(this.db, this.habitEditor);
        summaryRecyclerView.setAdapter(summaryViewAdapter);
        RecyclerView.LayoutManager summaryRecyclerViewLayoutManager = new LinearLayoutManager(this);
        summaryRecyclerView.setLayoutManager(summaryRecyclerViewLayoutManager);
        summaryRecyclerView.setVisibility(View.VISIBLE);
        summaryRecyclerView.post(() -> summaryRecyclerView.scrollToPosition(0));
        maybeShowNoHabitWarning(summaryViewAdapter.getItemCount() == 0);
    }

    private void hideSummaryView() {
        if (summaryRecyclerView != null) {
            summaryRecyclerView.setVisibility(View.INVISIBLE);
        }
        maybeHideNoHabitWarning();
    }

    private void inflateManageHabits() {
        boolean firstInitialization = manageHabitView == null;
        if (firstInitialization) {
            manageHabitView = getLayoutInflater().inflate(
                    R.layout.create_habit,
                    null);
        } else {
            manageHabitView.setVisibility(View.VISIBLE);
        }

        Button habitCreateButton = manageHabitView.findViewById(R.id.habit_create_button);
        final Button habitExportButton = manageHabitView.findViewById(R.id.export_button);
        final Button habitImportButton = manageHabitView.findViewById(R.id.import_button);
        habitCreateButton.setOnClickListener(v -> {
            Habit h = new Habit();
            AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
            EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);

            // Default to weekly
            h.period = 7 * 24;

            h.title = habitCreateTextInput.getText().toString();

            String frequencyString = habitCreateFrequencyInput.getText().toString();
            if (frequencyString.length() > 0) {
                h.frequency = Integer.parseInt(frequencyString);
            } else {
                // Default to once / period
                h.frequency = 1;
            }

            // Error out on empty title habits
            if (h.title.length() == 0) {
                habitCreateTextInput.setError("Habits must have a title");
                return;
            }
            if (h.period != 7 * 24) {
                // TODO: Make this error on something sane, remove the field from the
                // UI once I'm sure it's useless
                habitCreateTextInput.setError("Only weekly habits supported");
                return;
            }

            h.id = db.habitDao().insertNewHabit(h);
            habitList.addHabit(h);
            habitCreateTextInput.setText("");
            habitCreateFrequencyInput.setText("");

            // Close the keyboard hackily?
            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(habitCreateTextInput.getWindowToken(), 0);

            // Let's also navigate away to the habits view
            BottomNavigationView navigation = findViewById(R.id.navigation);
            navigation.setSelectedItemId(R.id.navigation_habits);
            inflateBasedOffMenuItem(R.id.navigation_habits);
        });

        habitExportButton.setOnClickListener(v -> {
            Intent exportActivity = new Intent(this, DisplayExportActivity.class);
            Habit[] habits = db.habitDao().loadAllHabits();
            Event[] events = db.habitDao().loadAllEventsSince(0);
            String habitsCSV = String.join("\n", Arrays.stream(habits).map(Habit::csv).collect(Collectors.toList()));
            String eventsCSV = String.join("\n", Arrays.stream(events).map(Event::csv).collect(Collectors.toList()));
            exportActivity.putExtra("habits", habitsCSV);
            exportActivity.putExtra("events", eventsCSV);
            startActivity(exportActivity);
        });

        habitImportButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Import Habits");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);

            builder.setPositiveButton("Import", (dialog, which) -> {
                String importText = input.getText().toString();
                String[] allLines = importText.split("\n");

                // Some sanity checks over the format
                int eventStart = -1;
                boolean valid = true;
                if (allLines.length < 3) {
                    valid = false;
                } else if (!allLines[0].equals("Habits")) {
                    valid = false;
                } else if (!(allLines[1] + "\n").equals(Habit.csvHeader())) {
                    valid = false;
                } else {
                    // Find the index where events start.
                    for (int i = 2; i < allLines.length; i++) {
                        if (allLines[i].length() == 0) {
                            eventStart = i + 1;
                            break;
                        }
                    }
                    if (eventStart == -1) {
                        valid = false;
                    } else if (!allLines[eventStart].equals("Events")) {
                    valid = false;
                } else if (!(allLines[eventStart + 1] + "\n").equals(Event.csvHeader())) {
                        valid = false;
                    }
                }

                if (!valid) {
                    Toast.makeText(
                            v.getContext(),
                            "Invalid input data!",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                String[] habitLines = Arrays.copyOfRange(allLines, 2, eventStart - 1);
                String[] eventLines = Arrays.copyOfRange(allLines, eventStart + 2, allLines.length);

                Habit[] allHabits = db.habitDao().loadAllHabits();
                Event[] allEvents = db.habitDao().loadAllEventsSince(0);
                HashSet<Long> habitIDs = new HashSet<>(allHabits.length);
                for (Habit h : allHabits) {
                    habitIDs.add(h.id);
                }
                HashSet<Long> eventIDs = new HashSet<>(allEvents.length);
                for (Event e : allEvents) {
                    eventIDs.add(e.id);
                }

                for (String habitCSV : habitLines) {
                    if (habitCSV.length() == 0) {
                        continue;
                    }
                    Habit h = Habit.fromCSV(habitCSV);
                    if (habitIDs.contains(h.id)) {
                        continue;
                    }
                    db.habitDao().insertNewHabit(h);
                }
                for (String eventCSV : eventLines) {
                    if (eventCSV.length() == 0) {
                        continue;
                    }
                    Event e = Event.fromCSV(eventCSV);
                    if (eventIDs.contains(e.id)) {
                        continue;
                    }
                    db.habitDao().insertNewEvent(e);
                }
                Toast.makeText(
                        v.getContext(),
                        String.format(
                                Locale.ENGLISH,
                                "Imported %d habits and %d events!",
                                habitLines.length,
                                eventLines.length
                        ),
                        Toast.LENGTH_SHORT
                ).show();
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.cancel();
            });
            builder.show();
        });


        RecyclerView eventListRecyclerView = manageHabitView.findViewById(R.id.event_list_recycler_view);
        EventList events = new EventList(new Event[]{}, db, getFragmentManager());
        eventListRecyclerView.setAdapter(events);
        RecyclerView.LayoutManager eventListRecyclerViewLayoutManager = new LinearLayoutManager(
                manageHabitView.getContext()
        );
        eventListRecyclerView.setLayoutManager(eventListRecyclerViewLayoutManager);

        final Switch habitArchiveSwitch = manageHabitView.findViewById(R.id.habit_archive_switch);
        final AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
        final EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);
        if (habitToEdit != null) {
            habitCreateTextInput.setText(habitToEdit.title);
            habitCreateFrequencyInput.setText(Integer.toString(habitToEdit.frequency));
            habitArchiveSwitch.setChecked(habitToEdit.archived);

            habitCreateButton.setOnClickListener(v -> {

                habitToEdit.period = 7 * 24;
                habitToEdit.title = habitCreateTextInput.getText().toString();

                String frequencyString = habitCreateFrequencyInput.getText().toString();
                if (frequencyString.length() > 0) {
                    habitToEdit.frequency = Integer.parseInt(frequencyString);
                }
                if (habitArchiveSwitch.isChecked() && !habitToEdit.archived) {
                    habitToEdit.archived = true;
                    habitList.removeHabit(habitList.getHabitIndex(habitToEdit));
                } else if (habitToEdit.archived){
                    habitToEdit.archived = false;
                    habitList.addHabit(habitToEdit);
                }

                db.habitDao().updateHabit(habitToEdit);
                habitList.notifyHabitUpdated(habitToEdit);
                Toast.makeText(
                        v.getContext(),
                        "Habit details saved!",
                        Toast.LENGTH_SHORT
                ).show();

                // Close the keyboard hackily?
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(habitCreateTextInput.getWindowToken(), 0);
            });

            habitCreateButton.setText(R.string.save_habit);
            habitArchiveSwitch.setVisibility(View.VISIBLE);
            habitExportButton.setVisibility(View.INVISIBLE);
            habitImportButton.setVisibility(View.INVISIBLE);

            Arrays.sort(eventsForHabitToEdit, (e1, e2) -> {
                long r = (e2.timestamp - e1.timestamp);
                // Clamp the long instead of casting it
                if (r < 0) {
                    return -1;
                } else if (r > 0) {
                    return 1;
                }
                return 0;
            });
            events.addAll(eventsForHabitToEdit);
        } else {
            habitArchiveSwitch.setVisibility(View.INVISIBLE);
            habitExportButton.setVisibility(View.VISIBLE);
            habitImportButton.setVisibility(View.VISIBLE);
            habitCreateButton.setText(R.string.create_habit);
        }

        if (firstInitialization) {
            ((ViewGroup) findViewById(R.id.container)).addView(manageHabitView);
        }
    }

    private void hideManageHabits() {
        // Reset the editing stuff when we navigate away
        if (this.habitToEdit != null) {
            // Also update the habit we might have edited because we've maybe changed the set of
            // events
            habitList.notifyHabitUpdated(habitToEdit);
            habitList.sort();
            this.setHabitToEdit(null, null);
        }

        if (manageHabitView != null) {
            AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
            EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);

            // Restore defaults too
            habitCreateTextInput.setText("");
            habitCreateFrequencyInput.setText("");

            manageHabitView.setVisibility(View.INVISIBLE);
        }
    }
}
