package com.lumora.sms.service;

import com.lumora.sms.dto.SetupRequest;
import com.lumora.sms.entity.User;

/**
 * First-time setup. On a brand-new, empty database nobody can sign in, so the
 * first visitor describes the school and chooses the administrator's username and
 * password. No account or password is built into the code. Once any account
 * exists, setup is closed for good.
 */
public interface SetupService {

    /** True only while the database has no accounts at all. */
    boolean needed();

    /** Creates the school, its calendar, forms, streams, subjects, grading scale and administrator. */
    User run(SetupRequest request);
}
