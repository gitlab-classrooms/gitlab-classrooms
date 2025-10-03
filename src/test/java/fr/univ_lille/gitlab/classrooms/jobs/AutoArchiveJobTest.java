package fr.univ_lille.gitlab.classrooms.jobs;

import fr.univ_lille.gitlab.classrooms.assignments.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoArchiveJobTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ArchiveAssignmentUseCase archiveAssignmentUseCase;

    private AutoArchiveJob job;

    @Captor
    private ArgumentCaptor<Assignment> assignmentCaptor;

    @BeforeEach
    void setUp() {
        job = new AutoArchiveJob(assignmentRepository, archiveAssignmentUseCase);
    }

    @Test
    void autoArchiveAssignments_should_archive_only_opened_auto_archived_and_past_due_assignments() {
        var pastDue = new ExerciseAssignment();
        pastDue.setStatus(AssignmentStatus.OPENED);
        pastDue.setAutoArchive(true);
        pastDue.setDueDate(ZonedDateTime.now().minusMinutes(1));

        var futureDue = new ExerciseAssignment();
        futureDue.setStatus(AssignmentStatus.OPENED);
        futureDue.setAutoArchive(true);
        futureDue.setDueDate(ZonedDateTime.now().plusMinutes(10));

        var notAutoArchive = new ExerciseAssignment();
        notAutoArchive.setStatus(AssignmentStatus.OPENED);
        notAutoArchive.setAutoArchive(false);
        notAutoArchive.setDueDate(ZonedDateTime.now().minusMinutes(5));

        var alreadyArchived = new ExerciseAssignment();
        alreadyArchived.setStatus(AssignmentStatus.ARCHIVED);
        alreadyArchived.setAutoArchive(true);
        alreadyArchived.setDueDate(ZonedDateTime.now().minusMinutes(5));

        when(assignmentRepository.findAll()).thenReturn(List.of(pastDue, futureDue, notAutoArchive, alreadyArchived));

        job.autoArchiveAssignments();

        verify(assignmentRepository, times(1)).findAll();
        verify(archiveAssignmentUseCase, times(1)).archive(assignmentCaptor.capture());
        verifyNoMoreInteractions(archiveAssignmentUseCase);

        var archivedAssignment = assignmentCaptor.getValue();
        assertThat(archivedAssignment).isSameAs(pastDue);
    }

}
