package fr.univ_lille.gitlab.classrooms.classrooms;

import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceImplTest {

    @InjectMocks
    private ClassroomServiceImpl classroomService;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private Gitlab gitlab;

    @Test
    void getAllClassrooms_shouldReturnAllNonArchivedClassrooms() {
        var activeClassroom = new ClassroomEntity();
        var archiveClassroom = new ClassroomEntity();
        archiveClassroom.setArchived(true);
        var allClassrooms = java.util.List.of(activeClassroom, archiveClassroom);

        when(classroomRepository.findAll()).thenReturn(allClassrooms);

        var classrooms = classroomService.getAllClassrooms();

        assertThat(classrooms).isNotNull()
                .hasSize(1)
                .contains(activeClassroom)
                .doesNotContain(archiveClassroom);

        verify(classroomRepository).findAll();
    }

    @Test
    void getAllJoinedClassrooms_shouldReturnAllNonArchivedJoinedClassrooms() {
        var student = new ClassroomUser();

        var activeClassroom = new ClassroomEntity();
        var archiveClassroom = new ClassroomEntity();
        archiveClassroom.setArchived(true);
        var allJoinedClassrooms = java.util.List.of(activeClassroom, archiveClassroom);

        when(classroomRepository.findClassroomByStudentsContains(student)).thenReturn(allJoinedClassrooms);

        var classrooms = classroomService.getAllJoinedClassrooms(student);

        assertThat(classrooms).isNotNull()
                .hasSize(1)
                .contains(activeClassroom)
                .doesNotContain(archiveClassroom);

        verify(classroomRepository).findClassroomByStudentsContains(student);
    }

    @Test
    void joinClassroom_shouldAddTheStudentToTheClassroom() {
        var student = new ClassroomUser();
        student.setName("luke.skywalker");

        var classroom = new ClassroomEntity();
        classroom.setGitlabGroupId(12L);

        classroomService.joinClassroom(classroom, student);

        assertThat(classroom.getStudents()).contains(student);

        verify(classroomRepository).save(classroom);
    }

    @Test
    void createClassroom_shouldCreateTheGitlabGroupAndSave() throws GitLabApiException {
        var teacher = new ClassroomUser();
        teacher.setName("obiwan.kenobi");

        classroomService.createClassroom("Test classroom", 12L, teacher);

        var classroomCaptor = ArgumentCaptor.forClass(ClassroomEntity.class);

        verify(gitlab).createGroup(classroomCaptor.capture(), eq(Optional.of(12L)));

        verify(classroomRepository).save(classroomCaptor.capture());

        assertThat(classroomCaptor.getValue())
                .isNotNull()
                .satisfies(it -> {
                    assertThat(it.getTeachers()).contains(teacher);
                });
    }

    @Test
    void archiveClassroom_shouldSetArchivedToTrue() {
        var classroom = new ClassroomEntity();
        assertThat(classroom.isArchived()).isFalse();

        classroomService.archiveClassroom(classroom);

        assertThat(classroom.isArchived()).isTrue();
        verify(classroomRepository).save(classroom);
    }

    @Test
    void unarchiveClassroom_shouldSetArchivedToFalse() {
        var classroom = new ClassroomEntity();
        classroom.setArchived(true);
        assertThat(classroom.isArchived()).isTrue();

        classroomService.unarchiveClassroom(classroom);

        assertThat(classroom.isArchived()).isFalse();
        verify(classroomRepository).save(classroom);
    }
}
