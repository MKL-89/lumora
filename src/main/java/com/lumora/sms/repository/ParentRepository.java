package com.lumora.sms.repository;

import com.lumora.sms.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByUserUsername(String username);

    List<Parent> findAllByOrderByLastNameAscFirstNameAsc();
}
