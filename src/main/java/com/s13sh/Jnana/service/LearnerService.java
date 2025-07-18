package com.s13sh.Jnana.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.cloudinary.Cloudinary;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.s13sh.Jnana.model.Certificate;
import com.s13sh.Jnana.model.Course;
import com.s13sh.Jnana.model.EnrolledCourse;
import com.s13sh.Jnana.model.EnrolledSection;
import com.s13sh.Jnana.model.Learner;
import com.s13sh.Jnana.model.QuizQuestion;
import com.s13sh.Jnana.model.Section;
import com.s13sh.Jnana.repository.CertificateRepository;
import com.s13sh.Jnana.repository.CourseRepository;
import com.s13sh.Jnana.repository.EnrolledCourseRepository;
import com.s13sh.Jnana.repository.EnrolledSectionRepository;
import com.s13sh.Jnana.repository.LearnerRepository;
import com.s13sh.Jnana.repository.QuizQuestionRepository;
import com.s13sh.Jnana.repository.SectionRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class LearnerService {

    private final PasswordEncoder encoder;

    private final Cloudinary cloudinary;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    LearnerRepository learnerRepository;

    @Autowired
    CertificateRepository certificateRepository;

    @Autowired
    ChatClient chatClient;

    @Autowired
    QuizQuestionRepository questionRepository;

    @Autowired
    EnrolledSectionRepository enrolledSectionRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    EnrolledCourseRepository enrolledCourseRepository;

    @Value("${razor-pay.api.key}")
    String key;
    @Value("${razor-pay.api.secret}")
    String secret;

    LearnerService(Cloudinary cloudinary, PasswordEncoder encoder) {
        this.cloudinary = cloudinary;
        this.encoder = encoder;
    }

    public String loadHome(HttpSession session) {
        if (session.getAttribute("learner") != null) {
            return "leaner-home.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewCourses(HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            List<Course> courses = courseRepository.findByPublishedTrue();
            if (courses.isEmpty()) {
                session.setAttribute("fail", "No Courses Available");
                return "redirect:/learner/home";
            } else {
                model.addAttribute("courses", courses);
                return "avaiable-courses.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String enrollCourse(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {
            Learner learner = (Learner) session.getAttribute("learner");
            Course course = courseRepository.findById(id).get();

            if (course.isPaid()) {
                double amount = 199;
                try {
                    RazorpayClient client = new RazorpayClient(key, secret);

                    JSONObject object = new JSONObject();
                    object.put("amount", amount * 100);
                    object.put("currency", "INR");

                    Order order = client.orders.create(object);
                    String orderId = order.get("id");

                    model.addAttribute("orderId", orderId);
                    model.addAttribute("course", course);
                    model.addAttribute("amount", amount * 100);
                    model.addAttribute("currency", "INR");
                    model.addAttribute("leaner", learner);
                    model.addAttribute("key", key);
                    model.addAttribute("path", "/learner/enroll-paidcourse/" + course.getId());

                    return "payment.html";

                } catch (RazorpayException e) {
                    e.printStackTrace();
                    session.setAttribute("fail", "Something Went Wrong");
                    return "redirect:/learner/home";
                }

            } else {

                List<Section> sections = sectionRepository.findByCourse(course);
                List<EnrolledSection> enrolledSections = new ArrayList<EnrolledSection>();
                for (Section section : sections) {
                    EnrolledSection enrolledSection = new EnrolledSection();
                    enrolledSection.setSection(section);
                    enrolledSections.add(enrolledSection);
                }

                EnrolledCourse enrolledCourse = new EnrolledCourse();
                enrolledCourse.setCourse(course);
                enrolledCourse.setEnrolledSections(enrolledSections);

                learner.getEnrolledCourses().add(enrolledCourse);

                learnerRepository.save(learner);

                session.setAttribute("pass", "Courses Enrolled Success, Thanks " + learner.getName());
                session.setAttribute("learner", learnerRepository.findById(learner.getId()).get());
                return "redirect:/learner/home";
            }

        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewEnrolledCourses(HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            Learner learner = (Learner) session.getAttribute("learner");

            List<EnrolledCourse> enrolledCourses = learner.getEnrolledCourses();
            if (enrolledCourses.isEmpty()) {
                session.setAttribute("fail", "Not Enrolled for Any of the Courses");
                return "redirect:/learner/home";
            } else {
                model.addAttribute("enrolledCourses", enrolledCourses);
                return "view-enrolled-courses.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewEnrolledSections(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {
            EnrolledCourse enrolledCourse = enrolledCourseRepository.findById(id).get();
            List<EnrolledSection> enrolledSections = enrolledCourse.getEnrolledSections();

            model.addAttribute("enrolledSections", enrolledSections);
            return "view-enrolled-sections.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewVideo(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {

            EnrolledSection section = enrolledSectionRepository.findById(id).get();
            section.setSectionCompleted(true);

            enrolledSectionRepository.save(section);

            String videoUrl = section.getSection().getVideoUrl();
            model.addAttribute("link", videoUrl);
            EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section);
            model.addAttribute("id", course.getId());
            return "play-video.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String loadSectionQuiz(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {

            EnrolledSection section = enrolledSectionRepository.findById(id).get();

            if (!section.isSectionCompleted()) {
                EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section);
                session.setAttribute("fail", "First Complete the Section to Take Quiz");
                return "redirect:/learner/view-enrolled-sections/" + course.getId();
            }
            List<QuizQuestion> questions = section.getSection().getQuizQuestions();
            model.addAttribute("questions", questions);
            model.addAttribute("section", section);
            model.addAttribute("id", id);

            return "section-quiz.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String submitQuiz(Long id, HttpSession session, Map<String, String> quiz) {
        if (session.getAttribute("learner") != null) {
            EnrolledSection section = enrolledSectionRepository.findById(id).get();
            String prompt = "";
            for (String questionId : quiz.keySet()) {
                String question = questionRepository.findById(Long.parseLong(questionId)).get().getQuestion();
                String answer = quiz.get(questionId);
                prompt += ". question: " + question + ". answer: " + answer;
            }
            prompt += "Evaluate the following quiz strictly. For each question, consider the given answer against the correct answer. Calculate and return ONLY a numerical score from 0-100 based on the accuracy of responses. Return just the number without any additional text.\n\n";
            String answer = chatClient.prompt(prompt).call().content();
            int score = Integer.parseInt(answer);
            if (score >= 75) {
                section.setSectionQuizCompleted(true);
                enrolledSectionRepository.save(section);
                session.setAttribute("pass", "Quiz Cleared Success, Congratulations!");
            } else {
                session.setAttribute("fail", "You failed to clear the quiz, as you got " + score + " marks. Try again");
            }
            EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section);
            session.setAttribute("learner", learnerRepository.findById(((Learner) session.getAttribute("learner")).getId()).get());
            return "redirect:/learner/view-enrolled-sections/" + course.getId();
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String enrollPaidCourse(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {
            Learner learner = (Learner) session.getAttribute("learner");
            Course course = courseRepository.findById(id).get();
            List<Section> sections = sectionRepository.findByCourse(course);
            List<EnrolledSection> enrolledSections = new ArrayList<EnrolledSection>();
            for (Section section : sections) {
                EnrolledSection enrolledSection = new EnrolledSection();
                enrolledSection.setSection(section);
                enrolledSections.add(enrolledSection);
            }

            EnrolledCourse enrolledCourse = new EnrolledCourse();
            enrolledCourse.setCourse(course);
            enrolledCourse.setEnrolledSections(enrolledSections);

            learner.getEnrolledCourses().add(enrolledCourse);

            learnerRepository.save(learner);

            session.setAttribute("pass", "Courses Enrolled Success, Thanks " + learner.getName());
            session.setAttribute("learner", learnerRepository.findById(learner.getId()).get());
            return "redirect:/learner/home";

        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String loadCourseQuiz(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            EnrolledCourse enrolledCourse = enrolledCourseRepository.findById(id).get();

            boolean completed = true;

            for (EnrolledSection section : enrolledCourse.getEnrolledSections()) {
                if (!section.isSectionQuizCompleted()) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                enrolledCourse.setCourseCompleted(true);
                enrolledCourseRepository.save(enrolledCourse);
            }

            if (!enrolledCourse.isCourseCompleted()) {
                session.setAttribute("fail", "First Complete all Sections to Take Quiz");
                return "redirect:/learner/view-enrolled-sections/" + enrolledCourse.getId();
            } else {
                List<QuizQuestion> questions = enrolledCourse.getCourse().getQuizQuestions();
                if (questions.isEmpty()) {
                    session.setAttribute("fail", "No Quiz Available for this Course");
                    return "redirect:/learner/view-enrolled-courses";
                }
                model.addAttribute("questions", questions);
                model.addAttribute("id", id);
                model.addAttribute("course", enrolledCourse.getCourse());
                return "course-quiz.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String submitCourseQuiz(Long id, HttpSession session, Map<String, String> quiz) {
        if (session.getAttribute("learner") != null) {
            EnrolledCourse enrolledCourse = enrolledCourseRepository.findById(id).get();
            String prompt = "";
            for (String questionId : quiz.keySet()) {
                String question = questionRepository.findById(Long.parseLong(questionId)).get().getQuestion();
                String answer = quiz.get(questionId);
                prompt += ". question: " + question + ". answer: " + answer;
            }
                prompt += "Evaluate the following quiz strictly. For each question, consider the given answer against the correct answer. Calculate and return ONLY a numerical score from 0-100 based on the accuracy of responses. Return just the number without any additional text.\n\n";
                String answer = chatClient.prompt(prompt).call().content();         
            int score = Integer.parseInt(answer);
            if (score >= 80) {
                enrolledCourse.setFinalQuizCompleted(true);
                enrolledCourse.setCompletionDate(LocalDate.now());
                enrolledCourseRepository.save(enrolledCourse);
                session.setAttribute("pass", "Quiz Cleared Success, Congratulations!");
                Certificate certificate = new Certificate();
                certificate.setLearner((Learner) session.getAttribute("learner"));
                certificate.setCourse(enrolledCourse.getCourse());
                certificateRepository.save(certificate);
            } else {
                session.setAttribute("fail", "You failed to clear the quiz, as you got " + score + " marks. Try again");
            }
            session.setAttribute("learner", learnerRepository.findById(((Learner) session.getAttribute("learner")).getId()).get());
            return "redirect:/learner/enrolled-courses";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewCertificate(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            EnrolledCourse enrolledCourse = enrolledCourseRepository.findById(id).get();
            if (!enrolledCourse.isFinalQuizCompleted()) {
                session.setAttribute("fail", "First Complete the Final Quiz to View Certificate");
                return "redirect:/learner/enrolled-courses";
            } else {
                Certificate certificate = certificateRepository.findByLearnerAndCourse((Learner) session.getAttribute("learner"), enrolledCourse.getCourse());
                model.addAttribute("certificate", certificate);
                return "view-certificate.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }

    }
}
