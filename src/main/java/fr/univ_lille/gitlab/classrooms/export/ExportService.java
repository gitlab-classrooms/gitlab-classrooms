package fr.univ_lille.gitlab.classrooms.export;

import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomEntity;

import java.util.List;


/**
 * Service that helps to export data from GitLab Classrooms.
 */
public interface ExportService {

    List<ExportServiceImpl.StudentRepository> listStudentRepositories(ClassroomEntity classroom);

}
