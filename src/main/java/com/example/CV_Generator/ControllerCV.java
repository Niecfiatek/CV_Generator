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
            @RequestParam String experience,
            @RequestParam String education,
            @RequestParam String skills,
            @RequestParam String interests,
            Model model
    ) {
        String[] experienceLines = experience.split("\\r?\\n");
        List<Map<String, String>> parsedExperiences = new ArrayList<>();

        for (String line : experienceLines) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                Map<String, String> exp = new HashMap<>();
                exp.put("years", parts[0].trim());
                exp.put("company", parts[1].trim());
                exp.put("position", parts[2].trim());
                parsedExperiences.add(exp);
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
        model.addAttribute("experienceList", parsedExperiences);
        model.addAttribute("education", education);
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
