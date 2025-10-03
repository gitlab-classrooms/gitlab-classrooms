package fr.univ_lille.gitlab.classrooms.export;

import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.StudentExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomRole;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @InjectMocks
    private ExportServiceImpl exportService;

    @Mock
    private StudentAssignmentService assignmentService;

    @Test
    void listStudentRepositories_ForAMultipleStudentsAndExercises() {
        var classroom = new Classroom();
        classroom.setName("Test Classroom");
        classroom.setId(UUID.randomUUID());

        var luke = new ClassroomUser("Luke", List.of(ClassroomRole.STUDENT));
        classroom.join(luke);

        var leia = new ClassroomUser("Leia", List.of(ClassroomRole.STUDENT));
        classroom.join(leia);

        var lukeExercise1 = new StudentExerciseAssignment();
        var lukeExercise2 = new StudentExerciseAssignment();
        var leiaExercise1 = new StudentExerciseAssignment();
        var leiaExercise2 = new StudentExerciseAssignment();

        when(assignmentService.getAllStudentExerciseAssignmentsForAClassroom(classroom, luke)).thenReturn(List.of(lukeExercise1, lukeExercise2));
        when(assignmentService.getAllStudentExerciseAssignmentsForAClassroom(classroom, leia)).thenReturn(List.of(leiaExercise1, leiaExercise2));

        lukeExercise1.setGitlabCloneUrl("git+ssh://luke-fake-url-exercise-1.git");
        lukeExercise2.setGitlabCloneUrl("git+ssh://luke-fake-url-exercise-2.git");
        leiaExercise1.setGitlabCloneUrl("git+ssh://leia-fake-url-exercise-1.git");
        leiaExercise2.setGitlabCloneUrl("git+ssh://leia-fake-url-exercise-2.git");

        var studentRepositories = exportService.listStudentRepositories(classroom);

        assertThat(studentRepositories)
                .hasSize(2)
                .satisfies(it -> {
                    assertThat(it.getFirst().studentName()).isEqualTo("Leia");
                    assertThat(it.getFirst().cloneUrls()).containsExactly("git+ssh://leia-fake-url-exercise-1.git", "git+ssh://leia-fake-url-exercise-2.git");

                    assertThat(it.getLast().studentName()).isEqualTo("Luke");
                    assertThat(it.getLast().cloneUrls()).containsExactly("git+ssh://luke-fake-url-exercise-1.git", "git+ssh://luke-fake-url-exercise-2.git");
                });
    }
}
