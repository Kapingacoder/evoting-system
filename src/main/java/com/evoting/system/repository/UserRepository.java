package com.evoting.system.repository;

import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    long countByRole(Role role);

    List<User> findAllByRole(Role role);
    
    List<User> findByRoleOrderByIdDesc(Role role, Pageable pageable);
    
    boolean existsByUsername(String username);
    
    boolean existsByAdmissionNumber(String admissionNumber);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByAdmissionNumber(String admissionNumber);
    
    long countByRoleAndVotedTrue(Role role);
    
    @Query("SELECT u FROM User u WHERE TRIM(LOWER(u.admissionNumber)) = TRIM(LOWER(:admissionNumber))")
    Optional<User> findByAdmissionNumberIgnoreCaseAndTrim(@Param("admissionNumber") String admissionNumber);
}