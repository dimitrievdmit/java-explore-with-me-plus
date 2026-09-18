package ru.practicum.explorewithme.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.user.dal.UserRepository;
import ru.practicum.explorewithme.user.dto.NewUserRequest;
import ru.practicum.explorewithme.user.dto.UserDto;
import ru.practicum.explorewithme.user.model.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private NewUserRequest newUserRequest;
    private User user;

    @BeforeEach
    void setUp() {
        newUserRequest = new NewUserRequest("user@example.com", "Иван Иванов");
        user = new User(1L, "user@example.com", "Иван Иванов");
    }

    @Test
    void registerUser_ShouldSaveAndReturnDto() {
        when(userRepository.existsByEmail(newUserRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.registerUser(newUserRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("user@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void registerUser_DuplicateEmail_ShouldThrowConflictException() {
        when(userRepository.existsByEmail(newUserRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(newUserRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email уже существует");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUsers_WithIds_ShouldCallFindAllByIdIn() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        when(userRepository.findAllByIdIn(eq(List.of(1L, 2L)), eq(pageable))).thenReturn(new PageImpl<>(List.of(user)));

        List<UserDto> result = userService.getUsers(List.of(1L, 2L), 0, 10);
        assertThat(result).hasSize(1);
    }

    @Test
    void deleteUser_NotFound_ShouldThrowNotFoundException() {
        when(userRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteUser(999L)).isInstanceOf(NotFoundException.class);
    }

    // --- новые internal-методы, обслуживающие UserInternalController ---

    @Test
    void getUserShort_Success() {
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        UserShortDto result = userService.getUserShort(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Иван Иванов");
    }

    @Test
    void getUserShort_NotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> userService.getUserShort(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getUsersShort_ReturnsBatchOfUsers() {
        User user2 = new User(2L, "second@example.com", "Пётр Петров");
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user, user2));

        List<UserShortDto> result = userService.getUsersShort(List.of(1L, 2L));

        assertThat(result).extracting(UserShortDto::getName).containsExactlyInAnyOrder("Иван Иванов", "Пётр Петров");
    }

    @Test
    void getUsersShort_EmptyIds_ReturnsEmptyList() {
        when(userRepository.findAllById(List.of())).thenReturn(List.of());
        assertThat(userService.getUsersShort(List.of())).isEmpty();
    }
}