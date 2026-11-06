package com.lumora.sms.controller.api;

import com.lumora.sms.dto.*;
import com.lumora.sms.service.DashboardMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Supplies dashboard figures to the browser. It reads through the metrics service
 * rather than repositories, keeping the service layer the only data boundary.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardMetricsService metrics;

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return metrics.summary();
    }

    @GetMapping("/charts")
    public Map<String, List<ChartPoint>> charts() {
        return Map.of(
                "enrolment", metrics.enrolmentByClass(),
                "performance", metrics.subjectPerformance(),
                "collections", metrics.feeCollectionByMethod(),
                "attendance", metrics.attendanceTrend(14));
    }
}
