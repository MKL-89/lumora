package com.lumora.sms.mapper;
import com.lumora.sms.dto.StudentResponse; import com.lumora.sms.entity.Student; import org.springframework.stereotype.Component;
/** Translates the student entity into the stable service response model. */
@Component public class StudentMapper { public StudentResponse toResponse(Student s) { return new StudentResponse(s.getId(),s.getAdmissionNumber(),s.getFirstName(),s.getMiddleName(),s.getLastName(),s.getGender(),s.getDateOfBirth(),s.isActive()); } }
