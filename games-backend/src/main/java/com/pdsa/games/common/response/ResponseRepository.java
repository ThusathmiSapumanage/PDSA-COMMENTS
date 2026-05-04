package com.pdsa.games.common.response;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseModel, Integer> {
    List<ResponseModel> findBySessionId(Integer sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ResponseModel> findWithLockBySessionId(Integer sessionId);
}
