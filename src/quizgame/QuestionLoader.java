package quizgame;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Minimal JSON parser για φόρτωση ερωτήσεων.
 * Δεν χρειάζεται εξωτερικές βιβλιοθήκες (π.χ. Gson).
 */
public class QuestionLoader {

	public static List<Question> loadFromFile(String filePath) throws IOException {
	    InputStream is = QuestionLoader.class
	            .getClassLoader()
	            .getResourceAsStream(filePath);
	    if (is == null) {
	        throw new FileNotFoundException(
	                "Αδυνατη φορτωση resource: " + filePath
	        );
	    }
	    String content = new String(
	            is.readAllBytes(),
	            java.nio.charset.StandardCharsets.UTF_8
	    );
	    return parseQuestions(content);
	}
	
    private static List<Question> parseQuestions(String json) {
        List<Question> questions = new ArrayList<>();
        // Βρίσκουμε κάθε object { ... } μέσα στο array
        List<String> objects = extractObjects(json);

        for (String obj : objects) {
            String questionText = extractStringValue(obj, "question");
            int correctIndex   = Integer.parseInt(extractStringValue(obj, "correctIndex"));
            String[] options   = extractArray(obj, "options");

            if (questionText != null && options != null) {
                questions.add(new Question(questionText, options, correctIndex));
            }
        }
        return questions;
    }

    /** Εξάγει τα { } blocks του top-level array */
    private static List<String> extractObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    /** Εξάγει string ή αριθμό για ένα key */
    private static String extractStringValue(String obj, String key) {
        String pattern = "\"" + key + "\"";
        int idx = obj.indexOf(pattern);
        if (idx == -1) return null;
        int colon = obj.indexOf(':', idx + pattern.length());
        if (colon == -1) return null;

        // Παράκαμψη κενών
        int valueStart = colon + 1;
        while (valueStart < obj.length() && Character.isWhitespace(obj.charAt(valueStart))) valueStart++;

        if (obj.charAt(valueStart) == '"') {
            // String value
            int end = obj.indexOf('"', valueStart + 1);
            return obj.substring(valueStart + 1, end);
        } else {
            // Αριθμός
            int end = valueStart;
            while (end < obj.length() && (Character.isDigit(obj.charAt(end)) || obj.charAt(end) == '-')) end++;
            return obj.substring(valueStart, end);
        }
    }

    /** Εξάγει string array για ένα key */
    private static String[] extractArray(String obj, String key) {
        String pattern = "\"" + key + "\"";
        int idx = obj.indexOf(pattern);
        if (idx == -1) return null;
        int arrStart = obj.indexOf('[', idx);
        int arrEnd   = obj.indexOf(']', arrStart);
        if (arrStart == -1 || arrEnd == -1) return null;

        String arrContent = obj.substring(arrStart + 1, arrEnd);
        List<String> items = new ArrayList<>();
        int i = 0;
        while (i < arrContent.length()) {
            int q1 = arrContent.indexOf('"', i);
            if (q1 == -1) break;
            int q2 = arrContent.indexOf('"', q1 + 1);
            if (q2 == -1) break;
            items.add(arrContent.substring(q1 + 1, q2));
            i = q2 + 1;
        }
        return items.toArray(new String[0]);
    }
}
