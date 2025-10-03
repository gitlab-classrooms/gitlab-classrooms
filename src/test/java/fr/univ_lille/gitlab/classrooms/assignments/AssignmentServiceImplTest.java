package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.GitLabException;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.quiz.QuizEntity;
import fr.univ_lille.gitlab.classrooms.quiz.QuizService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ClassroomService classroomService;

    @Mock
    private QuizService quizService;

    @Mock
    private Gitlab gitlab;

    @Mock
    private StudentAssignmentRepository studentAssignmentRepository;

    @Mock
    private StudentExerciceAssignmentRepository studentExerciceAssignmentRepository;

    @Test
    void getAssignment_shouldReturnTheAssignment() {
        var uuid = UUID.randomUUID();

        this.assignmentService.getAssignment(uuid);

        verify(this.assignmentRepository).findById(uuid);
    }

    @Test
    void acceptAssignment_shouldAssociateTheStudentWithTheAssignment_andSave() throws GitLabApiException, GitLabException {
        var assignment = new QuizAssignment();

        var student = new ClassroomUser();

        this.assignmentService.acceptAssigment(assignment, student);

        assertThat(assignment.getStudents()).contains(student);

        verify(assignmentRepository).save(assignment);
    }

    @Test
    void acceptAssignment_shouldDoNothing_ifQuizAssignmentIsAlreadyAccepted() throws GitLabApiException, GitLabException {
        var assignment = new QuizAssignment();

        var student = new ClassroomUser();

        when(studentAssignmentRepository.existsByAssignmentAndStudent(assignment,  student)).thenReturn(true);

        this.assignmentService.acceptAssigment(assignment, student);

        verifyNoMoreInteractions(studentAssignmentRepository);
    }

    @Test
    void acceptExerciceAssignment_shouldCreateAGitlabProject() throws GitLabApiException, GitLabException {
        var teacher = new ClassroomUser();

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");
        assignment.setGitlabGroupId(12L);
        classroom.addAssignment(assignment);

        var student = new ClassroomUser();
        student.setName("luke.skywalker");

        var project = new Project();
        project.setId(25L);
        project.setWebUrl("https://gitlab.univ-lille.fr/gitlab-classroom");
        project.setSshUrlToRepo("git@gitlab.univ-lille.fr:/gitlab-classroom");
        when(gitlab.createStudentProject(assignment, student)).thenReturn(project);

        this.assignmentService.acceptAssigment(assignment, student);

        verify(gitlab).createStudentProject(assignment, student);

        var studentExerciseCaptor = ArgumentCaptor.forClass(StudentExerciseAssignment.class);
        verify(studentExerciceAssignmentRepository).save(studentExerciseCaptor.capture());

        var studentExercise = studentExerciseCaptor.getValue();
        assertThat(studentExercise).isNotNull();
        assertThat(studentExercise.getAssignment()).isEqualTo(assignment);
        assertThat(studentExercise.getStudent()).isEqualTo(student);
        assertThat(studentExercise.getGitlabProjectId()).isEqualTo(25L);
        assertThat(studentExercise.getGitlabProjectUrl()).isEqualTo("https://gitlab.univ-lille.fr/gitlab-classroom");
        assertThat(studentExercise.getGitlabCloneUrl()).isEqualTo("git@gitlab.univ-lille.fr:/gitlab-classroom");
    }

    @Test
    void acceptAssignment_shouldUpdateGitlabInfo_ifExerciseAssignmentIsAlreadyAccepted() throws GitLabApiException, GitLabException {
        var teacher = new ClassroomUser();

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");
        assignment.setGitlabGroupId(12L);
        classroom.addAssignment(assignment);

        var student = new ClassroomUser();
        student.setName("luke.skywalker");

        var studentExerciseAssignment = new StudentExerciseAssignment();

        var project = new Project();
        project.setId(65L);
        project.setWebUrl("web_url");
        project.setSshUrlToRepo("clone_url");
        when(gitlab.createStudentProject(assignment, student)).thenReturn(project);

        when(studentExerciceAssignmentRepository.findByAssignmentAndStudent(assignment,  student)).thenReturn(Optional.of(studentExerciseAssignment));

        this.assignmentService.acceptAssigment(assignment, student);

        assertThat(studentExerciseAssignment.getGitlabProjectId()).isEqualTo(65L);
        assertThat(studentExerciseAssignment.getGitlabProjectUrl()).isEqualTo("web_url");
        assertThat(studentExerciseAssignment.getGitlabCloneUrl()).isEqualTo("clone_url");

        verify(studentExerciceAssignmentRepository).save(studentExerciseAssignment);
        verifyNoMoreInteractions(studentAssignmentRepository);
    }

    @Test
    void createQuizAssignment_shouldAssociateTheAssignmentWithTheQuiz_andSaveTheClassroom(){
        var quiz = new QuizEntity();
        when(quizService.getQuiz("Test Quiz")).thenReturn(Optional.of(quiz));

        var classroom = new Classroom();
        var assignment = this.assignmentService.createQuizAssignment(classroom, "Test Quiz Assignment", "Test Quiz");

        assertThat(assignment)
                .isNotNull()
                .isInstanceOf(QuizAssignment.class);
        assertThat(assignment.getName()).isEqualTo("Test Quiz Assignment");

        var quizAssignment = ((QuizAssignment) assignment);
        assertThat(quizAssignment.getQuiz()).isEqualTo(quiz);

        assertThat(classroom.getAssignments()).contains(assignment);

        verify(assignmentRepository).save(assignment);
        verify(classroomService).saveClassroom(classroom);
    }

    @Test
    void createExerciseAssignment_shouldCreateAGitlabGroup_andSaveTheClassroom() throws GitLabApiException {
        var classroom = new Classroom();
        classroom.setGitlabGroupId(9L);
        var assignment = this.assignmentService.createExerciseAssignment(classroom, "Test Exercise Assignment", null);

        assertThat(assignment)
                .isNotNull()
                .isInstanceOf(ExerciseAssignment.class);
        assertThat(assignment.getName()).isEqualTo("Test Exercise Assignment");

        assertThat(classroom.getAssignments()).contains(assignment);

        verify(gitlab).createGroup((ExerciseAssignment) assignment, classroom);

        verify(assignmentRepository).save(assignment);
        verify(classroomService).saveClassroom(classroom);
    }

    @Test
    void getAssignmentResults_shouldReturnTheStudentAssignmentResults() {
        var assignment = new QuizAssignment();

        this.assignmentService.getAssignmentResults(assignment);

        verify(this.studentAssignmentRepository).findAllByAssignment(assignment);
    }

    @Test
    void getAssignmentResultsForStudent_shouldReturnTheStudentAssignmentResults() {
        var assignment = new QuizAssignment();
        var student = new ClassroomUser();

        this.assignmentService.getAssignmentResultsForStudent(assignment, student);

        verify(this.studentAssignmentRepository).findByAssignmentAndStudent(assignment, student);
    }

    @Test
    void getAssignmentResultsForAClassroom_shouldReturnTheStudentAssignmentResults() {
        var classroom = new Classroom();
        var student = new ClassroomUser();

        this.assignmentService.getAllStudentAssignmentsForAClassroom(classroom, student);

        verify(this.studentAssignmentRepository).findByAssignmentClassroomIdAndStudent(classroom.getId(), student);
    }

}
