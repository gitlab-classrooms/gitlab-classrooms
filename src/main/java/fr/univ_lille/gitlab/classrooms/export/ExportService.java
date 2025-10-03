package fr.univ_lille.gitlab.classrooms.export;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;

import java.util.List;


/**
 * Service that helps to export data from GitLab Classrooms.
 */
public interface ExportService {

    record StudentRepository(String studentName, List<String> cloneUrls){}

    List<StudentRepository> listStudentRepositories(Classroom classroom);

}
