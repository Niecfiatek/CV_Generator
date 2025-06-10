package com.example.CV_Generator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

@Controller
public class ControllerCV {

    @GetMapping("/")
    public String cvForm() {
        return "cv_form";
    }

    @PostMapping("/submit")
    public String submitCv(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String adres,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String linkedin,
            @RequestParam String experience,
            @RequestParam String education,
            @RequestParam String languages,
            @RequestParam String skills,
            @RequestParam String interests,
            Model model
    ) {
        String[] experienceLines = experience.split("\\r?\\n");
        List<Map<String, String>> parsedExperiences = new ArrayList<>();

        for (String line : experienceLines) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                String years = parts[0].trim();
                String company = parts[1].trim();
                String position = parts[2].trim();

                String jobDescPrompt = String.format(
                        "Na podstawie poniższych danych wygeneruj jedno zdaniowy krótki opis obowiązków zawodowych w tej roli:\n\n" +
                                "Okres: %s\nFirma: %s\nStanowisko: %s\n\n" +
                                "Opis w języku polskim, konkretny, wymień po przecinku, maksymalnie 180 znaków.",
                        years, company, position
                );

                String jobDescription = askOllama(jobDescPrompt);

                Map<String, String> exp = new HashMap<>();
                exp.put("years", years);
                exp.put("company", company);
                exp.put("position", position);
                exp.put("description", jobDescription);

                parsedExperiences.add(exp);
            }
        }

        List<String> parsedLanguages = new ArrayList<>();
        String[] languageLines = languages.split("\\r?\\n");
        for (String line : languageLines) {
            parsedLanguages.add(line.trim());
        }

        String[] educationLines = education.split("\\r?\\n");
        List<Map<String, String>> parsedEducation = new ArrayList<>();

        for (String line : educationLines) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                Map<String, String> edu = new HashMap<>();
                edu.put("years", parts[0].trim());
                edu.put("degree", parts[1].trim());
                edu.put("institution", parts[2].trim());
                parsedEducation.add(edu);
            }
        }

        String prompt = String.format(
                "Na podstawie poniższych informacji wygeneruj krótki opis profilu zawodowego (2-3 zdania) do CV. " +
                        "Opis ma być konkretny i zawierać te dane w treści. Bez pustych frazesów i pustych określeń typu 'ambitny', 'zaangażowany', 'dynamiczne środowisko' itp.\n\n" +
                        "Imię: %s\n" +
                        "Doświadczenie zawodowe: %s\n" +
                        "Wykształcenie: %s\n" +
                        "Umiejętności: %s\n" +
                        "Zainteresowania: %s\n\n" +
                        "Zwróć tylko gotowy opis bez nagłówków w języku polskim.",
                name, experience, education, skills, interests);

        String profileDescription = askOllama(prompt);

        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
        model.addAttribute("adres", adres);
        model.addAttribute("website", website != null ? website : "");
        model.addAttribute("linkedin", linkedin != null ? linkedin : "");
        model.addAttribute("experienceList", parsedExperiences);
        model.addAttribute("educationList", parsedEducation);
        model.addAttribute("languagesList", parsedLanguages);
        model.addAttribute("skills", skills);
        model.addAttribute("profile", profileDescription);
        model.addAttribute("interests", interests);

        return "cv";
    }

    private String askOllama(String prompt) {
        try {
            URL url = new URL("http://localhost:11434/api/generate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            json.put("model", "llama3");
            json.put("prompt", prompt);
            json.put("stream", false);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.toString().getBytes("utf-8"));
            }

            StringBuilder fullResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("\"response\"")) {
                        JSONObject responseLine = new JSONObject(line);
                        fullResponse.append(responseLine.getString("response"));
                    }
                }
            }

            return fullResponse.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Nie udało się wygenerować profilu.";
        }
    }
}
