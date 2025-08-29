package fr.univ_lille.gitlab.classrooms.quiz;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;


    public QuizServiceImpl(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }


    @Override
    public List<QuizEntity> getAllQuizzes() {
        return this.quizRepository.findAllByArchivedFalse();
    }

    @Override
    public Optional<QuizEntity> getQuiz(String id) {
        return this.quizRepository.findById(id);
    }
}
