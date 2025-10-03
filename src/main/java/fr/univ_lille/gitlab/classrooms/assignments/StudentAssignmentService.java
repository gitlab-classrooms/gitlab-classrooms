package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentAssignmentService {

    /**
     * Gets an assignment from its Gitlab Project id
     *
     * @param gitlabProjectId
     * @return
     */
    Optional<StudentExerciseAssignment> getByGitlabProjectId(long gitlabProjectId);

    List<StudentExerciseAssignment> getAllStudentExerciseAssignmentsForAClassroom(ClassroomEntity classroom, ClassroomUser student);

    void resetGrades(ClassroomUser student, UUID studentAssignmentId);

    void save(StudentExerciseAssignment exerciseAssignment);
}
