package com.s13sh.Jnana.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.s13sh.Jnana.service.LearnerService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/learner")
public class LearnerController {

	@Autowired
	LearnerService learnerService;

	@GetMapping("/home")
	public String loadHome(HttpSession session) {
		return learnerService.loadHome(session);
	}

	@GetMapping("/view-courses")
	public String loadCourses(HttpSession session, Model model) {
		return learnerService.viewCourses(session, model);
	}

	@GetMapping("/enroll/{id}")
	public String enrollCourse(HttpSession session, @PathVariable Long id, Model model) {
		return learnerService.enrollCourse(session, id, model);
	}

	@GetMapping("/enrolled-courses")
	public String viewEnrolledCourses(HttpSession session, Model model) {
		return learnerService.viewEnrolledCourses(session, model);
	}

	@PostMapping("/enroll-paidcourse/{id}")
	public String enrollPaidCourse(HttpSession session, @PathVariable Long id, Model model) {
		return learnerService.enrollPaidCourse(session, id, model);
	}
	
	@GetMapping("/view-enrolled-sections/{id}")
	public String viewEnrolledSections(HttpSession session, @PathVariable Long id, Model model) {
		return learnerService.viewEnrolledSections(session, id, model);
	}

	@GetMapping("/view-video/{id}")
	public String viewVideo(HttpSession session, @PathVariable Long id, Model model) {
		return learnerService.viewVideo(session, id, model);
	}

	@GetMapping("/section/quiz/{id}")
	public String loadSectionQuiz(@PathVariable Long id, HttpSession session, Model model) {
		return learnerService.loadSectionQuiz(id, session, model);
	}

	@PostMapping("/section/quiz/{id}")
	public String sectionQuizSubmit(@PathVariable Long id, HttpSession session,
			@RequestParam Map<String, String> quiz) {
		return learnerService.submitQuiz(id, session, quiz);
	}

	@GetMapping("/course/quiz/{id}")
	public String takeQuiz(@PathVariable Long id, HttpSession session, Model model) {
		return learnerService.loadCourseQuiz(id, session, model);
	}

	@PostMapping("/course/quiz/{id}")
	public String submitCourseQuiz(@PathVariable Long id, HttpSession session,
			@RequestParam Map<String, String> quiz) {
		return learnerService.submitCourseQuiz(id, session, quiz);
	}

	@GetMapping("/certificate/{id}")
	public String viewCertificate(@PathVariable Long id, HttpSession session, Model model) {
		return learnerService.viewCertificate(id, session, model);
	}
}
