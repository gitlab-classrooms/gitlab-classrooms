package fr.univ_lille.gitlab.classrooms.classrooms;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository {

    Collection<ClassroomEntity> findAll();

    List<ClassroomEntity> findClassroomByStudentsContains(ClassroomUser student);

    Optional<ClassroomEntity> findById(UUID uuid);

    ClassroomEntity save(ClassroomEntity classroom);
}
