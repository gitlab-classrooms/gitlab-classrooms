package fr.univ_lille.gitlab.classrooms.adapters.jpa;

import fr.univ_lille.gitlab.classrooms.assignments.Assignment;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Utility mapper to convert between persistence ClassroomEntity and domain Classroom.
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

    public ClassroomEntity toEntity(Classroom classroom) {
        if (classroom == null) {
            return null;
        }
        var entity = new ClassroomEntity();
        entity.setId(classroom.getId());
        entity.setName(classroom.getName());
        entity.setGitlabUrl(classroom.getGitlabUrl());
        entity.setGitlabGroupId(classroom.getGitlabGroupId());
        entity.setArchived(classroom.isArchived());

        // Copy teachers and students defensively
        var teachers = classroom.getTeachers() == null ? new HashSet<ClassroomUser>() : new HashSet<>(classroom.getTeachers());
        entity.setTeachers(teachers);

        var students = classroom.getStudents() == null ? new HashSet<ClassroomUser>() : new HashSet<>(classroom.getStudents());
        entity.setStudents(students);

        // Map assignments ensuring back-reference is set
        if (classroom.getAssignments() != null) {
            for (Assignment assignment : classroom.getAssignments()) {
                if (assignment != null) {
                    entity.addAssignment(assignment);
                }
            }
        }

        return entity;
    }
}
