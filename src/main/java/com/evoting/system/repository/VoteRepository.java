package com.evoting.system.repository;

import com.evoting.system.model.Vote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findAllByOrderByTimestampDesc(Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM Vote v WHERE v.candidate.id = :candidateId")
    void deleteByCandidateId(@Param("candidateId") Long candidateId);
    
    @Modifying
    @Query("DELETE FROM Vote v WHERE v.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}