package com.lumora.sms.repository;

import com.lumora.sms.entity.Role;
import com.lumora.sms.enums.UserRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(UserRoleType name);
}
