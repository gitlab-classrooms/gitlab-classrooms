package fr.univ_lille.gitlab.classrooms.export;

import fr.univ_lille.gitlab.classrooms.assignments.*;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
class ExportServiceImpl implements ExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportServiceImpl.class.getName());

    private final StudentAssignmentService assignmentService;

    ExportServiceImpl(StudentAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    public record StudentRepository(String studentName, List<String> cloneUrls){}

    @Override
    public List<StudentRepository> listStudentRepositories(ClassroomEntity classroom) {
        LOGGER.info("Listing student repositories for classroom {} with name {}", classroom.getId(), classroom.getName());

        var studentRepositoriesList = new LinkedList<StudentRepository>();

        for(ClassroomUser student : classroom.getStudents()){
            var studentExerciseAssignments = assignmentService.getAllStudentExerciseAssignmentsForAClassroom(classroom, student);

            var cloneUrls = studentExerciseAssignments.stream()
                    .map(StudentExerciseAssignment::getGitlabCloneUrl)
                    .toList();

            studentRepositoriesList.add(new StudentRepository(student.getName(), cloneUrls));
        }

        return studentRepositoriesList;
    }

}
