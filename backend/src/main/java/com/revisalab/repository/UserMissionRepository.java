package com.revisalab.repository;

import com.revisalab.model.UserMission;
import com.revisalab.model.User;
import com.revisalab.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    List<UserMission> findByUser(User user);
    List<UserMission> findByMission(Mission mission);
    Optional<UserMission> findByUserAndMission(User user, Mission mission);
    
    @Query("SELECT um FROM UserMission um WHERE um.user = :user AND um.completed = true")
    List<UserMission> findCompletedMissionsByUser(@Param("user") User user);
    
    @Query("SELECT um FROM UserMission um WHERE um.user = :user AND um.completed = false")
    List<UserMission> findInProgressMissionsByUser(@Param("user") User user);
    
    @Query("SELECT um FROM UserMission um WHERE um.user = :user AND um.rewardClaimed = false AND um.completed = true")
    List<UserMission> findReadyToClaimRewardMissions(@Param("user") User user);
    
    @Query("SELECT COUNT(um) FROM UserMission um WHERE um.user = :user AND um.completed = true")
    Long countCompletedMissionsByUser(@Param("user") User user);
    
    @Query("SELECT COALESCE(SUM(m.xpReward), 0) FROM UserMission um JOIN um.mission m WHERE um.user = :user AND um.completed = true AND um.rewardClaimed = true")
    Integer getTotalXpRewardsClaimed(@Param("user") User user);
}
