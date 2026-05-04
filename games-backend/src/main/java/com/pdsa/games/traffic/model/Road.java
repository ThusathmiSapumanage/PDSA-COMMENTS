package com.pdsa.games.traffic.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Roads")
public class Road {

    // Composite key: one directed road belongs to one traffic game session.
    @EmbeddedId
    private RoadId id;

    // Maximum number of cars/flow units that can pass through this road.
    @Column(name = "Capacity", nullable = false)
    private Integer capacity;

    public Road() {}

    public Road(RoadId id, Integer capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public RoadId getId() {
        return id;
    }

    public void setId(RoadId id) {
        this.id = id;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
