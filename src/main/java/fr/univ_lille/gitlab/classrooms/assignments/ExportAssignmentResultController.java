package fr.univ_lille.gitlab.classrooms.assignments;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.UUID;

@Controller
@RolesAllowed("TEACHER")
class ExportAssignmentResultController {

    private final ExportAssignmentResultUseCase exportUseCase;

    ExportAssignmentResultController(ExportAssignmentResultUseCase exportUseCase) {
        this.exportUseCase = exportUseCase;
    }

    @GetMapping(value = "/assignments/{assignmentId}/export.csv", produces = "text/csv")
    ResponseEntity<String> exportCsv(@PathVariable UUID assignmentId) {
        var data = exportUseCase.getExportData(assignmentId);

        var csv = buildCsv(data);

        var filenameBase = (data.assignmentName() != null && !data.assignmentName().isBlank())
                ? data.assignmentName()
                : data.assignmentId().toString();
        var filename = "assignment-results-" + filenameBase + ".csv";
        var encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(new MediaType("text","csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    private String buildCsv(ExportAssignmentResultUseCase.ExportData data) {
        var sb = new StringBuilder();
        // Header
        sb.append("student email,student name,assignment score,max score,retakes,submission date\n");

        var dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        for (var row : data.rows()) {
            String email = safeCsv(row.studentEmail());
            String name = safeCsv(row.studentName());
            String score = row.assignmentScore() == null ? "" : row.assignmentScore().toString();
            String maxScore = row.maxScore() == null ? "" : row.maxScore().toString();
            String retakes = "";
            if (data.assignmentType() == AssignmentType.QUIZ) {
                retakes = row.retakes() == null ? "" : row.retakes().toString();
            }
            String date = row.submissionDate() == null ? "" : dtf.format(row.submissionDate());

            StringJoiner joiner = new StringJoiner(",");
            joiner.add(email).add(name).add(score).add(maxScore).add(retakes).add(safeCsv(date));
            sb.append(joiner).append("\n");
        }
        return sb.toString();
    }

    private String safeCsv(String value) {
        if (value == null) return "";
        boolean mustQuote = value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"");
        String processed = value.replace("\"", "\"\"");
        if (mustQuote) {
            return "\"" + processed + "\"";
        }
        return processed;
    }
}
