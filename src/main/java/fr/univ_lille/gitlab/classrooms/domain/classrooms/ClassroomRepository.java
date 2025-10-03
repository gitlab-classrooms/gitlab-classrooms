package fr.univ_lille.gitlab.classrooms.domain.classrooms;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository {

    Collection<Classroom> findAll();

    List<Classroom> findClassroomByStudentsContains(ClassroomUser student);

    Optional<Classroom> findById(UUID uuid);

    Classroom save(Classroom classroom);
}
