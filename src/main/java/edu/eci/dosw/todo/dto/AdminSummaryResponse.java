package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskStatus;

import java.util.Map;
import java.util.Objects;

/**
 * ==========================================================================
 * == LAB 7: Resumen administrativo NO sensible. Solo conteos agregados:   ==
 * ==        no incluye correos, nombres, títulos ni contenido de tareas   ==
 * ==========================================================================
 */

/**
 * totalUsers
 * totalAdmins
 * totalTasks
 * tasksByStatus
 */
public class AdminSummaryResponse {

    private long totalUsers;
    private long totalAdmins;
    private long totalTasks;
    private Map<TaskStatus, Long> tasksByStatus;

    // Constructors
    public AdminSummaryResponse (long totalUsers, long totalAdmins, long totalTasks, Map<TaskStatus, Long> tasksByStatus){
        this.totalUsers = totalUsers;
        this.totalAdmins = totalAdmins;
        this.totalTasks = totalTasks;
        this.tasksByStatus = tasksByStatus;
    }

    public long getTotalUsers() { return this.totalUsers; }
    public long getTotalAdmins() { return this.totalAdmins; }
    public long getTotalTasks() { return this.totalTasks; }
    public Map<TaskStatus, Long> getTasksByStatus() { return this.tasksByStatus; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminSummaryResponse that = (AdminSummaryResponse) o;

        return totalUsers == that.totalUsers &&
                totalAdmins == that.totalAdmins &&
                totalTasks == that.totalTasks &&
                Objects.equals(tasksByStatus, that.tasksByStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalUsers, totalAdmins, totalTasks, tasksByStatus);
    }
}
