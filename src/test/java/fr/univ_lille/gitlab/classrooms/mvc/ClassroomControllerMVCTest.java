package fr.univ_lille.gitlab.classrooms.mvc;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import fr.univ_lille.gitlab.classrooms.users.WithMockStudent;
import fr.univ_lille.gitlab.classrooms.users.WithMockTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/sql/init-test-users.sql")
class ClassroomControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassroomService classroomService;

    @MockBean
    private Gitlab gitlab;

    private final UUID classroomId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setName("ClassroomControllerMVCTest classroom");
        when(classroomService.getClassroom(classroomId)).thenReturn(Optional.of(classroom));
    }

    @Nested
    @WithMockTeacher
    class TeacherRole {

        @Test
        void shouldAccessNewClassroomPage() throws Exception {
            mockMvc.perform(get("/classrooms/new"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @WithMockStudent
    class StudentRole {

        @Test
        void shouldNotAccessNewClassroomPage() throws Exception {
            mockMvc.perform(get("/classrooms/new"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldNotAccessNewAssignmentPage() throws Exception {
            mockMvc.perform(get("/classrooms/" + classroomId + "/assignments/new"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockTeacher
    void newClassroom_shouldListTheGroupsOfTheTeacher() throws Exception {
        mockMvc.perform(get("/classrooms/new"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(this.gitlab).getGroupsOfConnectedUser();
    }

    @Test
    @WithMockTeacher
    void showClassroom_shouldListClassroomAssignments() throws Exception {
        mockMvc.perform(get("/classrooms/"+classroomId))
                .andDo(print())
                .andExpect(status().isOk());

        verify(this.classroomService).getClassroom(classroomId);
        verify(this.gitlab).getGroupURI(any());
    }

    @Test
    @WithMockTeacher
    void createClassroom_shouldCreateAGitlabGroup() throws Exception {
        mockMvc.perform(
                        post("/classrooms/new")
                                .with(csrf())
                                .param("classroomName", "ClassroomControllerMVCTest newClassroom"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(classroomService).createClassroom(eq("ClassroomControllerMVCTest newClassroom"), isNull(), any());
    }

    @Test
    @WithMockTeacher
    void createClassroom_shouldUseTheParentGitLabGroup() throws Exception {
        mockMvc.perform(
                        post("/classrooms/new")
                                .with(csrf())
                                .param("classroomName", "ClassroomControllerMVCTest newClassroom")
                                .param("parentGitLabGroupId", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        var captor = ArgumentCaptor.forClass(ClassroomUser.class);

        verify(classroomService).createClassroom(eq("ClassroomControllerMVCTest newClassroom"), eq(12L), captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .extracting("name").isEqualTo("obiwan.kenobi");
    }

    @Test
    @WithMockTeacher
    void createClassroom_shouldSaveTheAssociatedTeacher() throws Exception {
        mockMvc.perform(
                        post("/classrooms/new")
                                .with(csrf())
                                .param("classroomName", "ClassroomControllerMVCTest newClassroom"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        var captor = ArgumentCaptor.forClass(ClassroomUser.class);

        verify(classroomService).createClassroom(eq("ClassroomControllerMVCTest newClassroom"), isNull(), captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .extracting("name").isEqualTo("obiwan.kenobi");
    }

    @Test
    @WithMockStudent
    void joinClassroom_shouldShowJoinPage() throws Exception {
        mockMvc.perform(get("/classrooms/" + classroomId + "/join"))
                .andExpect(status().isOk())
                .andExpect(view().name("classrooms/join"))
                .andExpect(model().attributeExists("classroom"));
    }

    @Test
    @WithMockStudent
    void joinClassroom_shouldAddStudentToTheListOfJoined() throws Exception {
        mockMvc.perform(post("/classrooms/" + classroomId + "/join").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("classrooms/joined"));

        verify(classroomService).joinClassroom(any(), any());
    }

    @Test
    @WithMockStudent
    void joinClassroom_shouldAddStudentToTheListOfJoined_andRedirectIfSessionAttributExists() throws Exception {
        mockMvc.perform(
                        post("/classrooms/" + classroomId + "/join")
                                .sessionAttr("redirect", "/assignments/123456/accept")
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/assignments/123456/accept"));

        verify(classroomService).joinClassroom(any(), any());
    }

    @Test
    @WithMockTeacher
    void archiveClassroom_shouldArchiveAndRedirect_forTeacher() throws Exception {
        mockMvc.perform(get("/classrooms/" + classroomId + "/archive"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(classroomService).archiveClassroom(any());
    }

    @Test
    @WithMockStudent
    void archiveClassroom_shouldBeForbidden_forStudent() throws Exception {
        mockMvc.perform(get("/classrooms/" + classroomId + "/archive"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(classroomService, never()).archiveClassroom(any());
    }
}
