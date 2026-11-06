package com.lumora.sms.service.impl;

import com.lumora.sms.entity.Student;
import com.lumora.sms.entity.StudentGuardian;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.DashboardAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Shared authorization checks for ownership-scoped dashboard operations.
 *
 * <p>URL role rules alone are never trusted for record-level access: a learner and
 * a guardian both reach {@code /students/{id}}, so ownership is proven here. A
 * failure raises {@link AccessDeniedException} rather than a business exception,
 * so the request is reported as 403 Forbidden and written to the audit trail.
 */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAccessServiceImpl implements DashboardAccessService {

    private final StudentRepository students;
    private final StudentGuardianRepository guardians;

    @Override
    public void assertStudentOwnsRecord(String username, Long studentId) {
        if (!students.existsByIdAndUserUsername(studentId, username)) {
            throw new AccessDeniedException("Students can only view their own records.");
        }
    }

    @Override
    public void assertParentOwnsStudent(String username, Long studentId) {
        if (!guardians.existsByStudentIdAndParentUserUsername(studentId, username)) {
            throw new AccessDeniedException(
                    "Parents can only access their own children's information.");
        }
    }

    @Override
    public List<Student> childrenOf(String parentUsername) {
        return guardians.findByParentUserUsername(parentUsername).stream()
                .map(StudentGuardian::getStudent)
                .toList();
    }
}
