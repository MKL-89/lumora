package com.lumora.sms.repository;

import com.lumora.sms.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);

    Optional<Teacher> findByUserUsername(String username);

    List<Teacher> findAllByOrderByLastNameAscFirstNameAsc();
}
