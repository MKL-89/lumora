package com.lumora.sms.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chart geometry. Getting this wrong produces bars that overflow their panel. */
@DisplayName("ChartSeries geometry")
class ChartSeriesTest {

    private ChartSeries series(Number... values) {
        List<ChartPoint> points = new java.util.ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            points.add(ChartPoint.of("L" + i, values[i]));
        }
        return ChartSeries.of(points);
    }

    @Test
    @DisplayName("an empty series never divides by zero")
    void emptySeriesIsSafe() {
        ChartSeries empty = ChartSeries.of(List.of());

        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.max()).isEqualTo(1d);
        assertThat(empty.linePoints()).isEmpty();
        assertThat(empty.areaPoints()).isEmpty();
    }

    @Test
    @DisplayName("an all-zero series still renders without dividing by zero")
    void allZeroSeriesIsSafe() {
        ChartSeries zeros = series(0, 0, 0);

        assertThat(zeros.max()).isEqualTo(1d);
        assertThat(zeros.barHeight(zeros.points().get(0))).isGreaterThan(0);
    }

    @Test
    @DisplayName("the tallest bar fills the plot area and shorter bars scale against it")
    void barsScaleAgainstTheMaximum() {
        ChartSeries s = series(100, 50, 25);

        assertThat(s.max()).isEqualTo(100d);
        assertThat(s.barHeight(s.points().get(0))).isEqualTo(s.plotHeight());
        assertThat(s.barHeight(s.points().get(1))).isCloseTo(s.plotHeight() / 2, org.assertj.core.data.Offset.offset(2));
    }

    @Test
    @DisplayName("no bar is ever drawn outside the panel")
    void barsStayInsideTheViewBox() {
        ChartSeries s = series(3, 91, 47, 12, 78);

        for (ChartPoint point : s.points()) {
            assertThat(s.barY(point)).isGreaterThanOrEqualTo(0);
            assertThat(s.barY(point) + s.barHeight(point)).isLessThanOrEqualTo(s.height());
        }
        int lastRight = s.barX(s.size() - 1) + s.barWidth();
        assertThat(lastRight).isLessThanOrEqualTo(s.width());
    }

    @Test
    @DisplayName("labels are thinned once a series gets dense, so they cannot overlap")
    void denseSeriesThinsItsLabels() {
        ChartSeries sparse = series(1, 2, 3, 4);
        for (int i = 0; i < sparse.size(); i++) {
            assertThat(sparse.showLabel(i)).isTrue();
        }

        ChartSeries dense = series(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
        long shown = java.util.stream.IntStream.range(0, dense.size())
                .filter(dense::showLabel)
                .count();
        assertThat(shown).isLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("the trend polyline has one coordinate pair per point")
    void linePointsMatchTheSeries() {
        ChartSeries s = series(10, 20, 30);

        assertThat(s.linePoints().split(" ")).hasSize(3);
        // The filled area closes along the baseline, so it has two extra coordinates.
        assertThat(s.areaPoints().split(" ")).hasSize(5);
    }
}
