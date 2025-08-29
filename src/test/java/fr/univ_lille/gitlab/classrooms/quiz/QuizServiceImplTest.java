package fr.univ_lille.gitlab.classrooms.quiz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

    @InjectMocks
    private QuizServiceImpl quizService;

    @Mock
    private QuizRepository quizRepository;

    @Test
    void testGetAllQuizzes(){
        var quizzes = this.quizService.getAllQuizzes();

        assertThat(quizzes).isNotNull();

        verify(quizRepository).findAllByArchivedFalse();
    }

    @Test
    void testGetQuizzes(){
        when(quizRepository.findById("dummy")).thenReturn(Optional.of(new QuizEntity()));

        var quiz = this.quizService.getQuiz("dummy");

        assertThat(quiz).isNotNull().isPresent();
    }

}
