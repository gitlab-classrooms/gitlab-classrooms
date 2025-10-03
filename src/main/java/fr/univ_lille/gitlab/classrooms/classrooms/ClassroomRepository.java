package fr.univ_lille.gitlab.classrooms.classrooms;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface ClassroomRepository extends JpaRepository<ClassroomEntity, UUID> {

    List<ClassroomEntity> findClassroomByStudentsContains(ClassroomUser student);

}
