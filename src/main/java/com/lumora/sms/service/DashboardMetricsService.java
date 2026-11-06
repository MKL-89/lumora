package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.AuditLog;
import java.util.List;

/**
 * Read-only aggregation for dashboards. Keeping it behind the service layer means
 * controllers never touch repositories, as the architecture note requires.
 */
public interface DashboardMetricsService {

    DashboardSummary summary();

    List<ChartPoint> enrolmentByClass();

    List<ChartPoint> subjectPerformance();

    List<ChartPoint> feeCollectionByMethod();

    List<ChartPoint> attendanceTrend(int days);

    List<AuditLog> recentActivity();
}
