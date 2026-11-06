package com.lumora.sms.repository;

import com.lumora.sms.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    Optional<BookCategory> findBySchoolIdAndName(Long schoolId, String name);

    List<BookCategory> findAllByOrderByNameAsc();
}
