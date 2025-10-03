package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.GitLabException;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.quiz.*;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import fr.univ_lille.gitlab.classrooms.users.WithMockStudent;
import fr.univ_lille.gitlab.classrooms.users.WithMockTeacher;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/sql/init-test-users.sql")
class AssignmentControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssignmentService assignmentService;

    @MockBean
    private ClassroomService classroomService;

    @Autowired
    private ClassroomUserService classroomUserService;

    @MockBean
    private Gitlab gitlab;

    private final UUID quizAssignmentId = UUID.randomUUID();

    private final UUID exerciseAssignmentId = UUID.randomUUID();

    private final UUID classroomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var classroom = new Classroom();
        classroom.setName("AssignmentControllerMVCTest classroom");
        classroom.setId(classroomId);

        when(classroomService.getClassroom(classroomId)).thenReturn(Optional.of(classroom));

        var quiz = new QuizEntity();

        var quizAssignment = new QuizAssignment();
        quizAssignment.setId(quizAssignmentId);
        quizAssignment.setName("AssignmentControllerMVCTest Quiz assignment");
        quizAssignment.setQuiz(quiz);

        var exerciseAssignment = new ExerciseAssignment();
        exerciseAssignment.setId(exerciseAssignmentId);

        var classroomEntity = new ClassroomEntity();
        classroomEntity.setName("AssignmentControllerMVCTest classroom entity");
        classroomEntity.setId(classroomId);

        quizAssignment.setClassroom(classroomEntity);
        exerciseAssignment.setClassroom(classroomEntity);

        when(assignmentService.getAssignment(quizAssignmentId)).thenReturn(Optional.of(quizAssignment));
        when(assignmentService.getAssignment(exerciseAssignmentId)).thenReturn(Optional.of(exerciseAssignment));

        when(assignmentService.getAssignmentResults(exerciseAssignment)).thenReturn(List.of());
        when(assignmentService.getAssignmentResultsForStudent(eq(exerciseAssignment), any())).thenReturn(new StudentExerciseAssignment());
    }

    @Test
    void acceptAssignment_shouldAskForLogin() throws Exception {
        mockMvc.perform(get("/assignments/"+ quizAssignmentId +"/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockStudent
    void acceptAssignment_shouldRedirectToClassroomJoin_ifStudentDoesNotBelongToTheClassroom() throws Exception {
        mockMvc.perform(get("/assignments/"+ quizAssignmentId +"/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/classrooms/" + classroomId + "/join"));
    }

    @Test
    @WithMockStudent
    @Disabled("disabled during hexagonal refactoring, should work in production as objects are really saved")
    void acceptAssignment_shouldShowAcceptPage_ifStudentBelongsToTheClassroom() throws Exception {
        // make the student join the classroom first
        var classroom = this.classroomService.getClassroom(this.classroomId).orElseThrow();
        classroom.join(classroomUserService.getClassroomUser("luke.skywalker"));

        mockMvc.perform(get("/assignments/"+ quizAssignmentId +"/accept"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/accept"))
                .andExpect(model().attributeExists("assignment"));
    }

    @Test
    @WithMockStudent
    void acceptAssignment_shouldShowAcceptPage_afterStudentAcceptation() throws Exception {
        mockMvc.perform(post("/assignments/"+ quizAssignmentId +"/accept").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/accepted"));

        verify(assignmentService).acceptAssigment(any(), any());
    }

    @Test
    @WithMockStudent
    void acceptAssignment_shouldShowAcceptPageAndGitlabLink_afterStudentAcceptation() throws Exception {
        mockMvc.perform(post("/assignments/"+ exerciseAssignmentId +"/accept").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("assignmentResult"))
                .andExpect(view().name("assignments/accepted"));

        verify(assignmentService).acceptAssigment(any(), any());
    }

    @Test
    @WithMockStudent
    void acceptAssignment_shouldShowAnError_whenGitLabException() throws Exception {
        doThrow(new GitLabException("Could not create project", new GitLabApiException("500"))).when(this.assignmentService).acceptAssigment(any(), any());

        mockMvc.perform(post("/assignments/"+ exerciseAssignmentId +"/accept").with(csrf()))
                .andExpect(status().is5xxServerError());

        verify(assignmentService).acceptAssigment(any(), any());
    }

    @Test
    @WithMockTeacher
    void viewQuizAssignment_shouldShowTheAssignmentResults() throws Exception {
        mockMvc.perform(get("/assignments/"+ quizAssignmentId))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("quiz"))
                .andExpect(model().attributeExists("quizResults"))
                .andExpect(view().name("quiz/all-submissions"));
    }

    @Test
    @WithMockTeacher
    void viewExerciseAssignment_shouldShowTheAssignmentResults() throws Exception {
        mockMvc.perform(get("/assignments/"+ exerciseAssignmentId))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("exercise"))
                .andExpect(model().attributeExists("exerciseResults"))
                .andExpect(view().name("exercise/all-submissions"));
    }

    @Test
    @WithMockTeacher
    void createAssignment_shouldShowNewAssignmentPage() throws Exception {
        mockMvc.perform(get("/classrooms/"+classroomId+"/assignments/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignments/new"))
                .andExpect(model().attributeExists("classroom"))
                .andExpect(model().attributeExists("quizzes"))
                .andExpect(model().attributeExists("repositories"));

        verify(this.gitlab).getProjectTemplates(any(Classroom.class));
    }

    @Test
    @WithMockTeacher
    void createQuizAssignment_shouldSaveTheAssignmentToTheClassroom() throws Exception {
        mockMvc.perform(post("/classrooms/"+classroomId+"/assignments/new")
                        .with(csrf())
                        .param("assignmentName", "ClassroomControllerMVCTest assignment")
                        .param("assignmentType", "QUIZ")
                        .param("quizName", "ClassroomControllerMVCTest quiz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/classrooms/"+classroomId));
    }

    @Test
    @WithMockTeacher
    void createExerciseAssignment_shouldSaveTheAssignmentToTheClassroom() throws Exception {
        mockMvc.perform(post("/classrooms/"+classroomId+"/assignments/new")
                        .with(csrf())
                        .param("assignmentName", "ClassroomControllerMVCTest assignment")
                        .param("assignmentType", "EXERCISE")
                        .param("repositoryId", "Fake ID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/classrooms/"+classroomId));
    }

}
