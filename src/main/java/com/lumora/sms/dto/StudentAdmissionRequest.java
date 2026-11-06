package com.lumora.sms.dto;
import com.lumora.sms.enums.Gender; import jakarta.validation.constraints.*; import java.time.LocalDate;
/** Input for a student admission; a stream is mandatory by business policy. */
public record StudentAdmissionRequest(@NotNull Long schoolId, @NotBlank @Size(max=30) String admissionNumber, @NotBlank String firstName, String middleName, @NotBlank String lastName, @NotNull Gender gender, @NotNull @Past LocalDate dateOfBirth, @NotNull LocalDate admissionDate, @NotNull Long academicYearId, @NotNull Long classStreamId) { }
