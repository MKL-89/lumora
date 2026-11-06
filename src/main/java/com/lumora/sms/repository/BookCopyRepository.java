package com.lumora.sms.repository;

import com.lumora.sms.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByAccessionNumber(String accessionNumber);

    List<BookCopy> findByStatusOrderByAccessionNumberAsc(String status);

    List<BookCopy> findByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, String status);

    long countByStatus(String status);
}
