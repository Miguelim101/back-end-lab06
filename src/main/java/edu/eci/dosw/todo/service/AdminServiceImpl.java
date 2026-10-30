package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public AdminServiceImpl(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        // EnumMap: siempre aparecen los tres estados, en orden, aunque alguno tenga 0 tareas
        Map<TaskStatus, Long> tasksByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, taskRepository.countByStatus(status));
        }

        return new AdminSummaryResponse(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN),
                taskRepository.count(),
                tasksByStatus);
    }
}
