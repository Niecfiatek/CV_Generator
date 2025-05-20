package com.example.CV_Generator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControllerCV {

    @RequestMapping("/")
    public String cvForm() {
        return "cv";
    }

    @PostMapping("/submit")
    public String submitCv(@RequestParam String name, @RequestParam String email,
                           @RequestParam String phone, @RequestParam String experience,
                           @RequestParam String education, @RequestParam String skills,
                           RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "CV zapisane!");
        return "redirect:/";
    }
}
