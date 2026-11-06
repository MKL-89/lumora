package com.lumora.sms.dto;
import com.lumora.sms.enums.Gender; import java.time.LocalDate;
/** Safe student projection returned by services, rather than a managed entity. */
public record StudentResponse(Long id, String admissionNumber, String firstName, String middleName, String lastName, Gender gender, LocalDate dateOfBirth, boolean active) { }
