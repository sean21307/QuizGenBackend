package com.example.quizgenbackend.generator;

import com.opencsv.CSVWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizGenerator {
    static final String QUESTION_PREFIX = "Question #";
    static final String QUESTION_TYPE_PREFIX = "QuestionType:";
    static final String CODE_SECTION = ":Code:";
    static final String END_CODE_SECTION = ":EndCode:";
    static final String TEXT_SECTION = ":Text:";
    static final String END_TEXT_SECTION = ":EndText:";
    static final String SOLUTION_PREFIX = "Solution:";
    static final String CHOICES_PREFIX = ":Choices:";
    static final String END_CHOICES_SECTION = ":EndChoices:";
    static final String SOLUTION_TYPE_PREFIX = "SolutionType:";
    static final String UNIT_PREFIX = "Unit:";
    static final String TITLE_PREFIX = "Title:";
    static Map<String, Object> variables = new HashMap<>();

    public static String formatToHtml(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        input = input.replace(" ", "&nbsp;");
        input = input.replace("\n", "<br>");
        return input;
    }

    // Generates a CSV file for questions
    public static void writeToCsv(String fileName, List<String[][]> questions) {
        File file = new File(fileName);
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            for (String[][] arr: questions) {
                for (String[] data: arr) {
                    writer.writeNext(data);
                }

                writer.writeNext(new String[]{});
            }

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Replaces variable symbols with the generated values (#R1# -> 2)
    public static String replaceVariables(String line) {
        int hash1 = line.indexOf("#");
        int hash2 = line.indexOf("#", hash1 + 1);

        while (hash1 != -1) {
            if (hash2 == -1) {
                System.out.println("Isolated hashtag in line: " + line);
                continue;
            }
            String findVar = line.substring(hash1 + 1, hash2);
            Object var = variables.get(findVar);

            if (var == null) {
                throw new IllegalArgumentException(findVar + " is not a defined variable");
            }

            line = line.replace("#" + findVar + "#", var.toString());

            hash1 = line.indexOf("#");
            hash2 = line.indexOf("#", hash1 + 1);
        }

        return line;
    }

    public static String generateQuizFile(String input) {
        String name = "GeneratedQuiz";
        String outputFileName = String.format("%s.docx", name).replace("Template", "Quiz");
        String csvFileName = String.format("%s.csv", name).replace("Template", "Quiz");

        List<String[][]> allQuestions = new ArrayList<>();

        try (Scanner file = new Scanner(input);
             XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputFileName)) {

            XWPFParagraph paragraph = document.createParagraph();


            String questionNumber = "";
            String executionCode = "";
            String questionType = "";
            String csvQuestionText = "";
            String title = "";

            boolean mustExecute = false;

            while (file.hasNext()) {
                String nextLine = file.nextLine();
                System.out.println(nextLine);
                if (nextLine.isEmpty() || nextLine.contains("##")) continue;

                if (nextLine.contains(QUESTION_PREFIX)) {
                    int hashIndex = nextLine.indexOf("#");
                    int colonIndex = nextLine.indexOf(":");
                    questionNumber = nextLine.substring(hashIndex + 1, colonIndex);
                    executionCode = "";
                    mustExecute = false;
                } else if (nextLine.contains(TITLE_PREFIX)) {
                    int colonIndex = nextLine.indexOf(":");
                    title = nextLine.substring(colonIndex + 1).trim();
                }

                System.out.println(nextLine);
                if (nextLine.startsWith("#")) {
                    createVariables(nextLine);
                } else if (nextLine.equals(CODE_SECTION)) {
                    mustExecute = true;
                    executionCode = readCodeSection(file);
                } else if (nextLine.equals(CHOICES_PREFIX)) {
                    System.out.println("Yeah choices");
                    mustExecute = true;
                    Map<String, Integer> choiceToPoints = getChoicesFromChoicesSection(file);

                    // Generate Java code
                    StringBuilder code = new StringBuilder();
                    code.append("public class DynamicCode {\n")
                            .append("    public static void main(String[] args) {\n")
                            .append("        String result = \"\";\n\n");

                    code.append("        String[] choices = new String[] {\n");
                    for (String choice : choiceToPoints.keySet()) {
                        code.append("            \"").append(choice).append("\",\n");
                    }
                    code.append("        };\n\n");
                    code.append("        System.out.println(\"Choices: \" + choices.length);\n")
                            .append("        for (String choice: choices) {\n")
                            .append("            System.out.println(choice);\n")
                            .append("        }\n\n");

                    code.append("        String[] points = new String[] {\n");
                    for (Integer point : choiceToPoints.values()) {
                        code.append("            \"").append(point).append("\",\n");
                    }
                    code.append("        };\n\n");

                    code.append("        System.out.println(\"Points: \");\n")
                        .append("        for (String point: points) {\n")
                        .append("            System.out.println(point);\n")
                        .append("        }\n\n")
                        .append("        System.out.println(result);\n")
                        .append("    }\n")
                        .append("}\n");

                    executionCode = String.valueOf(code);
                    System.out.println(executionCode);
                } else if (nextLine.equals(TEXT_SECTION)) {
                    csvQuestionText = readTextSection(file, paragraph, questionNumber);
                } else if (nextLine.contains(SOLUTION_PREFIX)) {
                    processSolution(file, nextLine, paragraph, mustExecute, executionCode, questionType, csvFileName, csvQuestionText, allQuestions, title);
                } else if (nextLine.contains(QUESTION_TYPE_PREFIX)) {
                    questionType = nextLine.substring(nextLine.indexOf(":") + 1).trim();
                }
            }

            document.write(out);
            writeToCsv(csvFileName, allQuestions);
            System.out.println(String.format("%s generated successfully", outputFileName));
            System.out.println(String.format("%s generated successfully", csvFileName));
            return name + ".docx" + "," + name + ".csv";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createVariables(String nextLine) {
        Random rand = new Random();
        String variableName = nextLine.substring(1, 3);
        int colonIndex = nextLine.indexOf(":");
        String sub = nextLine.substring(colonIndex + 1);

        if (sub.contains("{")) {
            ArrayList<String> setValues = new ArrayList<>(Arrays.asList(sub.substring(2, sub.length() - 1).split(",")));
            variables.put(variableName, setValues);
        } else if (sub.contains("from")) {
            int hashMark = sub.indexOf("#");
            int hashMark2 = sub.indexOf("#", hashMark + 1);
            String setNum = sub.substring(hashMark + 1, hashMark2);

            @SuppressWarnings("unchecked")
            ArrayList<String> setOptions = (ArrayList<String>) variables.get(setNum);
            String pickedValue = setOptions.get(rand.nextInt(setOptions.size()));
            variables.put(variableName, pickedValue);

        } else if (sub.contains("#")) {
            String[] variableInformation = sub.trim().split(",");
            String varName = variableInformation[0].trim();
            String method = variableInformation[1].trim();
            String value = variableInformation[2].trim();


            int hashMark = sub.indexOf("#");
            int hashMark2 = sub.indexOf("#", hashMark + 1);
            String var = sub.substring(hashMark + 1, hashMark2);
            Object referenceVariable = variables.get(var);

            if (method.equalsIgnoreCase("add")) {
                variables.put(variableName, (int) referenceVariable + Integer.parseInt(value));
            }

        } else {
            String[] variableInformation = sub.trim().split(",");
            String variableType = variableInformation[0].trim();
            String generationType = variableInformation[1].trim();

            if (variableType.equals("int") && generationType.equals("random")) {
                int min = Integer.parseInt(variableInformation[2].trim());
                int max = Integer.parseInt(variableInformation[3].trim());
                int generated = rand.nextInt(max + 1 - min) + min;
                variables.put(variableName, generated);
            } else if (variableType.equals("double") && generationType.equals("random")) {
                int min = Integer.parseInt(variableInformation[2].trim());
                int max = Integer.parseInt(variableInformation[3].trim());
                double generated = rand.nextInt(max + 1 - min) + min + (rand.nextInt(9) + 1) / 10.0;
                variables.put(variableName, generated);
            }
        }
    }

    public static Map<String, Integer> getChoicesFromChoicesSection(Scanner file) {
        Map<String, Integer> choicesMap = new LinkedHashMap<>(); // LinkedHashmap to preserve question order

        while (file.hasNext()) {
            String textLine = file.nextLine().trim();
            if (textLine.equals(END_CHOICES_SECTION)) {
                break;
            }

            int hashIndex = textLine.indexOf(",");
            if (hashIndex > 0) {
                try {
                    int points = Integer.parseInt(textLine.substring(0, hashIndex).trim());
                    String choiceText = textLine.substring(hashIndex + 1).replaceFirst("^,+", "");
                    choiceText = replaceVariables(choiceText);
                    choicesMap.put(choiceText.trim(), points);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid entry: " + textLine);
                }
            }
        }

        return choicesMap;
    }

    private static String readCodeSection(Scanner file) {
        StringBuilder executionCode = new StringBuilder();
        while (file.hasNext()) {
            String textLine = file.nextLine();
            if (textLine.equals(END_CODE_SECTION)) {
                break;
            }
            executionCode.append(replaceVariables(textLine)).append("\n");
        }
        return executionCode.toString();
    }

    private static String readTextSection(Scanner file, XWPFParagraph paragraph, String questionNumber) {
        System.out.println("Reading text section");
        boolean setQuestionText = false;
        String csvQuestionText = "";

        while (file.hasNext()) {
            String textLine = file.nextLine();
            if (textLine.equals(END_TEXT_SECTION)) {
                break;
            }

            textLine = replaceVariables(textLine);
            csvQuestionText += textLine + "\n";

            String[] parts = textLine.split("(?=<b>)|(?<=</b>)");

            if (!setQuestionText && !textLine.contains("Type: ")) {
                System.out.println("Setting type line: " + textLine);
                setQuestionText = true;
                XWPFRun run = paragraph.createRun();
                run.setText(questionNumber + ". " + textLine);
                run.addBreak();
            } else {
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if (true) {
                        XWPFRun run = paragraph.createRun();
                        if (part.startsWith("<b>") || part.endsWith("<b>")) {
                            part = part.replace("<b>", "").replace("</b>", "");
                            run.setBold(true);
                        } else {
                            run.setBold(false);
                        }
                        run.setText(part);

                        if (i == parts.length - 1) {
                            run.addBreak();
                        }
                    }
                }
            }
            System.out.println("Line: " + textLine);
        }

        return csvQuestionText;
    }


    public static Object extractValue(String str) {
        // Pattern for extracting a number
        Pattern numberPattern = Pattern.compile("\\[(\\d+)]");
        Matcher numberMatcher = numberPattern.matcher(str);

        if (numberMatcher.find()) {
            return Integer.parseInt(numberMatcher.group(1));
        }

        // Pattern for extracting a boolean
        Pattern booleanPattern = Pattern.compile("\\[(true|false)]");
        Matcher booleanMatcher = booleanPattern.matcher(str);

        if (booleanMatcher.find()) {
            return Boolean.parseBoolean(booleanMatcher.group(1));
        }

        // Pattern for extracting a custom string inside []
        Pattern customPattern = Pattern.compile("\\[(.*?)]");
        Matcher customMatcher = customPattern.matcher(str);

        if (customMatcher.find()) {
            return customMatcher.group(1);
        }

        return null;
    }

    private static void processSolution(Scanner file, String nextLine, XWPFParagraph paragraph, boolean mustExecute, String executionCode, String questionType, String csvFileName, String questionText, List<String[][]> allQuestions, String title) {
        questionText = formatToHtml(questionText);

        if (mustExecute) {
            System.out.println(executionCode);
            String[] solutionStringArray = Executor.compileAndExecute(executionCode).split("\n");

            if (questionType.equalsIgnoreCase("MC")) {
                String[] choices = new String[0];
                String[] points = new String[0];
                boolean readingPoints = false;
                int index = 0;

                for (String solString : solutionStringArray) {
                    if (solString.contains("Choices:")) {
                        int length = Integer.parseInt(solString.substring(solString.indexOf(":") + 1).trim());
                        choices = new String[length];
                        points = new String[length];
                        continue;
                    } else if (solString.contains("Points:")) {
                        readingPoints = true;
                        index = 0;
                        continue;
                    }

                    if (readingPoints) {
                        points[index] = solString;
                    } else {
                        choices[index] = solString;
                    }

                    index += 1;
                }

//                run.addCarriageReturn();
                for (int i = 0; i < choices.length; i++) {
                    String choiceString = choices[i].trim() + ": " + points[i].trim() + "%\n";
                    String[] parts = choiceString.split("(?=<b>)|(?<=</b>)");

                    for (int k = 0; k < parts.length; k++) {
                        String part = parts[k];
                        XWPFRun run = paragraph.createRun();

                        if (i == 0 && k == 0) {
                            run.addCarriageReturn();
                        }

                        if (part.startsWith("<b>") || part.endsWith("<b>")) {
                            part = part.replace("<b>", "").replace("</b>", "");
                            run.setBold(true);
                        } else {
                            run.setBold(false);
                        }
                        run.setText(part);

                        if (k == parts.length - 1) {
                            run.addCarriageReturn();
                        }

                        if (i == choices.length - 1) {
                            run.addBreak();
                            run.addBreak();
                        }
                    }


//                    run.setText(choiceString);
//                    run.addCarriageReturn();
                }

//                run.addBreak();
//                run.addBreak();


                // Generate CSV
                String[][] csvData = new String[5 + choices.length][];
                csvData[0] = new String[]{"NewQuestion", "MC"};
                csvData[1] = new String[]{"Title", title};
                csvData[2] = new String[]{"QuestionText", questionText, "html"};
                csvData[3] = new String[]{"Points", "1"};
                csvData[4] = new String[]{"Difficulty", "1"};
                for (int i = 0; i < choices.length; i++) {
                    csvData[5 + i] = new String[]{"Option", points[i].trim(), choices[i].trim(), "html"};
                }


                allQuestions.add(csvData);

            } else {
                // Generate CSV
                String[][] csvData = new String[6 + solutionStringArray.length][];
                csvData[0] = new String[]{"NewQuestion", "SA"};
                csvData[1] = new String[]{"Title", title};
                csvData[2] = new String[]{"QuestionText", questionText, "html"};
                csvData[3] = new String[]{"Points", "1"};
                csvData[4] = new String[]{"Difficulty", "1"};
                csvData[5] = new String[]{"InputBox", String.valueOf(solutionStringArray.length), "40"};
                for (int i = 0; i < solutionStringArray.length; i++) {
                    csvData[6 + i] = new String[]{"Answer", "100", String.valueOf(extractValue(solutionStringArray[i]))};
                }

                allQuestions.add(csvData);

//                run.addCarriageReturn();
                for (int i = 0; i < solutionStringArray.length; i++) {
                    String solString = solutionStringArray[i];
                    String[] parts = solString.split("(?=<b>)|(?<=</b>)");

                    for (int k = 0; k < parts.length; k++) {
                        String part = parts[k];
                        XWPFRun run = paragraph.createRun();

                        if (i == 0 && k == 0) {
                            run.addCarriageReturn();
                        }

                        if (part.startsWith("<b>") || part.endsWith("<b>")) {
                            part = part.replace("<b>", "").replace("</b>", "");
                            run.setBold(true);
                        } else {
                            run.setBold(false);
                        }
                        run.setText(part);

                        if (k == parts.length - 1) {
                            run.addCarriageReturn();
                        }

                        if (i == solutionStringArray.length - 1) {
                            run.addBreak();
                            run.addBreak();
                        }
                    }
//                    run.setText(solString);
//                    run.addCarriageReturn();
                }
//                run.addBreak();
//                run.addBreak();
            }
        } else {
            int colonIndex = nextLine.indexOf(":");
            String solutionString = nextLine.substring(colonIndex + 1).trim();
            solutionString = replaceVariables(solutionString);
            double result = EvaluateExpression.evaluateExpression(solutionString);
            String resultString = formatResult(file, result);
            List<String> units = readUnits(file);

            String solution = formatSolution(resultString, units);

            XWPFRun run = paragraph.createRun();
            run.setText(solution);
            run.addBreak();
            run.addBreak();


            // Generate CSV
            String str = solution.replaceAll("[\\[\\]]", "").trim();
            String[] entries = str.split("\\s*,\\s*");

            String[][] csvData = new String[6 + entries.length][];
            csvData[0] = new String[]{"NewQuestion", "SA"};
            csvData[1] = new String[]{"Title", title};
            csvData[2] = new String[]{"QuestionText", questionText, "html"};
            csvData[3] = new String[]{"Points", "1"};
            csvData[4] = new String[]{"Difficulty", "1"};
            csvData[5] = new String[]{"InputBox", String.valueOf(entries.length), "40"};
            for (int i = 0; i < entries.length; i++) {
                csvData[6 + i] = new String[]{"Answer", "100", String.valueOf(entries[i])};
            }

            allQuestions.add(csvData);

        }
    }

    private static String formatResult(Scanner file, double result) {
        String resultString;
        String type = "double";
        String nextLine = file.nextLine();
        if (nextLine.startsWith(SOLUTION_TYPE_PREFIX)) {
            type = nextLine.substring(nextLine.indexOf(":") + 1).trim();
        }

        switch (type) {
            case "long":
                resultString = String.valueOf((long) result);
                break;
            case "short":
                resultString = String.valueOf((short) result);
                break;
            case "int":
                resultString = String.valueOf((int) result);
                break;
            default:
                resultString = String.valueOf(new DecimalFormat("#.##").format(result));
                if (!resultString.contains(".")) {
                    resultString += ".0";
                }
                break;
        }
        return resultString;
    }

    private static List<String> readUnits(Scanner file) {
        List<String> units = new ArrayList<>();
        String nextLine = file.nextLine();
        if (nextLine.startsWith(UNIT_PREFIX)) {
            String unitString = nextLine.substring(nextLine.indexOf("{") + 1, nextLine.indexOf("}"));
            units = Arrays.asList(unitString.split(","));
            for (int i = 0; i < units.size(); i++) {
                units.set(i, units.get(i).trim());
            }
        }
        return units;
    }

    private static String formatSolution(String resultString, List<String> units) {
        StringBuilder solutionBuilder = new StringBuilder("[");
        for (String unit : units) {
            solutionBuilder.append(resultString).append(unit).append(", ");
        }
        solutionBuilder.setLength(solutionBuilder.length() - 2);  // Remove last comma and space
        solutionBuilder.append("]");
        return solutionBuilder.toString();
    }
}
