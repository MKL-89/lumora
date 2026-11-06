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

    /**
     * The name to greet a signed-in person by: the teacher's, learner's or
     * guardian's real name when the account belongs to one, otherwise a tidied
     * form of the username ("deputy" becomes "Deputy").
     */
    String displayName(String username);

    /**
     * Creates an account with a generated temporary password, plus the teacher
     * record (teaching roles) or the link to a learner (learner and guardian roles).
     * Only a super administrator may grant the super administrator role.
     *
     * @return the temporary password, to be shown once and handed to the holder
     */
    String createAccount(@Valid AccountCreateRequest request, boolean actorIsSuperAdmin);

    /**
     * Replaces an account's password with a new temporary one and lifts any
     * lockout. The holder must choose their own at the next sign-in.
     *
     * @return the temporary password, to be shown once
     */
    String resetPassword(@NotNull Long id, boolean actorIsSuperAdmin);

    /** A signed-in person changing their own password; the current one must be given. */
    void changeOwnPassword(String username, String currentPassword, String newPassword, String confirmation);
}
