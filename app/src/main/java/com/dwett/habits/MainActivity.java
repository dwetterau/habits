package com.dwett.habits;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {

    private HabitList habitList;
    private RecyclerView habitListRecyclerView;
    private RecyclerView summaryRecyclerView;
    private HabitDatabase db;

    private View manageHabitView;

    private Habit habitToEdit;
    private Event[] eventsForHabitToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(
                getApplicationContext(),
                HabitDatabase.class,
                "habits"
        )
                // TODO(davidw): Remove this!
                .allowMainThreadQueries()
                .build();

        habitList = new HabitList(db.habitDao().loadAllHabits(), db, new habitEditor(this));
        habitList.sort();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return inflateBasedOffMenuItem(item.getItemId());
            }
        });
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

    private void inflateHabitList() {
        boolean firstInitialization = habitListRecyclerView == null;
        if (firstInitialization) {
            habitListRecyclerView = findViewById(R.id.habit_list_recycler_view);
            // TODO Why?
            habitListRecyclerView.setHasFixedSize(true);
            RecyclerView.LayoutManager habitListRecyclerViewLayoutManager = new LinearLayoutManager(this);
            habitListRecyclerView.setLayoutManager(habitListRecyclerViewLayoutManager);
            RecyclerView.Adapter habitListRecyclerViewAdapter = habitList;
            habitListRecyclerView.setAdapter(habitListRecyclerViewAdapter);
        }
        habitList.sort();
        habitListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideHabitList() {
        habitListRecyclerView.setVisibility(View.INVISIBLE);
    }

    private void inflateSummaryView() {
        summaryRecyclerView = findViewById(R.id.summary_recycler_view);
        RecyclerView.LayoutManager summaryRecyclerViewLayoutManager = new LinearLayoutManager(this);
        summaryRecyclerView.setLayoutManager(summaryRecyclerViewLayoutManager);
        RecyclerView.Adapter summaryViewAdapter = new Summary(this.db);
        summaryRecyclerView.setAdapter(summaryViewAdapter);
        summaryRecyclerView.setVisibility(View.VISIBLE);
        summaryRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                summaryRecyclerView.scrollToPosition(0);
            }
        });
    }

    private void hideSummaryView() {
        if (summaryRecyclerView != null) {
            summaryRecyclerView.setVisibility(View.INVISIBLE);
        }
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

        final Button habitDeleteButton = manageHabitView.findViewById(R.id.habit_delete_button);
        Button habitCreateButton = manageHabitView.findViewById(R.id.habit_create_button);
        habitCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Habit h = new Habit();
                AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
                EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);
                RadioGroup habitCreatePeriodRadioGroup = manageHabitView.findViewById(R.id.habit_period_radio_group);

                if (habitCreatePeriodRadioGroup.getCheckedRadioButtonId() == R.id.daily_radio_button) {
                    h.period = 24;
                } else if (habitCreatePeriodRadioGroup.getCheckedRadioButtonId() == R.id.weekly_radio_button) {
                    h.period = 7 * 24;
                } else {
                    // Default to weekly
                    h.period = 7 * 24;
                }

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

                // TODO: Navigate to the habits page?
            }
        });

        RecyclerView eventListRecyclerView = manageHabitView.findViewById(R.id.event_list_recycler_view);
        RecyclerView.LayoutManager eventListRecyclerViewLayoutManager = new LinearLayoutManager(
                manageHabitView.getContext()
        );
        eventListRecyclerView.setLayoutManager(eventListRecyclerViewLayoutManager);
        eventListRecyclerView.setAdapter(new EventList(new Event[]{}, db, getFragmentManager()));

        if (habitToEdit != null) {
            // TODO Populate other fields to edit too!
            AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
            EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);
            RadioGroup habitCreatePeriodRadioGroup = manageHabitView.findViewById(R.id.habit_period_radio_group);

            habitCreateTextInput.setText(habitToEdit.title);
            habitCreateFrequencyInput.setText(Integer.toString(habitToEdit.frequency));
            if (habitToEdit.period == 24) {
                habitCreatePeriodRadioGroup.check(R.id.daily_radio_button);
            } else if (habitToEdit.period == 7 * 24) {
                habitCreatePeriodRadioGroup.check(R.id.weekly_radio_button);
            }

            habitCreateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AutoCompleteTextView habitCreateTextInput = manageHabitView.findViewById(R.id.habit_title_input);
                    EditText habitCreateFrequencyInput = manageHabitView.findViewById(R.id.habit_frequency_input);
                    RadioGroup habitCreatePeriodRadioGroup = manageHabitView.findViewById(R.id.habit_period_radio_group);

                    if (habitCreatePeriodRadioGroup.getCheckedRadioButtonId() == R.id.daily_radio_button) {
                        habitToEdit.period = 24;
                    } else if (habitCreatePeriodRadioGroup.getCheckedRadioButtonId() == R.id.weekly_radio_button) {
                        habitToEdit.period = 7 * 24;
                    }

                    habitToEdit.title = habitCreateTextInput.getText().toString();

                    String frequencyString = habitCreateFrequencyInput.getText().toString();
                    if (frequencyString.length() > 0) {
                        habitToEdit.frequency = Integer.parseInt(frequencyString);
                    }
                    db.habitDao().updateHabit(habitToEdit);
                    habitList.notifyHabitUpdated(habitToEdit);

                    // Close the keyboard hackily?
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(habitCreateTextInput.getWindowToken(), 0);
                }
            });
            habitDeleteButton.setVisibility(View.VISIBLE);
            final AlertDialog.Builder deleteConfirmer = new AlertDialog.Builder(manageHabitView.getContext())
                    .setTitle("Confirm habit deletion")
                    .setMessage("Do you really want to delete this habit?")
                    .setNegativeButton(android.R.string.no, null);

            habitDeleteButton.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    deleteConfirmer.setPositiveButton(
                            android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    db.habitDao().deleteHabit(habitToEdit);
                                    habitList.removeHabit(habitList.getHabitIndex(habitToEdit));
                                    habitToEdit = null;
                                    eventsForHabitToEdit = null;
                                }
                            }
                    ).show();
                }
            });

            Arrays.sort(eventsForHabitToEdit, new Comparator<Event>() {
                @Override
                public int compare(Event e1, Event e2) {
                    return (int) (e2.timestamp - e1.timestamp);
                }
            });
            eventListRecyclerView.setAdapter(new EventList(eventsForHabitToEdit, db, getFragmentManager()));
        } else {
            habitDeleteButton.setVisibility(View.INVISIBLE);
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
            RadioGroup habitCreatePeriodRadioGroup = manageHabitView.findViewById(R.id.habit_period_radio_group);

            // Restore defaults too
            habitCreateTextInput.setText("");
            habitCreateFrequencyInput.setText("");
            habitCreatePeriodRadioGroup.check(R.id.weekly_radio_button);

            manageHabitView.setVisibility(View.INVISIBLE);
        }
    }
}
