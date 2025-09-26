package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ExportAssignmentResultUseCaseTest {

    @Test
    @DisplayName("getExportData should map student fields, scores and retakes for quiz assignments")
    void getExportData_mapsFieldsAndRetakes() {
        // Arrange
        var assignmentService = Mockito.mock(AssignmentService.class);
        var useCase = new ExportAssignmentResultUseCase(assignmentService);

        UUID assignmentId = UUID.randomUUID();
        var assignment = Mockito.mock(Assignment.class);
        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getName()).thenReturn("My Quiz");
        when(assignment.getType()).thenReturn(AssignmentType.QUIZ);

        when(assignmentService.getAssignment(assignmentId)).thenReturn(Optional.of(assignment));

        var student = new ClassroomUser();
        student.setName("Alice");
        student.setEmail("alice@example.com");

        var sqa = new StudentQuizAssignment();
        sqa.setStudent(student);
        sqa.setScore(8L);
        sqa.setMaxScore(10L);
        sqa.setRetakes(2);

        when(assignmentService.getAssignmentResults(assignment)).thenReturn(List.of(sqa));

        // Act
        var data = useCase.getExportData(assignmentId);

        // Assert
        assertThat(data.assignmentId()).isEqualTo(assignmentId);
        assertThat(data.assignmentName()).isEqualTo("My Quiz");
        assertThat(data.assignmentType()).isEqualTo(AssignmentType.QUIZ);
        assertThat(data.rows()).hasSize(1);

        var row = data.rows().getFirst();
        assertThat(row.studentEmail()).isEqualTo("alice@example.com");
        assertThat(row.studentName()).isEqualTo("Alice");
        assertThat(row.assignmentScore()).isEqualTo(8L);
        assertThat(row.maxScore()).isEqualTo(10L);
        assertThat(row.retakes()).isEqualTo(2);
        assertThat(row.submissionDate()).isNull();
    }

    @Test
    @DisplayName("getExportData should set retakes to null for non-quiz assignments")
    void getExportData_setsRetakesNullForExercise() {
        // Arrange
        var assignmentService = Mockito.mock(AssignmentService.class);
        var useCase = new ExportAssignmentResultUseCase(assignmentService);

        UUID assignmentId = UUID.randomUUID();
        var assignment = Mockito.mock(Assignment.class);
        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getName()).thenReturn("Exercise 1");
        when(assignment.getType()).thenReturn(AssignmentType.EXERCISE);

        when(assignmentService.getAssignment(assignmentId)).thenReturn(Optional.of(assignment));

        var student = new ClassroomUser();
        student.setName("Bob, the \"Builder\"");
        student.setEmail("bob@example.com");

        var sea = new StudentExerciseAssignment();
        sea.setStudent(student);
        // no grades -> score and maxScore computed to 0

        when(assignmentService.getAssignmentResults(assignment)).thenReturn(List.of(sea));

        // Act
        var data = useCase.getExportData(assignmentId);

        // Assert
        assertThat(data.assignmentType()).isEqualTo(AssignmentType.EXERCISE);
        assertThat(data.rows()).hasSize(1);

        var row = data.rows().getFirst();
        assertThat(row.studentEmail()).isEqualTo("bob@example.com");
        assertThat(row.studentName()).isEqualTo("Bob, the \"Builder\"");
        assertThat(row.assignmentScore()).isZero();
        assertThat(row.maxScore()).isZero();
        assertThat(row.retakes()).isNull();
    }
}
