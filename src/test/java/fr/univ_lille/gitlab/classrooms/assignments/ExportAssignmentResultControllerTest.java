package fr.univ_lille.gitlab.classrooms.assignments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExportAssignmentResultController.class)
class ExportAssignmentResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportAssignmentResultUseCase exportAssignmentResultUseCase;

    // Mock OAuth2 client infrastructure required by security auto-configuration in MVC slice
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @MockitoBean
    private JdbcOperations jdbcOperations;

    @MockitoBean
    private ClassroomUserService classroomUserService;

    @Test
    @DisplayName("should return CSV export for given assignment id")
    @WithMockUser(roles = {"TEACHER"})
    void exportCsv_returnsCsvContent() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        var row = new ExportAssignmentResultUseCase.ExportRow(
                "alice@example.com",
                "Alice",
                8L,
                10L,
                0,
                null
        );
        var data = new ExportAssignmentResultUseCase.ExportData(
                assignmentId,
                "My Quiz",
                AssignmentType.QUIZ,
                List.of(row)
        );

        String expectedCsv = """
                student email,student name,assignment score,max score,retakes,submission date
                alice@example.com,Alice,8,10,0,
                """;

        when(exportAssignmentResultUseCase.getExportData(assignmentId)).thenReturn(data);

        // When/Then
        mockMvc.perform(get("/assignments/{assignmentId}/export.csv", assignmentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"assignment-results-")))
                .andExpect(content().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)))
                .andExpect(content().string(expectedCsv));

        // Verify interaction with use case
        Mockito.verify(exportAssignmentResultUseCase).getExportData(assignmentId);
    }
}
