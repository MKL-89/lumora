package com.lumora.sms.repository;

import com.lumora.sms.entity.ClassStream;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassStreamRepository extends JpaRepository<ClassStream, Long> {

    List<ClassStream> findBySchoolClassId(Long schoolClassId);

    List<ClassStream> findAllByOrderBySchoolClassLevelAscNameAsc();
}
