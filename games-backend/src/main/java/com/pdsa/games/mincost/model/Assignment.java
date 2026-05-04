package com.pdsa.games.mincost.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One task-to-employee assignment in the cost matrix.
 * task = row index (0-based), employee = column index (0-based).
 */
public class Assignment {

    private final int task;
    private final int employee;
    private final int cost;

    public Assignment(int task, int employee, int cost) {
        this.task = task;
        this.employee = employee;
        this.cost = cost;
    }

    @JsonProperty("task")
    public int getTask() {
        return task;
    }

    @JsonProperty("employee")
    public int getEmployee() {
        return employee;
    }

    @JsonProperty("cost")
    public int getCost() {
        return cost;
    }
}
