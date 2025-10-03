package fr.univ_lille.gitlab.classrooms.gitlab;

import fr.univ_lille.gitlab.classrooms.assignments.ExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.users.ClassroomRole;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitlabImplTest {

    @InjectMocks
    private GitlabImpl gitlab;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GitLabApi gitLabApi;

    @Mock
    private GitlabApiFactory gitlabApiFactory;

    @Test
    void getProjectsOfConnectedUser_shouldListMemberProjects() throws GitLabApiException {
        gitlab.getProjectsOfConnectedUser();

        verify(gitLabApi.getProjectApi()).getMemberProjects();
    }

    @Test
    void getGroupsOfConnectedUser_shouldListUserGroups() throws GitLabApiException {
        gitlab.getGroupsOfConnectedUser();

        verify(gitLabApi.getGroupApi()).getGroups();
    }

    @Test
    void getGroupURI_shouldGetTheURI_forAGivenClassroom() throws GitLabApiException {
        var classroom = new Classroom();
        classroom.setGitlabGroupId(12L);

        var group = new Group();
        group.setWebUrl("https://gitlab.univ-lille.fr/gitlab-classrooms");

        when(gitLabApi.getGroupApi().getGroup(12L)).thenReturn(group);

        var uri = gitlab.getGroupURI(classroom);

        assertThat(uri).isEqualTo(URI.create("https://gitlab.univ-lille.fr/gitlab-classrooms"));
    }

    @Test
    void createGroup_shouldCreateAGroup_forAGivenClassroom() throws GitLabApiException{
        var classroom = new Classroom();
        classroom.setName("Test classroom");

        var group = new Group();
        group.setId(36L);

        when(gitLabApi.getGroupApi().createGroup(any())).thenReturn(group);

        gitlab.createGroup(classroom, Optional.empty());

        var gitlabGroupCaptor = ArgumentCaptor.forClass(GroupParams.class);
        verify(this.gitLabApi.getGroupApi()).createGroup(gitlabGroupCaptor.capture());

        assertThat(gitlabGroupCaptor.getValue())
                .hasFieldOrPropertyWithValue("name", "Test classroom")
                .hasFieldOrPropertyWithValue("path", "test-classroom")
                .hasFieldOrPropertyWithValue("description", "Gitlab group for the Classroom Test classroom");

        assertThat(classroom.getGitlabGroupId()).isEqualTo(36L);
    }

    @Test
    void createGroup_shouldCreateAGroup_forAGivenExerciseAssignment() throws GitLabApiException{
        var classroom = new Classroom();
        classroom.setName("Test classroom");
        classroom.setGitlabGroupId(12L);

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");

        var group = new Group();
        group.setId(72L);
        when(gitLabApi.getGroupApi().createGroup(any())).thenReturn(group);

        gitlab.createGroup(assignment, classroom);

        var gitlabGroupCaptor = ArgumentCaptor.forClass(GroupParams.class);
        verify(this.gitLabApi.getGroupApi()).createGroup(gitlabGroupCaptor.capture());

        assertThat(gitlabGroupCaptor.getValue())
                .hasFieldOrPropertyWithValue("name", "Exercice 1")
                .hasFieldOrPropertyWithValue("path", "exercice-1")
                .hasFieldOrPropertyWithValue("parentId", 12L)
                .hasFieldOrPropertyWithValue("description", "Gitlab group for the assignment Exercice 1");

        assertThat(assignment.getGitlabGroupId()).isEqualTo(72L);
    }

    @Test
    void createProject_shouldCreateAProject_andGiveAccessToTheStudent() throws GitLabApiException, GitLabException {
        var student = new ClassroomUser("luke.skywalker", List.of(ClassroomRole.STUDENT));
        student.setGitlabUserId(8L);
        var teacher = new ClassroomUser("obiwan.kenobi", List.of(ClassroomRole.TEACHER));

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");
        assignment.setGitlabGroupId(72L);

        var group = new Group();
        group.setId(72L);
        group.setFullPath("my-group/path");
        when(gitLabApi.getGroupApi().getOptionalGroup(72L)).thenReturn(Optional.of(group));

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);
        classroom.addAssignment(assignment);

        when(gitlabApiFactory.userGitlabApi(teacher)).thenReturn(gitLabApi);

        var projectMock = new Project();
        projectMock.setId(125L);
        // project non existing
        when(gitLabApi.getProjectApi().getOptionalProject("my-group/path", "exercice-1-luke.skywalker")).thenReturn(Optional.empty());
        when(gitLabApi.getProjectApi().createProject(any(Project.class))).thenReturn(projectMock);

        // no rights on project yet
        when(gitLabApi.getProjectApi().getOptionalMember(125L, 8L, true)).thenReturn(Optional.empty());

        gitlab.createStudentProject(assignment, student);

        var projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(gitLabApi.getProjectApi()).createProject(projectCaptor.capture());

        var project = projectCaptor.getValue();
        assertThat(project).isNotNull();
        assertThat(project.getName()).isEqualTo("Exercice 1-luke.skywalker");
        assertThat(project.getPath()).isEqualTo("exercice-1-luke.skywalker");
        assertThat(project.getNamespace().getId()).isEqualTo(72L);

        verify(gitLabApi.getProjectApi()).addMember(125L, 8L, AccessLevel.MAINTAINER);

        verifyNoMoreInteractions(gitLabApi.getProjectApi());
    }

    @Test
    void createProject_shouldNotCreateAProject_ifClassroomHasNoTeacher() throws GitLabApiException, GitLabException {
        var student = new ClassroomUser("luke.skywalker", List.of(ClassroomRole.STUDENT));
        student.setGitlabUserId(8L);

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");

        var classroom = new ClassroomEntity();
        classroom.addAssignment(assignment);

        assertThatThrownBy(() -> gitlab.createStudentProject(assignment, student))
                .isInstanceOf(GitLabException.class)
                .hasMessage("Could not create student luke.skywalker project for assignment Exercice 1. Classroom has no teacher.");

        verifyNoMoreInteractions(gitLabApi.getProjectApi());
    }

    @Test
    void createProject_shouldNotCreateAProject_ifProjectAlreadyExists() throws GitLabApiException, GitLabException {
        var student = new ClassroomUser("luke.skywalker", List.of(ClassroomRole.STUDENT));
        student.setGitlabUserId(8L);
        var teacher = new ClassroomUser("obiwan.kenobi", List.of(ClassroomRole.TEACHER));

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");
        assignment.setGitlabGroupId(72L);

        var group = new Group();
        group.setId(72L);
        group.setFullPath("my-group/path");
        when(gitLabApi.getGroupApi().getOptionalGroup(72L)).thenReturn(Optional.of(group));

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);
        classroom.addAssignment(assignment);

        when(gitlabApiFactory.userGitlabApi(teacher)).thenReturn(gitLabApi);

        // project already exists
        var projectMock = new Project();
        projectMock.setId(125L);
        when(gitLabApi.getProjectApi().getOptionalProject("my-group/path", "exercice-1-luke.skywalker")).thenReturn(Optional.of(projectMock));

        // no rights on project
        when(gitLabApi.getProjectApi().getOptionalMember(125L, 8L, true)).thenReturn(Optional.empty());

        gitlab.createStudentProject(assignment, student);

        verify(gitLabApi.getProjectApi()).addMember(125L, 8L, AccessLevel.MAINTAINER);

        verifyNoMoreInteractions(gitLabApi.getProjectApi());
    }

    @Test
    void createProject_shouldNotCreateAProject_ifProjectAlreadyExistsAndStudentAlreadyHasRights() throws GitLabApiException, GitLabException {
        var student = new ClassroomUser("luke.skywalker", List.of(ClassroomRole.STUDENT));
        student.setGitlabUserId(8L);
        var teacher = new ClassroomUser("obiwan.kenobi", List.of(ClassroomRole.TEACHER));

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 1");
        assignment.setGitlabGroupId(72L);

        var group = new Group();
        group.setId(72L);
        group.setFullPath("my-group/path");
        when(gitLabApi.getGroupApi().getOptionalGroup(72L)).thenReturn(Optional.of(group));

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);
        classroom.addAssignment(assignment);

        when(gitlabApiFactory.userGitlabApi(teacher)).thenReturn(gitLabApi);

        // project already exists
        var projectMock = new Project();
        projectMock.setId(125L);
        when(gitLabApi.getProjectApi().getOptionalProject("my-group/path", "exercice-1-luke.skywalker")).thenReturn(Optional.of(projectMock));

        // rights on project
        when(gitLabApi.getProjectApi().getOptionalMember(125L, 8L, true)).thenReturn(Optional.of(new Member()));

        gitlab.createStudentProject(assignment, student);

        verifyNoMoreInteractions(gitLabApi.getProjectApi());
    }

    @Test
    void createProject_withTemplate_shouldForkAProject_andGiveAccessToTheStudent() throws GitLabApiException, GitLabException {
        var student = new ClassroomUser("luke.skywalker", List.of(ClassroomRole.STUDENT));
        student.setGitlabUserId(8L);
        var teacher = new ClassroomUser("obiwan.kenobi", List.of(ClassroomRole.TEACHER));

        var assignment = new ExerciseAssignment();
        assignment.setName("Exercice 2 - Template");
        assignment.setGitlabGroupId(72L);
        assignment.setGitlabRepositoryTemplateId("12");

        var group = new Group();
        group.setId(72L);
        group.setFullPath("my-group/path");
        when(gitLabApi.getGroupApi().getOptionalGroup(72L)).thenReturn(Optional.of(group));

        var classroom = new ClassroomEntity();
        classroom.addTeacher(teacher);
        classroom.addAssignment(assignment);

        when(gitlabApiFactory.userGitlabApi(teacher)).thenReturn(gitLabApi);

        var projectMock = new Project();
        projectMock.setId(125L);
        // project non existing
        when(gitLabApi.getProjectApi().getOptionalProject("my-group/path", "exercice-2---template-luke.skywalker")).thenReturn(Optional.empty());
        when(gitLabApi.getProjectApi().forkProject("12", "my-group/path", "exercice-2---template-luke.skywalker", "Exercice 2 - Template-luke.skywalker")).thenReturn(projectMock);

        // no rights on project
        when(gitLabApi.getProjectApi().getOptionalMember(125L, 8L, true)).thenReturn(Optional.empty());

        gitlab.createStudentProject(assignment, student);

        verify(gitLabApi.getProjectApi()).forkProject("12", "my-group/path", "exercice-2---template-luke.skywalker", "Exercice 2 - Template-luke.skywalker");
        verify(gitLabApi.getProjectApi()).addMember(125L, 8L, AccessLevel.MAINTAINER);

        verifyNoMoreInteractions(gitLabApi.getProjectApi());
    }

    @Test
    void slugify_shouldReplacellSpecialChars(){
        assertThat( gitlab.slugify("luke.skywalker") ).isEqualTo("luke.skywalker");

        assertThat( gitlab.slugify("luke-skywalker") ).isEqualTo("luke-skywalker");

        assertThat( gitlab.slugify("luke_skywalker") ).isEqualTo("luke_skywalker");

        // special chars at the end should be stripped
        assertThat( gitlab.slugify("luke-skywalker_.-") ).isEqualTo("luke-skywalker");

        // no diacritics
        assertThat( gitlab.slugify("léïa") ).isEqualTo("leia");
    }
}
