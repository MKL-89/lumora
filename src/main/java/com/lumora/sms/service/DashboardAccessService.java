package com.lumora.sms.service;

import com.lumora.sms.entity.Student;
import java.util.List;

/** Enforces ownership restrictions used by the Student and Parent/Guardian dashboards. */
public interface DashboardAccessService {

    void assertStudentOwnsRecord(String username, Long studentId);

    void assertParentOwnsStudent(String username, Long studentId);

    /** The learners explicitly linked to a guardian account. */
    List<Student> childrenOf(String parentUsername);
}
