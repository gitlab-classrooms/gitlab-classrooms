package fr.univ_lille.gitlab.classrooms.export;

import fr.univ_lille.gitlab.classrooms.adapters.jpa.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.classrooms.Classroom;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.users.WithMockStudent;
import fr.univ_lille.gitlab.classrooms.users.WithMockTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportControllerMVCTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassroomService classroomService;

    @MockBean
    private ExportService exportService;

    String expectedScript = """
            #!/bin/sh
            
            mkdir -p classroom
            cd classroom
            
            mkdir -p Luke
            cd Luke
            git clone git@gitlab:luke-repo-1.git
            git clone git@gitlab:luke-repo-2.git
            cd ..
            
            mkdir -p Leia
            cd Leia
            git clone git@gitlab:leia-repo-1.git
            git clone git@gitlab:leia-repo-2.git
            cd ..
            
            cd ..
            """;

    UUID classroomId = UUID.randomUUID();

    @Test
    @WithMockStudent
    void export_shouldNotBeAccessibleToStudents() throws Exception {
        mockMvc.perform(get("/classrooms/{classroomId}/export/clone-classroom.sh", classroomId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockTeacher
    void exportCloneClassroomScriptByStudent() throws Exception {
        var classroom = new Classroom();
        classroom.setName("Test Classroom");

        when(classroomService.getClassroom(classroomId)).thenReturn(Optional.of(classroom));

        var studentRepositories = List.of(
                new ExportServiceImpl.StudentRepository("Luke", List.of("git@gitlab:luke-repo-1.git", "git@gitlab:luke-repo-2.git")),
                new ExportServiceImpl.StudentRepository("Leia", List.of("git@gitlab:leia-repo-1.git", "git@gitlab:leia-repo-2.git"))
        );

        when(exportService.listStudentRepositories(classroom)).thenReturn(studentRepositories);

        mockMvc.perform(get("/classrooms/{classroomId}/export/clone-classroom.sh", classroomId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(content().string(expectedScript));
    }
}
