package com.dwett.habits;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;

public class EventList extends RecyclerView.Adapter<EventList.EventHolder> {

    private LinkedList<Event> events;
    private HabitDatabase db;
    private AlertDialog.Builder deleteEventConfirmerBuilder;
    private FragmentManager fm;

    public EventList(Event[] events, HabitDatabase db, FragmentManager fm) {
        this.events = new LinkedList<>();
        this.events.addAll(Arrays.asList(events));
        this.db = db;
        this.fm = fm;
    }

    @Override
    public EventHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list, parent, false);

        deleteEventConfirmerBuilder = new AlertDialog.Builder(parent.getContext())
                .setTitle("Confirm event deletion")
                .setMessage("Do you really want to delete this event?")
                .setNegativeButton(android.R.string.no, null);

        return new EventHolder(view);
    }

    @Override
    public void onBindViewHolder(final EventHolder holder, int position) {
        final EventList thisList = this;
        final Event thisEvent = this.events.get(position);

        final LocalDateTime time = Instant.ofEpochMilli(thisEvent.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        holder.eventDate.setText(time.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        holder.eventDate.setOnClickListener(v -> {
            // First show a date picker to allow the date to be adjusted
            EventDatePicker datePicker = new EventDatePicker();
            datePicker.setEvent(thisEvent);
            datePicker.setEventList(thisList);
            datePicker.show(thisList.fm, "datePicker");
        });

        holder.eventTime.setText(time.format(DateTimeFormatter.ofPattern("@ hh:mma")));
        holder.eventTime.setOnClickListener(v -> {
            // First show a time picker to allow the time to be adjusted
            EventTimePicker timePicker = new EventTimePicker();
            timePicker.setEvent(thisEvent);
            timePicker.setEventList(thisList);
            timePicker.show(thisList.fm, "timePicker");
        });

        holder.deleteButton.setOnClickListener(v -> {
            deleteEventConfirmerBuilder.setPositiveButton(
                    android.R.string.yes,
                    (dialog, whichButton) -> {
                        db.habitDao().deleteEvent(thisEvent);
                        thisList.removeEvent(holder.getAdapterPosition());
                    }
            ).show();
        });
    }

    public void addAll(Event[] events) {
        this.events.addAll(Arrays.asList(events));
        this.notifyDataSetChanged();
    }

    public void removeEvent(int index) {
        this.events.remove(index);
        this.notifyItemRangeRemoved(index, 1);
        this.notifyItemRangeChanged(index, this.getItemCount());
    }

    public int getEventIndex(Event e) {
        int i = 0;
        for (Event cur : this.events) {
            if (cur.id == e.id) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Unexpected event passed to getEventIndex " + e.id);
    }

    public void notifyEventUpdated(Event e) {
        this.notifyItemChanged(this.getEventIndex(e));
    }

    @Override
    public int getItemCount() {
        return this.events.size();
    }

    static class EventHolder extends RecyclerView.ViewHolder {

        private TextView eventDate;
        private TextView eventTime;
        private Button deleteButton;

        EventHolder(View itemView) {
            super(itemView);
            eventDate = itemView.findViewById(R.id.event_date);
            eventTime = itemView.findViewById(R.id.event_time);
            deleteButton = itemView.findViewById(R.id.event_delete_button);
        }

        // TODO: Add listeners?
    }

    public static class EventDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        Event e;
        EventList list;

        public void setEvent(Event e) {
            this.e = e;
        }

        public void setEventList(EventList list) {
            this.list = list;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker

            LocalDateTime dt = Instant.ofEpochMilli(this.e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(
                    getActivity(),
                    this,
                    dt.getYear(),
                    dt.getMonthValue() - 1,
                    dt.getDayOfMonth());
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            LocalDateTime dt = Instant.ofEpochMilli(this.e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LocalDateTime newDateTime = LocalDateTime.of(
                    year,
                    month + 1,
                    dayOfMonth,
                    dt.getHour(),
                    dt.getMinute());

            e.timestamp = newDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            list.db.habitDao().updateEvent(e);
            list.notifyEventUpdated(e);
        }
    }

    public static class EventTimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        Event e;
        EventList list;

        public void setEvent(Event e) {
            this.e = e;
        }

        public void setEventList(EventList list) {
            this.list = list;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker

            LocalDateTime dt = Instant.ofEpochMilli(this.e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Create a new instance of DatePickerDialog and return it
            return new TimePickerDialog(
                    getActivity(),
                    this,
                    dt.getHour(),
                    dt.getMinute(),
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hour, int minute) {

            LocalDateTime dt = Instant.ofEpochMilli(this.e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LocalDateTime newDateTime = LocalDateTime.of(
                    dt.getYear(),
                    dt.getMonthValue(),
                    dt.getDayOfMonth(),
                    hour,
                    minute);

            e.timestamp = newDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
            list.db.habitDao().updateEvent(e);
            Toast.makeText(getActivity(), "Event updated!", Toast.LENGTH_SHORT).show();
            list.notifyEventUpdated(e);
        }
    }
}
