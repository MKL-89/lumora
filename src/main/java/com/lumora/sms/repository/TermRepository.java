package com.lumora.sms.repository;

import com.lumora.sms.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findFirstByActiveTrue();

    Optional<Term> findByAcademicYearIdAndTermNumber(Long academicYearId, Integer termNumber);

    List<Term> findAllByOrderByAcademicYearNameAscTermNumberAsc();
}
