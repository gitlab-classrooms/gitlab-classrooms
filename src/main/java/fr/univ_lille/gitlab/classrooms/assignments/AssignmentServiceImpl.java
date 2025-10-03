package fr.univ_lille.gitlab.classrooms.assignments;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.GitLabException;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.quiz.QuizService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.transaction.Transactional;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class AssignmentServiceImpl implements AssignmentService {

    private final QuizService quizService;

    private final Gitlab gitlab;

    private final ClassroomService classroomService;

    private final AssignmentRepository assignmentRepository;

    private final StudentAssignmentRepository studentAssignmentRepository;

    private final StudentExerciceAssignmentRepository studentExerciceAssignmentRepository;

    AssignmentServiceImpl(QuizService quizService, Gitlab gitlab, ClassroomService classroomService, AssignmentRepository assignmentRepository, StudentAssignmentRepository studentAssignmentRepository, StudentExerciceAssignmentRepository studentExerciceAssignmentRepository) {
        this.quizService = quizService;
        this.gitlab = gitlab;
        this.classroomService = classroomService;
        this.assignmentRepository = assignmentRepository;
        this.studentAssignmentRepository = studentAssignmentRepository;
        this.studentExerciceAssignmentRepository = studentExerciceAssignmentRepository;
    }

    @Override
    public Optional<Assignment> getAssignment(UUID id) {
        return this.assignmentRepository.findById(id);
    }

    @Override
    public void archive(Assignment assignment){
        var studentAssignments = this.studentAssignmentRepository.findAllByAssignment(assignment);
        studentAssignments.stream()
                .map(it -> (StudentExerciseAssignment)it)
                .forEach(studentExerciseAssignment -> {
                    try {
                        this.gitlab.archiveProject(studentExerciseAssignment);
                    } catch (GitLabException e) {
                        System.out.println(e.getMessage());
                    }
                });
    }

    @Override
    @Transactional
    public void acceptAssigment(Assignment assignment, ClassroomUser student) throws GitLabApiException, GitLabException {
        assignment.accept(student);
        this.assignmentRepository.save(assignment);

        if (assignment instanceof ExerciseAssignment exerciseAssignment) {
            // create the project in gitlab
            var project = gitlab.createStudentProject(exerciseAssignment, student);
            // save in database, and update if necessary
            var studentExercise = this.studentExerciceAssignmentRepository.findByAssignmentAndStudent(exerciseAssignment, student).orElse(new StudentExerciseAssignment());
            studentExercise.setAssignment(exerciseAssignment);
            studentExercise.setStudent(student);
            studentExercise.setGitlabProjectId(project.getId());
            studentExercise.setGitlabProjectUrl(project.getWebUrl());
            studentExercise.setGitlabCloneUrl(project.getSshUrlToRepo());
            this.studentExerciceAssignmentRepository.save(studentExercise);
        } else if (assignment instanceof QuizAssignment quizAssignment) {
            if (this.studentAssignmentRepository.existsByAssignmentAndStudent(quizAssignment, student)) {
                return;
            }
            // create the student quiz assignment
            var studentExercise = new StudentQuizAssignment();
            studentExercise.setAssignment(quizAssignment);
            studentExercise.setStudent(student);
            this.studentAssignmentRepository.save(studentExercise);
        }
    }

    @Override
    public List<StudentAssignment> getAssignmentResults(Assignment assignment) {
        return this.studentAssignmentRepository.findAllByAssignment(assignment);
    }

    @Override
    @Transactional
    public Assignment createQuizAssignment(Classroom classroom, String assignmentName, String quizName){
        var quiz = this.quizService.getQuiz(quizName).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var quizAssignment = new QuizAssignment();
        quizAssignment.setName(assignmentName);
        quizAssignment.setQuiz(quiz);

        classroom.addAssignment(quizAssignment);

        this.assignmentRepository.save(quizAssignment);
        this.classroomService.saveClassroom(classroom);

        return quizAssignment;
    }

    @Override
    @Transactional
    public Assignment createExerciseAssignment(Classroom classroom, String assignmentName, String repositoryId) throws GitLabApiException {
        var exerciseAssignment = new ExerciseAssignment();
        exerciseAssignment.setName(assignmentName);
        exerciseAssignment.setGitlabRepositoryTemplateId(repositoryId);

        this.gitlab.createGroup(exerciseAssignment, classroom);

        classroom.addAssignment(exerciseAssignment);

        this.assignmentRepository.save(exerciseAssignment);
        this.classroomService.saveClassroom(classroom);

        return exerciseAssignment;
    }

    @Override
    public StudentAssignment getAssignmentResultsForStudent(Assignment assignment, ClassroomUser student) {
        return this.studentAssignmentRepository.findByAssignmentAndStudent(assignment, student);
    }

    @Override
    public List<StudentAssignment> getAllStudentAssignmentsForAClassroom(Classroom classroom, ClassroomUser student) {
        return this.studentAssignmentRepository.findByAssignmentClassroomIdAndStudent(classroom.getId(), student);
    }
}
