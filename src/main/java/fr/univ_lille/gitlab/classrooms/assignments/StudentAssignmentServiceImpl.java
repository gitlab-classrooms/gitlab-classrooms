package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class StudentAssignmentServiceImpl implements StudentAssignmentService {

    private final StudentExerciceAssignmentRepository studentExerciceAssignmentRepository;

    private final StudentAssignmentRepository studentAssignmentRepository;

    StudentAssignmentServiceImpl(StudentExerciceAssignmentRepository studentExerciceAssignmentRepository, StudentAssignmentRepository studentAssignmentRepository) {
        this.studentExerciceAssignmentRepository = studentExerciceAssignmentRepository;
        this.studentAssignmentRepository = studentAssignmentRepository;
    }

    @Override
    public Optional<StudentExerciseAssignment> getByGitlabProjectId(long gitlabProjectId) {
        return this.studentExerciceAssignmentRepository.findByGitlabProjectId(gitlabProjectId);
    }

    @Override
    public List<StudentExerciseAssignment> getAllStudentExerciseAssignmentsForAClassroom(Classroom classroom, ClassroomUser student) {
        return this.studentExerciceAssignmentRepository.findByAssignmentClassroomIdAndStudent(classroom.getId(), student);
    }

    @Override
    @Transactional
    public void resetGrades(ClassroomUser student, UUID assignmentId) {
        var studentAssignment= this.studentAssignmentRepository.findByAssignmentIdAndStudent(assignmentId, student);

        studentAssignment.ifPresent(it -> {
            it.resetGrades();
            this.studentAssignmentRepository.save(it);
        });
    }

    @Override
    public void save(StudentExerciseAssignment exerciseAssignment) {
        this.studentExerciceAssignmentRepository.save(exerciseAssignment);
    }
}
