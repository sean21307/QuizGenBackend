package com.example.quizgenbackend;

public class QuizOutput {
    public final String plainText;
    public final byte[] docxBytes;
    public final byte[] csvBytes;

    public QuizOutput(String plainText, byte[] docxBytes, byte[] csvBytes) {
        this.plainText = plainText;
        this.docxBytes = docxBytes;
        this.csvBytes = csvBytes;
    }
}
