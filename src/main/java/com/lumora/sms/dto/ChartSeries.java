package com.lumora.sms.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A chart series together with the geometry a template needs to draw it.
 *
 * <p>The arithmetic lives here rather than in Thymeleaf so the views stay
 * declarative and the scaling can be reasoned about (and tested) in Java. All
 * charts are drawn as inline SVG, which keeps the strict Content-Security-Policy
 * intact: no charting library is downloaded or executed.
 */
public record ChartSeries(List<ChartPoint> points, int width, int height) {

    private static final int LABEL_BAND = 22;
    private static final int VALUE_BAND = 14;

    public static ChartSeries of(List<ChartPoint> points) {
        return new ChartSeries(points == null ? List.of() : points, 480, 170);
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public int size() {
        return points.size();
    }

    /** The largest value, never zero, so scaling can always divide by it. */
    public double max() {
        double max = points.stream()
                .map(ChartPoint::value)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(0d);
        return max <= 0 ? 1d : max;
    }

    /** Vertical space available to the bars or line, above the label band. */
    public int plotHeight() {
        return height - LABEL_BAND - VALUE_BAND;
    }

    public int slotWidth() {
        return size() == 0 ? width : width / size();
    }

    public int barWidth() {
        return Math.max(6, (int) (slotWidth() * 0.56));
    }

    public int barX(int index) {
        return (index * slotWidth()) + ((slotWidth() - barWidth()) / 2);
    }

    public int barHeight(ChartPoint point) {
        double value = point.value() == null ? 0 : point.value().doubleValue();
        return Math.max(2, (int) Math.round(value / max() * plotHeight()));
    }

    public int barY(ChartPoint point) {
        return VALUE_BAND + plotHeight() - barHeight(point);
    }

    /** Baseline the bars sit on and the axis rule is drawn along. */
    public int baseline() {
        return VALUE_BAND + plotHeight();
    }

    public int labelY() {
        return height - 7;
    }

    public int centreX(int index) {
        return (index * slotWidth()) + (slotWidth() / 2);
    }

    public int valueY(ChartPoint point) {
        return Math.max(10, barY(point) - 4);
    }

    /** Three evenly spaced horizontal gridlines. */
    public List<Integer> gridlines() {
        int plot = plotHeight();
        return List.of(VALUE_BAND, VALUE_BAND + plot / 3, VALUE_BAND + (plot * 2 / 3));
    }

    /** Polyline coordinates for the trend chart, e.g. "12,80 46,64 80,71". */
    public String linePoints() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(centreX(i)).append(',').append(barY(points.get(i)));
        }
        return sb.toString();
    }

    /** The same path closed along the baseline, so it can be filled. */
    public String areaPoints() {
        if (points.isEmpty()) {
            return "";
        }
        return centreX(0) + "," + baseline() + " " + linePoints()
                + " " + centreX(points.size() - 1) + "," + baseline();
    }

    /** Labels are thinned on dense series so they never overlap. */
    public boolean showLabel(int index) {
        int every = size() <= 8 ? 1 : (size() + 7) / 8;
        return index % every == 0;
    }
}
