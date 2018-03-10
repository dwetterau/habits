package com.dwett.habits;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private RecyclerView habitListRecyclerView;
    private RecyclerView.Adapter habitListRecyclerViewAdapter;
    private RecyclerView.LayoutManager habitListRecyclerViewLayoutManager;

    private AutoCompleteTextView habitCreateTextInput;
    private Button habitCreateButton;
    private HabitDatabase db;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int currentPage = 0;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    currentPage = R.string.title_updates;
                    break;
                case R.id.navigation_dashboard:
                    currentPage = R.string.title_habits;
                    break;
                case R.id.navigation_notifications:
                    currentPage = R.string.title_notifications;
                    break;
            }
            if (currentPage > 0) {
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        db = Room.databaseBuilder(
                getApplicationContext(),
                HabitDatabase.class,
                "habits"
        )
                // TODO(davidw): Remove this!
                .allowMainThreadQueries()
                .build();


        habitListRecyclerView = findViewById(R.id.habit_list_recycler_view);
        // TODO Why?
        habitListRecyclerView.setHasFixedSize(true);
        habitListRecyclerViewLayoutManager = new LinearLayoutManager(this);
        habitListRecyclerView.setLayoutManager(habitListRecyclerViewLayoutManager);
        habitListRecyclerViewAdapter = new HabitList(db.habitDao().loadAllHabits());
        habitListRecyclerView.setAdapter(habitListRecyclerViewAdapter);


        habitCreateButton = findViewById(R.id.habit_create_button);
        habitCreateTextInput = findViewById(R.id.habit_title_input);
        habitCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Habit h = new Habit();
                h.title = habitCreateTextInput.getText().toString();
                db.habitDao().insertNewHabit(h);
            }
        });
    }

}
