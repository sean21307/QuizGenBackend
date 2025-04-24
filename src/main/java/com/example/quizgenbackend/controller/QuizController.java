package com.example.quizgenbackend.controller;

import com.example.quizgenbackend.QuizOutput;
import com.example.quizgenbackend.generator.QuizGenerator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CrossOrigin(origins = {
        "http://localhost:4200",
        "https://web-quiz-gen.vercel.app"
})
@RestController
@RequestMapping("/quiz")
public class QuizController {

    @PostMapping("/output")
    public ResponseEntity<String> outputQuiz(@RequestBody Map<String, String> body) {
        String input = body.get("input");
        if (input == null) return ResponseEntity.badRequest().body("Missing input");

        QuizOutput output = QuizGenerator.generateQuizFile(input);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(output.plainText);
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateQuiz(@RequestBody Map<String, String> body) {
        String input = body.get("input");
        if (input == null || input.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        System.out.println(input);
        QuizOutput output = QuizGenerator.generateQuizFile(input);

        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            ZipEntry docxEntry = new ZipEntry("quiz.docx");
            zipOut.putNextEntry(docxEntry);
            zipOut.write(output.docxBytes);
            zipOut.closeEntry();

            ZipEntry csvEntry = new ZipEntry("quiz.csv");
            zipOut.putNextEntry(csvEntry);
            zipOut.write(output.csvBytes);
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Error creating zip", e);
        }

        byte[] zipBytes = zipOutputStream.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz_files.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }
}
