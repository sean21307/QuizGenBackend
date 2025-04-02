package com.example.quizgenbackend.controller;

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

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/quiz")
public class QuizController {

    @GetMapping("/hi")
    public String test() {
        return "hi";
    }

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateQuiz(@RequestParam String input) {
        System.out.println("hi");
        // Generate in-memory DOCX and CSV files
        Map<String, byte[]> files = QuizGenerator.generateQuizFile(input);
        System.out.println(files.isEmpty() + " (empty)");

        // Create ZIP in memory
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipOut.putNextEntry(zipEntry);
                zipOut.write(entry.getValue());
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] zipBytes = zipOutputStream.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz_files.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);

    }
}
