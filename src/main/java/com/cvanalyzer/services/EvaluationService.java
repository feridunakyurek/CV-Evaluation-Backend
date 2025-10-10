package com.cvanalyzer.services;

import com.cvanalyzer.entities.CvUpload;
import com.cvanalyzer.entities.Evaluation;
import com.cvanalyzer.entities.User;
import com.cvanalyzer.exceptions.CvNotFoundException;
import com.cvanalyzer.exceptions.EvaluationAlreadyExistsException;
import com.cvanalyzer.exceptions.UnauthorizedAccessException;
import com.cvanalyzer.exceptions.UserNotFoundException;
import com.cvanalyzer.repos.CvUploadRepository;
import com.cvanalyzer.repos.EvaluationRepository;
import com.cvanalyzer.repos.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final CvUploadRepository cvUploadRepository;
    private final UserRepository userRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;


    public EvaluationService(EvaluationRepository evaluationRepository, CvUploadRepository cvUploadRepository, UserRepository userRepository, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.evaluationRepository = evaluationRepository;
        this.cvUploadRepository = cvUploadRepository;
        this.userRepository = userRepository;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Evaluation analyzeCvAndVerifyUser(Long cvId, String userEmail) throws IOException, UserNotFoundException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı."));

        CvUpload cvUpload = cvUploadRepository.findById(cvId)
                .orElseThrow(() -> new CvNotFoundException("CV bulunamadı."));

        if (!cvUpload.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedAccessException("Bu CV'yi analiz etme yetkiniz yok.");
        }

        Optional<Evaluation> existingEvaluation = evaluationRepository.findByCvUpload(cvUpload);
        if (existingEvaluation.isPresent()) {
            throw new EvaluationAlreadyExistsException("Bu CV zaten değerlendirilmiş.");
        }

        String cvText = extractTextFromCv(cvUpload.getFilePath());

        String prompt = "Aşağıdaki CV içeriğini incele ve 0 ile 100 arasında bir puan ver." +
                " Ayrıca, CV'yi genel olarak özetleyen ve iyileştirme için öneriler sunan bir özet yaz." +
                " Bu özet 2000 karakteri kesinlikle geçmemelidir." +
                " SADECE ve KESİNLİKLE JSON formatında yanıt ver. JSON anahtarları 'score' ve 'summary' olsun." +
                " CV içeriği: " + cvText;

        String aiResponse = chatClient.prompt().user(prompt).call().content();

        double score;
        String summary;
        try {
            int jsonStart = aiResponse.indexOf('{');
            int jsonEnd = aiResponse.lastIndexOf('}');
            String cleanedResponse = aiResponse;

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleanedResponse = aiResponse.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode root = objectMapper.readTree(cleanedResponse);
            score = root.path("score").asDouble();
            summary = root.path("summary").asText();

            if (summary.length() > 2000) {
                summary = summary.substring(0, 2000);
            }

        } catch (IOException e) {
            score = 0;
            summary = "JSON ayrıştırma hatası oluştu: " + aiResponse;

            if (summary.length() > 2000) {
                summary = summary.substring(0, 2000);
            }
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setScore(score); // Rastgele puan yerine AI'dan gelen puan
        evaluation.setAnalysisSummary(summary); // Sabit metin yerine AI'dan gelen özet
        evaluation.setEvaluationType("AI_ANALYSIS");
        evaluation.setCvUpload(cvUpload);
        evaluation.setUser(user);

        return evaluationRepository.save(evaluation);
    }

    public List<Evaluation> getByUserEmail(String userEmail) throws UserNotFoundException {
        // Kullanıcıyı bul
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı."));

        // Kullanıcının tüm değerlendirmelerini getir
        return evaluationRepository.findByUser(user);
    }

    private String extractTextFromCv(String filePath) throws IOException {
        String fullPath = Paths.get("uploads", new File(filePath).getName()).toAbsolutePath().toString();
        try (FileInputStream inputStream = new FileInputStream(new File(filePath))) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();

            parser.parse(inputStream, handler, metadata);
            return handler.toString();
        } catch (TikaException | SAXException e) {
            throw new IOException("Dosya içeriği okunurken bir hata oluştu.", e);
        }
    }
}