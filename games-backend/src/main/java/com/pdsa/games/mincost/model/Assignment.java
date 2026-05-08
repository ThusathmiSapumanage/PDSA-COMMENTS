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

    /**
     * Creates a new assignment mapping a task to an employee with a cost.
     *
     * @param task the task index
     * @param employee the employee index
     * @param cost the cost for this assignment
     */
    public Assignment(int task, int employee, int cost) {
        this.task = task;
        this.employee = employee;
        this.cost = cost;
    }

    /**
     * Returns the task index for this assignment.
     *
     * @return task index (0-based)
     */
    @JsonProperty("task")
    public int getTask() {
        return task;
    }

    /**
     * Returns the employee index for this assignment.
     *
     * @return employee index (0-based)
     */
    @JsonProperty("employee")
    public int getEmployee() {
        return employee;
    }

    /**
     * Returns the cost associated with this assignment.
     *
     * @return assignment cost
     */
    @JsonProperty("cost")
    public int getCost() {
        return cost;
    }
}
