package com.lumora.sms.web;

import com.lumora.sms.dto.ChartPoint;
import com.lumora.sms.dto.ChartSeries;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * View helper exposed to Thymeleaf as {@code @chartRenderer}. It turns a plain
 * list of points into drawable geometry and formats the figures that appear
 * beside them.
 */
@Component("chartRenderer")
public class ChartRenderer {

    public ChartSeries series(List<ChartPoint> points) {
        return ChartSeries.of(points);
    }

    /** Kenyan shillings with thousands separators and no decimal noise. */
    public String money(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return String.format("%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    /** Compact form for stat cards: 1,250,000 becomes "1.3M". */
    public String compact(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000d);
        }
        if (value >= 1_000) {
            return String.format("%.0fK", value / 1_000d);
        }
        return String.valueOf(value);
    }

    /** Maps a grade to the CSS class that colours it. */
    public String gradeClass(String grade) {
        if (grade == null || grade.isBlank()) {
            return "";
        }
        return "g-" + Character.toLowerCase(grade.charAt(0));
    }

    /** Bar colour band for a percentage meter. */
    public String meterClass(int percentage) {
        if (percentage >= 75) {
            return "good";
        }
        return percentage >= 45 ? "warn" : "bad";
    }
}
