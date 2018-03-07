package com.dwett.habits;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private CardView cardView;
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
                cardView.removeAllViews();
                LayoutInflater inflater = getLayoutInflater();
                for (Habit h : db.habitDao().loadAllHabits()) {
                    View child = getLayoutInflater().inflate(R.layout.habit_card, cardView, true);
                    TextView habitTitle = child.findViewById(R.id.habit_title);
                    habitTitle.setText(h.title);
                    TextView habitDescription = child.findViewById(R.id.habit_description);
                    habitDescription.setText(currentPage);
                }
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardView = findViewById(R.id.cardview);
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
