package fr.univ_lille.gitlab.classrooms.domain.classrooms;

import fr.univ_lille.gitlab.classrooms.assignments.Assignment;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;

import java.net.URL;
import java.util.*;

public class Classroom {

    private UUID id = UUID.randomUUID();

    private String name;

    private URL gitlabUrl;

    private Long gitlabGroupId;

    private boolean archived = false;

    private Set<ClassroomUser> teachers = new HashSet<>();

    private Set<ClassroomUser> students = new HashSet<>();

    private List<Assignment> assignments = new LinkedList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getGitlabUrl() {
        return gitlabUrl;
    }

    public void setGitlabUrl(URL gitlabUrl) {
        this.gitlabUrl = gitlabUrl;
    }

    public Set<ClassroomUser> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<ClassroomUser> teachers) {
        this.teachers = teachers;
    }

    public void addTeacher(ClassroomUser teacher) {
        this.teachers.add(teacher);
    }

    public Set<ClassroomUser> getStudents() {
        return students;
    }

    public void setStudents(Set<ClassroomUser> students) {
        this.students = students;
    }

    public void join(ClassroomUser classroomUser){
        this.students.add(classroomUser);
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public void addAssignment(Assignment assignment) {
        this.assignments.add(assignment);
    }

    public Long getGitlabGroupId() {
        return gitlabGroupId;
    }

    public void setGitlabGroupId(Long gitlabGroupId) {
        this.gitlabGroupId = gitlabGroupId;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
