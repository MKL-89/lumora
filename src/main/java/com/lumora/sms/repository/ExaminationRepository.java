package com.lumora.sms.repository;

import com.lumora.sms.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {

    List<Examination> findAllByOrderByExaminationDateDesc();

    List<Examination> findByClassStreamIdOrderByExaminationDateDesc(Long classStreamId);

    long countByPublishedFalse();
}
