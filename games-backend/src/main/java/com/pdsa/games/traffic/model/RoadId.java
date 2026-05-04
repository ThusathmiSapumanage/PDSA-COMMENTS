package com.pdsa.games.traffic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoadId implements Serializable {

    // Session that owns this generated road.
    @Column(name = "Session_Id")
    private Integer sessionId;

    // Start node of the directed road, such as A or B.
    @Column(name = "Start_Node", length = 1)
    private String startNode;

    // End node of the directed road, such as G or T.
    @Column(name = "End_Node", length = 1)
    private String endNode;

    public RoadId() {}

    public RoadId(Integer sessionId, String startNode, String endNode) {
        this.sessionId = sessionId;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getStartNode() {
        return startNode;
    }

    public void setStartNode(String startNode) {
        this.startNode = startNode;
    }

    public String getEndNode() {
        return endNode;
    }

    public void setEndNode(String endNode) {
        this.endNode = endNode;
    }

    @Override
    public boolean equals(Object o) {
        // JPA composite keys need value-based equality for reliable repository lookup.
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoadId roadId = (RoadId) o;
        return Objects.equals(sessionId, roadId.sessionId) &&
               Objects.equals(startNode, roadId.startNode) &&
               Objects.equals(endNode, roadId.endNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, startNode, endNode);
    }
}
