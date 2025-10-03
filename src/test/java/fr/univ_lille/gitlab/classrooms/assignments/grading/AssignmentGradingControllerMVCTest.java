package fr.univ_lille.gitlab.classrooms.assignments.grading;

import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.StudentExerciseAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/sql/init-test-users.sql")
class AssignmentGradingControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentAssignmentService studentAssignmentService;

    @MockBean
    private AssignmentGradeService assignmentGradeService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void submitJunit_shouldReturn403_whenNoAuth() throws Exception {
        mockMvc.perform(post("/api/assignments/submit/junit"))
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(403));
    }

    @Test
    void submitJunit_shouldReturn404_whenNoProjectIdCanBeFound() throws Exception {
        var reportFile = new ClassPathResource("/junit-reports/TEST-test.dao.CatalogDaoTest.xml");
        var mockMultipartFile = new MockMultipartFile("file", reportFile.getInputStream());

        mockMvc.perform(
                    multipart("/api/assignments/submit/junit")
                        .file(mockMultipartFile)
                        .with(jwt())
                )
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(404));
    }

    @Test
    void submitJunit_shouldReturn404_whenNoProjectCanBeFound() throws Exception {
        var reportFile = new ClassPathResource("/junit-reports/TEST-test.dao.CatalogDaoTest.xml");
        var mockMultipartFile = new MockMultipartFile("file", reportFile.getInputStream());

        when(studentAssignmentService.getByGitlabProjectId(12)).thenReturn(Optional.empty());

        mockMvc.perform(
                        multipart("/api/assignments/submit/junit")
                                .file(mockMultipartFile)
                                .with(jwt().jwt(builder -> builder.claim("project_id", 12).claim("user_login", "luke.skywalker")))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(404));
    }

    @Test
    void submitJunit_shouldReturn200_whenProjectIsSuccessfullyGraded() throws Exception {
        var reportInputStream = new ClassPathResource("/junit-reports/TEST-test.dao.CatalogDaoTest.xml").getInputStream();
        var mockMultipartFile = new MockMultipartFile("file", reportInputStream);

        var studentExerciseAssignment = new StudentExerciseAssignment();

        when(studentAssignmentService.getByGitlabProjectId(12)).thenReturn(Optional.of(studentExerciseAssignment));

        mockMvc.perform(
                        multipart("/api/assignments/submit/junit")
                                .file(mockMultipartFile)
                                .with(jwt().jwt(builder -> builder.claim("project_id", 12).claim("user_login", "luke.skywalker")))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200));

        verify(assignmentGradeService).gradeAssignmentWithJUnitReport(eq(studentExerciseAssignment), any());
        verify(studentAssignmentService).save(studentExerciseAssignment);
    }

}
