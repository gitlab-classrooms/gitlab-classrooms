package fr.univ_lille.gitlab.classrooms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface QuizRepository extends JpaRepository<QuizEntity, String> {
    List<QuizEntity> findAllByArchivedFalse();
}
