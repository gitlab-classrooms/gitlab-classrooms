package fr.univ_lille.gitlab.classrooms.gitlab;

import fr.univ_lille.gitlab.classrooms.assignments.ExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.assignments.StudentExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupParams;
import org.gitlab4j.api.models.Project;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Service
class GitlabImpl implements Gitlab {

    private final GitLabApi gitLabApi;

    private final GitlabApiFactory gitlabApiFactory;

    public GitlabImpl(GitLabApi gitLabApi, GitlabApiFactory gitlabApiFactory) {
        this.gitLabApi = gitLabApi;
        this.gitlabApiFactory = gitlabApiFactory;
    }

    @Override
    public List<Project> getProjectTemplates(Classroom classroom) throws GitLabApiException {
        var group = this.gitLabApi.getGroupApi().getGroup(classroom.getGitlabGroupId());
        var templateGroup = this.gitLabApi.getGroupApi().getGroups(group.getPath()+"/templates");
        if(templateGroup.isEmpty()){
            return List.of();
        }
        return this.gitLabApi.getGroupApi().getProjects(templateGroup.getFirst().getFullPath());
    }

    @Override
    public List<Project> getProjectsOfConnectedUser() throws GitLabApiException {
        return this.gitLabApi.getProjectApi().getMemberProjects();
    }

    @Override
    public List<Group> getGroupsOfConnectedUser() throws GitLabApiException {
        return this.gitLabApi.getGroupApi().getGroups();
    }

    @Override
    public URI getGroupURI(Classroom classroom) throws GitLabApiException {
        var group = this.gitLabApi.getGroupApi().getGroup(classroom.getGitlabGroupId());
        return URI.create(group.getWebUrl());
    }

    /**
     * Slugify a name by removing all chars that are not letters, numbers, underscore or hyphens, and replacing them by hyphens.
     *
     * @param name value to slugify
     * @return slugified value
     */
    String slugify(String name) {
        // normalize string to remove diacritics
        return Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase()
                // replace all chars that are not chars, digits, dot, dash, underscore with -
                .replaceAll("[^a-zA-Z0-9.\\-_]", "-")
                // remove last special chars as path should not end with a special char
                .replaceAll("[^a-zA-Z0-9]+$", "");
    }

    @Override
    public void createGroup(Classroom classroom, Optional<Long> parentGroupId) throws GitLabApiException {
        var classroomName = classroom.getName();
        var groupPath = slugify(classroomName);

        var groupParams = new GroupParams()
                .withName(classroomName)
                .withPath(groupPath)
                .withDescription("Gitlab group for the Classroom " + classroomName);
        parentGroupId.ifPresent(groupParams::withParentId);

        var group = this.gitLabApi.getGroupApi().createGroup(groupParams);

        classroom.setGitlabGroupId(group.getId());
    }

    @Override
    public void createGroup(ExerciseAssignment exerciseAssignment, Classroom classroom) throws GitLabApiException {
        var assignmentName = exerciseAssignment.getName();
        var groupPath = slugify(assignmentName);

        var groupParams = new GroupParams()
                .withName(assignmentName)
                .withPath(groupPath)
                .withDescription("Gitlab group for the assignment " + assignmentName)
                .withParentId(classroom.getGitlabGroupId());

        var group = this.gitLabApi.getGroupApi().createGroup(groupParams);

        exerciseAssignment.setGitlabGroupId(group.getId());
    }

    @Override
    public Project createStudentProject(ExerciseAssignment exerciseAssignment, ClassroomUser student) throws GitLabException {
        var classroom = exerciseAssignment.getClassroom();

        var teacher = classroom.getTeachers().stream().findFirst();

        if(teacher.isEmpty()){
            throw new GitLabException("Could not create student %s project for assignment %s. Classroom has no teacher.".formatted(student.getName(), exerciseAssignment.getName()));
        }
        // get a gitlab api client with the teacher's rights
        var teacherGitlabApi = this.gitlabApiFactory.userGitlabApi(teacher.get());

        // create the project if needed
        var project = this.ensureStudentProjectExists(teacherGitlabApi, exerciseAssignment, student);

        // grant the student access to its project
        this.ensureStudentCanAccessItsProject(teacherGitlabApi, project, student);

        return project;
    }

    @Override
    public void archiveProject(StudentExerciseAssignment studentExerciseAssignment) throws GitLabException {
        var projectId = studentExerciseAssignment.getGitlabProjectId();

        var classroom = studentExerciseAssignment.getAssignment().getClassroom();
        var teacher = classroom.getTeachers().stream().findFirst();

        if(teacher.isEmpty()){
            throw new GitLabException("Could not archive student %s project for assignment %s. Classroom has no teacher.".formatted(studentExerciseAssignment.getStudent().getName(), studentExerciseAssignment.getAssignment().getName()));
        }
        // get a gitlab api client with the teacher's rights
        var teacherGitlabApi = this.gitlabApiFactory.userGitlabApi(teacher.get());

        try {
            teacherGitlabApi.getProjectApi().archiveProject(projectId);
        } catch (GitLabApiException e) {
            throw new GitLabException("Unable to archive GitLab project with id '%s'".formatted(projectId), e);
        }
    }

    private Project ensureStudentProjectExists(GitLabApi teacherGitlabApi, ExerciseAssignment exerciseAssignment, ClassroomUser student) throws GitLabException {
        var groupId = exerciseAssignment.getGitlabGroupId();
        var group = teacherGitlabApi.getGroupApi().getOptionalGroup(groupId).orElseThrow(() -> new GitLabException("Group with id %s doest not exists".formatted(groupId)));
        var projectName = exerciseAssignment.getName() + "-" + student.getName();
        var projectPath = slugify(projectName);

        var existingProject = teacherGitlabApi.getProjectApi().getOptionalProject(group.getFullPath(), projectPath);
        if (existingProject.isPresent()) {
            return existingProject.get();
        }
        try {
            if (exerciseAssignment.getGitlabRepositoryTemplateId() != null && !exerciseAssignment.getGitlabRepositoryTemplateId().isBlank()) {
                // fork the template project
                return teacherGitlabApi.getProjectApi().forkProject(exerciseAssignment.getGitlabRepositoryTemplateId(), group.getFullPath(), projectPath, projectName);
            } else {
                // create a blank project
                var projectParams = new Project().withPath(projectPath).withName(projectName).withNamespaceId(group.getId());
                return teacherGitlabApi.getProjectApi().createProject(projectParams);
            }
        } catch (GitLabApiException e) {
            throw new GitLabException("Unable to create GitLab project with path '%s' in group '%s'".formatted(projectPath, group.getFullPath()), e);
        }
    }

    private void ensureStudentCanAccessItsProject(GitLabApi teacherGitlabApi, Project project, ClassroomUser student) throws GitLabException {
        var member = teacherGitlabApi.getProjectApi().getOptionalMember(project.getId(), student.getGitlabUserId(), true);
        if (member.isPresent()) {
            return;
        }
        // grant the student access to its project
        try {
            teacherGitlabApi.getProjectApi().addMember(project.getId(), student.getGitlabUserId(), AccessLevel.MAINTAINER);
        } catch (GitLabApiException e) {
            var message = String.format("Unable to give student '%s' access to its GitLab project '%s'", student.getGitlabUserId(), project.getId());
            throw new GitLabException(message, e);
        }
    }

}
