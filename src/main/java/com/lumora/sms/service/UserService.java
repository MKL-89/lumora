package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Manages identities and their granted roles. */
public interface UserService {

    /** Creates a unique, securely hashed user account. */
    User create(@Valid UserCreateRequest request);

    /** Reads a user or fails with a domain exception. */
    User get(@NotNull Long id);

    /** Disables, without deleting, a user account. */
    void disable(@NotNull Long id);

    void enable(@NotNull Long id);

    List<User> list();

    /** Accounts allowed to borrow from the library: learners, guardians, and staff. */
    List<User> borrowers();

    List<Parent> parents();

    List<RoleUsage> roleUsage();
}
