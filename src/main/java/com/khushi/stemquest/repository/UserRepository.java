package com.khushi.stemquest.repository;

import com.khushi.stemquest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentCode(String studentCode);
    Optional<User> findByStudentCodeIgnoreCase(String studentCode);
    boolean existsByStudentCode(String studentCode);
    List<User> findByRole(String role);
    List<User> findByRoleOrderByXpDesc(String role);
    List<User> findByRoleAndGradeOrderByXpDesc(String role, Integer grade);
    List<User> findTop10ByRoleOrderByXpDesc(String role);
    List<User> findTop10ByRoleAndGradeOrderByXpDesc(String role, Integer grade);
}
