package com.dwett.habits;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {

    private HabitList habitList;
    private RecyclerView habitListRecyclerView;
    private HabitDatabase db;

    private View manageHabitView;

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

        habitList = new HabitList(db.habitDao().loadAllHabits(), db);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return inflateBasedOffMenuItem(item.getItemId());
            }
        });
        this.inflateBasedOffMenuItem(navigation.getSelectedItemId());
    }

    /*
     * Returns if a known type was selected
     */
    private boolean inflateBasedOffMenuItem(int item) {
        switch (item) {
            case R.id.navigation_summary:
                this.hideManageHabits();
                // TODO build a summary view and inflate it here
                this.inflateHabitList();
                break;
            case R.id.navigation_habits:
                this.hideManageHabits();
                // Inflate the habit list view
                this.inflateHabitList();
                break;
            case R.id.navigation_manage:
                this.hideHabitList();
                // Inflate the view to create habits
                this.inflateManageHabits();
                break;
            default:
                return false;
        }
        return true;
    }

    private void inflateHabitList() {
        habitListRecyclerView = findViewById(R.id.habit_list_recycler_view);
        // TODO Why?
        habitListRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager habitListRecyclerViewLayoutManager = new LinearLayoutManager(this);
        habitListRecyclerView.setLayoutManager(habitListRecyclerViewLayoutManager);
        RecyclerView.Adapter habitListRecyclerViewAdapter = habitList;
        habitListRecyclerView.setAdapter(habitListRecyclerViewAdapter);
        habitListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideHabitList() {
        habitListRecyclerView.setVisibility(View.INVISIBLE);
    }

    private void inflateManageHabits() {
        manageHabitView = getLayoutInflater().inflate(
                R.layout.create_habit,
                null);

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
        ((ViewGroup) findViewById(R.id.container)).addView(manageHabitView);
    }

    private void hideManageHabits() {
        if (manageHabitView != null) {
            manageHabitView.setVisibility(View.INVISIBLE);
        }
    }
}
