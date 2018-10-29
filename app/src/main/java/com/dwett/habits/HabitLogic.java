package com.dwett.habits;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

class HabitLogic {

    static Event[] loadEventsInCurrentPeriod(HabitDao hd, Habit h) {
        long start = periodStart(h).atZone(ZoneId.systemDefault()).toEpochSecond();
        return hd.loadEventsForHabitSince(h.id, start);
    }

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

    static boolean onTrack(Habit h, Event[] events, int percentBehindAllowed) {
        LocalDateTime cur = LocalDateTime.now();
        cur.minusSeconds(cur.getSecond());
        cur.minusMinutes(cur.getMinute());
        cur.minusHours(cur.getHour());

        LocalDateTime start = periodStart(h);
        long between = ChronoUnit.HOURS.between(start, cur);
        int diff = (int) Math.round(((double)between / (double)h.period) * 100);
        return diff - currentProgress(h, events) > percentBehindAllowed;
    }

    static boolean isDone(Habit h, Event[] events) {
        return numEventsInCurrentPeriod(h, events) >= h.frequency;
    }

    /**
     * @param h the habit
     * @param events all relevant events for the habit
     * @return a value in [0, 100] for the percent done the habit is in this period
     */
    static int currentProgress(Habit h, Event[] events) {
        int n = numEventsInCurrentPeriod(h, events);
        int val = Math.round(((float)n / (float)h.frequency) * 100);
        if (val > 100) {
            val = 100;
        }
        return val;
    }

    private static int numEventsInCurrentPeriod(Habit h, Event[] events) {
        LocalDateTime start = periodStart(h);
        int num = 0;
        for (Event e : events) {
            LocalDateTime cur = Instant.ofEpochMilli(e.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            // Note: what we really want here is cur >= start
            if (!cur.isBefore(start)) {
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
