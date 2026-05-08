package com.pdsa.games.common.response;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseModel, Integer> {
    /**
     * Find all responses for a given game session.
     *
     * @param sessionId identifier of the game session
     * @return list of responses associated with the session
     */
    List<ResponseModel> findBySessionId(Integer sessionId);

    /**
     * Find all responses for a given game session while acquiring a pessimistic write lock.
     *
     * @param sessionId identifier of the game session
     * @return list of responses associated with the session with lock protection
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ResponseModel> findWithLockBySessionId(Integer sessionId);
}
