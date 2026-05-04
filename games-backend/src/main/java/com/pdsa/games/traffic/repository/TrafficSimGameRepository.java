package com.pdsa.games.traffic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pdsa.games.traffic.model.TrafficSimGame;

// Provides CRUD access to the saved traffic game result for each session.
public interface TrafficSimGameRepository extends JpaRepository<TrafficSimGame, Integer> {
}
