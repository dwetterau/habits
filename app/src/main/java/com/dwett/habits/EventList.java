package com.dwett.habits;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;

public class EventList extends RecyclerView.Adapter<EventList.EventHolder> {

    private LinkedList<Event> events;
    private HabitDatabase db;
    private AlertDialog.Builder deleteEventConfirmerBuilder;

    public EventList(Event[] events, HabitDatabase db) {
        this.events = new LinkedList<>();
        this.events.addAll(Arrays.asList(events));
        this.db = db;
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

        holder.eventTimestamp.setText(
                Instant.ofEpochMilli(thisEvent.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .toString()
        );

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Event thisEvent = thisList.events.get(holder.getAdapterPosition());

                deleteEventConfirmerBuilder.setPositiveButton(
                        android.R.string.yes,
                        new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        db.habitDao().deleteEvent(thisEvent);
                        thisList.removeEvent(holder.getAdapterPosition());
                    }}
                ).show();
            }
        });

        // TODO: Listeners for editing the date / time of the event
    }

    public void addEvent(Event e) {
        this.events.add(e);
        this.notifyItemInserted(this.getItemCount() - 1);
    }

    public void removeEvent(int index) {
        this.events.remove(index);
        this.notifyItemRangeRemoved(index, 1);
        this.notifyItemRangeChanged(index, this.getItemCount());
    }

    public void notifyEventUpdated(int index) {
        this.notifyItemChanged(index);
    }

    @Override
    public int getItemCount() {
        return this.events.size();
    }

    static class EventHolder extends RecyclerView.ViewHolder {

        private TextView eventTimestamp;
        private Button deleteButton;

        EventHolder(View itemView) {
            super(itemView);
            eventTimestamp = itemView.findViewById(R.id.event_timestamp);
            deleteButton = itemView.findViewById(R.id.event_delete_button);
        }

        // TODO: Add listeners?
    }
}
