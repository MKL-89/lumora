package com.lumora.sms.security;

import com.lumora.sms.entity.Teacher;
import com.lumora.sms.service.AcademicService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** A teacher can never record marks or attendance under a colleague's name. */
@DisplayName("Acting teacher")
class ActingTeacherTest {

    private final AcademicService academics = mock(AcademicService.class);
    private final ActingTeacher acting = new ActingTeacher(academics);
    private Teacher mercy;

    @BeforeEach
    void setUp() {
        mercy = new Teacher();
        mercy.setId(3L);
        when(academics.teacherForUser("mercy.chebet")).thenReturn(Optional.of(mercy));
        when(academics.teacherForUser("orphan")).thenReturn(Optional.empty());
        Teacher other = new Teacher();
        other.setId(9L);
        when(academics.teachers()).thenReturn(List.of(mercy, other));
    }

    @Test
    @DisplayName("a teacher naming a colleague is still recorded as themselves")
    void teacherForcedToSelf() {
        Authentication teacher = auth("mercy.chebet", "ROLE_TEACHER");

        assertThat(acting.resolve(teacher, 9L)).isEqualTo(3L);
        assertThat(acting.resolve(teacher, null)).isEqualTo(3L);
        assertThat(acting.choices(teacher)).containsExactly(mercy);
    }

    @Test
    @DisplayName("the principal may record on any teacher's behalf")
    void leadershipMayChoose() {
        Authentication principal = auth("principal", "ROLE_PRINCIPAL");

        assertThat(acting.resolve(principal, 9L)).isEqualTo(9L);
        assertThat(acting.choices(principal)).hasSize(2);
    }

    @Test
    @DisplayName("a teacher account with no teacher record is refused")
    void unlinkedTeacherRefused() {
        assertThatThrownBy(() -> acting.resolve(auth("orphan", "ROLE_CLASS_TEACHER"), 9L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static Authentication auth(String user, String role) {
        return new UsernamePasswordAuthenticationToken(user, "n/a", AuthorityUtils.createAuthorityList(role));
    }
}
