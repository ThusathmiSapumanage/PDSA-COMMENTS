package com.pdsa.games.traffic.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Traffic_Sim_Game")
public class TrafficSimGame {

    // Uses the common Game_Session id as the primary key for this traffic game row.
    @Id
    @Column(name = "Session_Id")
    private Integer sessionId;

    // Correct maximum-flow answer calculated when the network is generated.
    @Column(name = "Max_Flow_Value", nullable = false)
    private Integer maxFlowValue;

    public TrafficSimGame() {}

    public TrafficSimGame(Integer sessionId, Integer maxFlowValue) {
        this.sessionId = sessionId;
        this.maxFlowValue = maxFlowValue;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getMaxFlowValue() {
        return maxFlowValue;
    }

    public void setMaxFlowValue(Integer maxFlowValue) {
        this.maxFlowValue = maxFlowValue;
    }
}
