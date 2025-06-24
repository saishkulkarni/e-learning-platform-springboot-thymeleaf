package com.s13sh.Jnana.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.s13sh.Jnana.model.Certificate;
import com.s13sh.Jnana.model.Course;
import com.s13sh.Jnana.model.Learner;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Certificate findByLearnerAndCourse(Learner attribute, Course course);
    
}
