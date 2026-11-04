package com.simaoqingnian.myaccount;

import java.time.LocalDate;
import java.util.function.Supplier;

/** Deliberately kept in memory: a new app session starts with today's date. */
public final class EntrySession {
    public static final EntrySession CURRENT = new EntrySession(Ledger::today);
    private final Supplier<LocalDate> clock;
    private LocalDate chosen;

    public EntrySession(Supplier<LocalDate> clock) { this.clock = clock; }

    public synchronized LocalDate date() {
        LocalDate today = clock.get();
        if (chosen != null && chosen.isAfter(today)) chosen = null;
        return chosen == null ? today : chosen;
    }

    public synchronized void select(LocalDate date) {
        if (date == null || date.isBefore(Ledger.MIN_DATE) || date.isAfter(clock.get())) {
            throw new IllegalArgumentException("请选择今天或以前的账目日期");
        }
        chosen = date;
    }

    public synchronized void reset() { chosen = null; }

    public synchronized LocalDate adjacent(LocalDate current, int direction) {
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("日期步长必须为一天");
        if (current == null) throw new IllegalArgumentException("账目日期无效");
        LocalDate next = current.plusDays(direction);
        return next.isBefore(Ledger.MIN_DATE) || next.isAfter(clock.get()) ? null : next;
    }
}
