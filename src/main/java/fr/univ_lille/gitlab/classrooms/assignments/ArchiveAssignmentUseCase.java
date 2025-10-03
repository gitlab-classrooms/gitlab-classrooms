package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.gitlab.GitLabException;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ArchiveAssignmentUseCase {

    private final AssignmentRepository assignmentRepository;

    private final StudentAssignmentRepository studentAssignmentRepository;

    private final Gitlab gitlab;

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveAssignmentUseCase.class.getName());

    ArchiveAssignmentUseCase(AssignmentRepository assignmentRepository, StudentAssignmentRepository studentAssignmentRepository, Gitlab gitlab) {
        this.assignmentRepository = assignmentRepository;
        this.studentAssignmentRepository = studentAssignmentRepository;
        this.gitlab = gitlab;
    }

    public void archive(Assignment assignment){
        LOGGER.info("Archiving assignment {}", assignment.getId());
        var studentAssignments = this.studentAssignmentRepository.findAllByAssignment(assignment);
        studentAssignments.stream()
                .map(it -> (StudentExerciseAssignment)it)
                .forEach(studentExerciseAssignment -> {
                    try {
                        this.gitlab.archiveProject(studentExerciseAssignment);
                    } catch (GitLabException e) {
                        LOGGER.error("Unable to archive GitLab project {} for student {}", studentExerciseAssignment.getGitlabProjectId(), studentExerciseAssignment.getStudent().getName());
                    }
                });

        assignment.setStatus(AssignmentStatus.ARCHIVED);
        assignmentRepository.save(assignment);
    }

}
