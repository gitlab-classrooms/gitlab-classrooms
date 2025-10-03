package fr.univ_lille.gitlab.classrooms.domain.classrooms;

import fr.univ_lille.gitlab.classrooms.assignments.AssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignment;
import fr.univ_lille.gitlab.classrooms.assignments.StudentExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomStudentControllerTest {

    @InjectMocks
    private ClassroomStudentController classroomStudentController;

    @Mock
    private ClassroomService classroomService;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private ClassroomUserService classroomUserService;

    @Test
    void showClassroomStudent() {
        var classroom = new Classroom();
        var classroomId = UUID.randomUUID();
        when(this.classroomService.getClassroom(classroomId)).thenReturn(Optional.of(classroom));

        var student = new ClassroomUser();
        var studentId = "luke.skywalker";
        when(this.classroomUserService.getClassroomUser(studentId)).thenReturn(student);

        List<StudentAssignment> assignments = List.of(new StudentExerciseAssignment());
        when(this.assignmentService.getAllStudentAssignmentsForAClassroom(classroom, student)).thenReturn(assignments);

        var model = this.classroomStudentController.showClassroomStudent(classroomId, studentId);

        assertThat(model).isNotNull();
        assertThat(model.getViewName()).isEqualTo("classrooms/student");
        assertThat(model.getModel()).containsKeys("classroom", "student", "assignments");
    }
}
