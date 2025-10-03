package fr.univ_lille.gitlab.classrooms.classrooms;

import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.transaction.Transactional;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;

    private final Gitlab gitlab;

    ClassroomServiceImpl(ClassroomRepository classroomRepository, Gitlab gitlab) {
        this.classroomRepository = classroomRepository;
        this.gitlab = gitlab;
    }

    @Override
    public List<ClassroomEntity> getAllClassrooms() {
        return this.classroomRepository.findAll().stream()
                .filter(classroom -> !classroom.isArchived())
                .toList();
    }

    @Override
    public List<ClassroomEntity> getAllJoinedClassrooms(ClassroomUser student) {
        return this.classroomRepository.findClassroomByStudentsContains(student).stream()
                .filter(classroom -> !classroom.isArchived())
                .toList();
    }

    @Override
    public Optional<ClassroomEntity> getClassroom(UUID uuid) {
        return this.classroomRepository.findById(uuid);
    }

    @Transactional
    @Override
    public void joinClassroom(ClassroomEntity classroom, ClassroomUser student) {
        classroom.join(student);

        this.classroomRepository.save(classroom);
    }

    @Transactional
    @Override
    public void createClassroom(String classroomName, Long parentGitlabGroupId, ClassroomUser teacher) throws GitLabApiException {
        var classroom = new ClassroomEntity();
        classroom.setName(classroomName);
        classroom.addTeacher(teacher);

        this.gitlab.createGroup(classroom, Optional.ofNullable(parentGitlabGroupId));

        this.classroomRepository.save(classroom);
    }

    @Override
    public void saveClassroom(ClassroomEntity classroom) {
        this.classroomRepository.save(classroom);
    }

    @Transactional
    @Override
    public void archiveClassroom(ClassroomEntity classroom) {
        classroom.setArchived(true);
        this.classroomRepository.save(classroom);
    }

    @Transactional
    @Override
    public void unarchiveClassroom(ClassroomEntity classroom) {
        classroom.setArchived(false);
        this.classroomRepository.save(classroom);
    }
}
