package fr.univ_lille.gitlab.classrooms.jobs;

import fr.univ_lille.gitlab.classrooms.assignments.ArchiveAssignmentUseCase;
import fr.univ_lille.gitlab.classrooms.assignments.Assignment;
import fr.univ_lille.gitlab.classrooms.assignments.AssignmentRepository;
import fr.univ_lille.gitlab.classrooms.assignments.AssignmentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class AutoArchiveJob {

    private final AssignmentRepository assignmentRepository;

    private final ArchiveAssignmentUseCase archiveAssignmentUseCase;

    AutoArchiveJob(AssignmentRepository assignmentRepository, ArchiveAssignmentUseCase archiveAssignmentUseCase) {
        this.assignmentRepository = assignmentRepository;
        this.archiveAssignmentUseCase = archiveAssignmentUseCase;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES, initialDelay = 0)
    void autoArchiveAssignments(){
        this.assignmentRepository.findAll()
                .stream()
                .filter(it -> it.getStatus()== AssignmentStatus.OPENED)
                .filter(Assignment::isAutoArchive)
                .filter(it -> it.getDueDate().isBefore(ZonedDateTime.now()))
                .forEach(this.archiveAssignmentUseCase::archive);
    }
}
