package com.example.quizgenbackend.controller;

import com.example.quizgenbackend.generator.QuizGenerator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/quiz")
public class QuizController {

    @GetMapping("/hi")
    public String test() {
        return "hi";
    }

    @PostMapping("/generate/word")
    public ResponseEntity<FileSystemResource> generateQuiz(@RequestBody Map<String, String> inputData) {
        String input = inputData.get("input");
        String filePaths = QuizGenerator.generateQuizFile(input);

        String[] paths = filePaths.split(",");
        if (paths.length != 2) {
            return ResponseEntity.internalServerError().body(null);
        }

        File wordFile = new File(paths[0]);
        File csvFile = new File(paths[1]);

        if (!wordFile.exists() || !csvFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + wordFile.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(wordFile));
    }

    @PostMapping("/generate/csv")
    public ResponseEntity<FileSystemResource> generateQuizCSV(@RequestBody Map<String, String> inputData) {
        String input = inputData.get("input");
        String filePaths = QuizGenerator.generateQuizFile(input);

        String[] paths = filePaths.split(",");
        if (paths.length != 2) {
            return ResponseEntity.internalServerError().body(null);
        }

        File wordFile = new File(paths[0]);
        File csvFile = new File(paths[1]);

        if (!wordFile.exists() || !csvFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + csvFile.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(csvFile));
    }
}
