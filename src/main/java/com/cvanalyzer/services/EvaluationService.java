package com.cvanalyzer.services;

import com.cvanalyzer.entities.Evaluation;
import com.cvanalyzer.repos.EvaluationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class EvaluationService {

    private final EvaluationRepository repo;

    public EvaluationService(EvaluationRepository repo) {
        this.repo = repo;
    }

    public List<Evaluation> getByUser(Long userId) {
        return repo.findByUserId(userId);
    }

    public Evaluation analyzeAndSave(Evaluation eval) {
        // Mock AI analizi
        Random r = new Random();
        double score = 50 + r.nextDouble() * 50;
        String comment = score > 75 ?
                "Güçlü bir özgeçmiş! Teknik beceriler çok iyi." :
                "CV geliştirilebilir. Daha fazla deneyim ve detay eklenmeli.";

        eval.setScore(score);
        eval.setAiComment(comment);
        return repo.save(eval);
    }
}
