package fr.univ_lille.gitlab.classrooms.classrooms;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApiException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomService {

    List<Classroom> getAllClassrooms();

    /**
     * Returns all the classrooms the student has joined
     * @param student the student
     * @return a list of classrooms
     */
    List<Classroom> getAllJoinedClassrooms(ClassroomUser student);

    Optional<Classroom> getClassroom(UUID uuid);

    void joinClassroom(Classroom classroom, ClassroomUser student);

    void createClassroom(String classroomName, Long parentGitlabGroupId, ClassroomUser teacher) throws GitLabApiException;

    void saveClassroom(Classroom classroom);

    /**
     * Archives a classroom
     * @param classroom the classroom to archive
     */
    void archiveClassroom(Classroom classroom);

    /**
     * Unarchives a classroom
     * @param classroom the classroom to unarchive
     */
    void unarchiveClassroom(Classroom classroom);
}
