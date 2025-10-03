package fr.univ_lille.gitlab.classrooms.adapters.jpa;

import fr.univ_lille.gitlab.classrooms.assignments.Assignment;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Utility mapper to convert persistence ClassroomEntity to domain Classroom.
 */
@Component
public class ClassroomEntityMapper {

    public Classroom toClassroom(ClassroomEntity entity) {
        if (entity == null) {
            return null;
        }
        var classroom = new Classroom();
        classroom.setId(entity.getId());
        classroom.setName(entity.getName());
        classroom.setGitlabUrl(entity.getGitlabUrl());
        classroom.setGitlabGroupId(entity.getGitlabGroupId());
        classroom.setArchived(entity.isArchived());

        // Copy collections defensively to decouple persistence context from domain
        var teachers = entity.getTeachers() == null ? new HashSet<ClassroomUser>() : new HashSet<>(entity.getTeachers());
        classroom.setTeachers(teachers);

        var students = entity.getStudents() == null ? new HashSet<ClassroomUser>() : new HashSet<>(entity.getStudents());
        classroom.setStudents(students);

        var assignments = new LinkedList<Assignment>();
        if (entity.getAssignments() != null) {
            assignments.addAll(entity.getAssignments());
        }
        classroom.setAssignments(assignments);

        return classroom;
    }
}
