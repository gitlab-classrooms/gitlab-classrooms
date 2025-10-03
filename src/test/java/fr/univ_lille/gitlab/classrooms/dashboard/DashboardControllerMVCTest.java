package fr.univ_lille.gitlab.classrooms.dashboard;

import fr.univ_lille.gitlab.classrooms.assignments.AssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.QuizAssignment;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.users.WithMockStudent;
import fr.univ_lille.gitlab.classrooms.users.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/sql/init-test-users.sql")
class DashboardControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassroomService classroomService;

    @MockBean
    private AssignmentService assignmentService;

    @Nested
    @WithMockTeacher
    class TeacherRole {

        @Test
        void shouldAccessTeacherDashboard() throws Exception {
            mockMvc.perform(get("/"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/teacher-dashboard"));

            verify(classroomService).getAllClassrooms();
        }
    }

    @Nested
    @WithMockStudent
    class StudentRole {

        @Test
        void shouldAccessStudentDashboard() throws Exception {
            mockMvc.perform(get("/"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/student-dashboard"));

            verify(classroomService).getAllJoinedClassrooms(any());
        }

        @Test
        void shouldListJoinedClassroomAndAssignmentResults() throws Exception {
            var classroom = new Classroom();
            var assignment = new QuizAssignment();
            classroom.addAssignment(assignment);

            when(classroomService.getAllJoinedClassrooms(any())).thenReturn(List.of(classroom));

            mockMvc.perform(get("/"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/student-dashboard"));

            verify(classroomService).getAllJoinedClassrooms(any());
            verify(assignmentService).getAllStudentAssignmentsForAClassroom(eq(classroom), any());
        }
    }
}
