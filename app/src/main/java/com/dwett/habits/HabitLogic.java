package com.dwett.habits;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

class HabitLogic {

    static String getDescription(Habit h, Event[] events) {
        int numEvents = numEventsInCurrentPeriod(h, events);
        if (numEvents >= h.frequency) {
            // Done for this period
            return String.format(Locale.ENGLISH, "Done for this %s", getPeriodName(h.period));
        }
        return String.format(
                Locale.ENGLISH,
                "Done %d / %d times so far this %s",
                numEvents,
                h.frequency,
                getPeriodName(h.period));
    }

    static boolean shouldAllowDone(Habit h, Event[] events) {
        return numEventsInCurrentPeriod(h, events) < h.frequency;
    }

    private static int numEventsInCurrentPeriod(Habit h, Event[] events) {
        LocalDateTime start = periodStart(h);
        int num = 0;
        for (Event e : events) {
            LocalDateTime cur = Instant.ofEpochMilli(e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            if (cur.isAfter(start)) {
                num++;
            }
        }
        return num;
    }

    private static LocalDateTime periodStart(Habit h) {
        LocalDateTime cur = LocalDateTime.now();
        if (h.period == 7 * 24) {
            DayOfWeek today = cur.getDayOfWeek();
            while (today != DayOfWeek.MONDAY) {
                cur = cur.minusDays(1);
                today = cur.getDayOfWeek();
            }
            // Move it back to 0
            cur = cur.minusHours(cur.getHour());
            cur = cur.minusMinutes(cur.getMinute());
            return cur.minusSeconds(cur.getSecond());
        }
        if (h.period == 24) {
            // Move it back to 0
            cur = cur.minusHours(cur.getHour());
            cur = cur.minusMinutes(cur.getMinute());
            return cur.minusSeconds(cur.getSecond());
        }
        throw new RuntimeException("Don't know how to compute start of period.");
    }

    private static String getPeriodName(int period) {
        if (period == 7 * 24) {
            return "week";
        } else if (period == 24) {
            return "day";
        }
        return String.format(Locale.ENGLISH, "%d hours", period);
    }
}
