package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomRole;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceImplTest {

    @InjectMocks
    private StudentAssignmentServiceImpl studentAssignmentService;

    @Mock
    private StudentExerciceAssignmentRepository studentExerciceAssignmentRepository;


    @Mock
    private StudentAssignmentRepository studentAssignmentRepository;

    @Test
    void getByGitlabProjectId() {
        this.studentAssignmentService.getByGitlabProjectId(12);

        verify(this.studentExerciceAssignmentRepository).findByGitlabProjectId(12L);
    }

    @Test
    void getAllByClassroomAndStudent() {
        var classroom = new Classroom();
        var student = new ClassroomUser();

        this.studentAssignmentService.getAllStudentExerciseAssignmentsForAClassroom(classroom, student);

        verify(this.studentExerciceAssignmentRepository).findByAssignmentClassroomIdAndStudent(classroom.getId(), student);
    }

    @Test
    void save() {
        var exercise = new StudentExerciseAssignment();
        this.studentAssignmentService.save(exercise);

        verify(this.studentExerciceAssignmentRepository).save(exercise);
    }

    @Test
    void resetGrades_doesNothing_ifNoStudentAssignment_exists(){
        var student = new ClassroomUser("luke", List.of(ClassroomRole.STUDENT));
        var assignmentId = UUID.randomUUID();

        when(this.studentAssignmentRepository.findByAssignmentIdAndStudent(assignmentId, student)).thenReturn(Optional.empty());

        this.studentAssignmentService.resetGrades(student, assignmentId);

        verify(studentAssignmentRepository).findByAssignmentIdAndStudent(assignmentId, student);
        verifyNoMoreInteractions(studentAssignmentRepository);
    }

    @Test
    void resetGrades(){
        var student = new ClassroomUser("luke", List.of(ClassroomRole.STUDENT));
        var assignmentId = UUID.randomUUID();

        var studentAssignment = mock(StudentAssignment.class);

        when(this.studentAssignmentRepository.findByAssignmentIdAndStudent(assignmentId, student)).thenReturn(Optional.of(studentAssignment));

        this.studentAssignmentService.resetGrades(student, assignmentId);

        verify(studentAssignmentRepository).findByAssignmentIdAndStudent(assignmentId, student);
        verify(studentAssignment).resetGrades();
        verify(studentAssignmentRepository).save(studentAssignment);
    }
}
