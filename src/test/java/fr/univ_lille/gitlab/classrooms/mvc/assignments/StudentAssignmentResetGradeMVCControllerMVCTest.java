package fr.univ_lille.gitlab.classrooms.mvc.assignments;

import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignmentService;
import fr.univ_lille.gitlab.classrooms.users.WithMockStudent;
import fr.univ_lille.gitlab.classrooms.users.WithMockTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/sql/init-test-users.sql")
class StudentAssignmentResetGradeMVCControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentAssignmentService studentAssignmentService;

    private final UUID quizAssignmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
    }

    @Test
    @WithMockStudent(username = "leia.organa")
    void testResetGrade_asWrongStudent_shouldBeForbidden() throws Exception {
        mockMvc.perform(post("/assignments/{assignmentId}/students/{studentId}/reset", quizAssignmentId.toString(), "luke.skywalker")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockStudent
    void testResetGrade_asSelf() throws Exception {
        mockMvc.perform(post("/assignments/{assignmentId}/students/{studentId}/reset", quizAssignmentId.toString(), "luke.skywalker")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(studentAssignmentService).resetGrades(argThat(it -> it.getName().equals("luke.skywalker")), eq(quizAssignmentId));
    }

    @Test
    @WithMockTeacher
    void testResetGrade_asTeacher() throws Exception {
        mockMvc.perform(post("/assignments/{assignmentId}/students/{studentId}/reset", quizAssignmentId.toString(), "luke.skywalker")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(studentAssignmentService).resetGrades(argThat(it -> it.getName().equals("luke.skywalker")), eq(quizAssignmentId));
    }

    @Test
    @WithMockTeacher
    void testResetGrade_asTeacher_shouldRedirectToRefererPage() throws Exception {
        mockMvc.perform(post("/assignments/{assignmentId}/students/{studentId}/reset", quizAssignmentId.toString(), "luke.skywalker")
                        .header("Referer", "http://localhost:8080/classrooms/death-star")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:http://localhost:8080/classrooms/death-star"));
    }

}
