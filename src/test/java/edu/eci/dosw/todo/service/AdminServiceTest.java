package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.repository.TaskRepository;
import edu.eci.dosw.todo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void summary_shouldReturnAggregatedCounts() {
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(taskRepository.count()).thenReturn(6L);
        when(taskRepository.countByStatus(TaskStatus.PENDING)).thenReturn(3L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(taskRepository.countByStatus(TaskStatus.COMPLETED)).thenReturn(1L);

        AdminSummaryResponse summary = adminService.summary();

        assertThat(summary.getTotalUsers()).isEqualTo(3);
        assertThat(summary.getTotalAdmins()).isEqualTo(1);
        assertThat(summary.getTotalTasks()).isEqualTo(6);
        assertThat(summary.getTasksByStatus())
                .containsEntry(TaskStatus.PENDING, 3L)
                .containsEntry(TaskStatus.IN_PROGRESS, 2L)
                .containsEntry(TaskStatus.COMPLETED, 1L);
    }

    @Test
    void summary_shouldIncludeEveryStatusEvenWithoutTasks() {
        AdminSummaryResponse summary = adminService.summary(); // los mocks devuelven 0

        assertThat(summary.getTasksByStatus()).containsOnlyKeys(TaskStatus.values());
        assertThat(summary.getTasksByStatus().values()).containsOnly(0L);
    }

    @Test
    void summary_shouldNotReadIndividualTasksOrUsers() {
        adminService.summary();

        // Solo conteos: nunca se cargan filas con datos privados
        verify(taskRepository, never()).findAll();
        verify(taskRepository, never()).findAllByOwnerId(anyLong());
        verify(userRepository, never()).findAll();
        verify(userRepository, never()).findByEmail(any());
    }
}
