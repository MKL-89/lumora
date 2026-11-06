package com.lumora.sms.dto;

import java.math.BigDecimal;

/** One labelled value in a chart series. */
public record ChartPoint(String label, BigDecimal value) {

    public static ChartPoint of(String label, Number value) {
        return new ChartPoint(label, value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()));
    }

    /** Rounded value, which is all the SVG chart renderer needs. */
    public long rounded() {
        return value.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }
}
