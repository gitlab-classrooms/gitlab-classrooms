package fr.univ_lille.gitlab.classrooms.adapters.jpa;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomRepository;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter that implements the domain ClassroomRepository by delegating to the Spring Data JPA repository.
 */
@Component
class ClassroomAdapter implements ClassroomRepository {

    private final ClassroomJPARepository jpaRepository;

    private final ClassroomEntityMapper classroomEntityMapper;

    ClassroomAdapter(ClassroomJPARepository jpaRepository, ClassroomEntityMapper classroomEntityMapper) {
        this.jpaRepository = jpaRepository;
        this.classroomEntityMapper = classroomEntityMapper;
    }

    @Override
    public Collection<Classroom> findAll() {
        return jpaRepository.findAll()
                .stream().map(classroomEntityMapper::toClassroom)
                .toList();
    }

    @Override
    public List<Classroom> findClassroomByStudentsContains(ClassroomUser student) {
        return jpaRepository.findClassroomByStudentsContains(student)
                .stream().map(classroomEntityMapper::toClassroom)
                .toList();
    }

    @Override
    public Optional<Classroom> findById(UUID uuid) {
        return jpaRepository.findById(uuid)
                .map(classroomEntityMapper::toClassroom);
    }

    @Override
    public Classroom save(Classroom classroom) {
        var entity = classroomEntityMapper.toEntity(classroom);
        jpaRepository.save(entity);
        return classroomEntityMapper.toClassroom(entity);
    }
}
