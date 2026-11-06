package com.lumora.sms.dto;

/** A learner's attendance tally across every register they appear in. */
public record AttendanceSummaryView(long present, long absent, long late, long excused) {

    public long total() {
        return present + absent + late + excused;
    }

    /** Present-and-late counted as attended, expressed as a whole percentage. */
    public int rate() {
        long total = total();
        return total == 0 ? 0 : (int) Math.round((present + late) * 100.0 / total);
    }
}
