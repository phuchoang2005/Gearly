package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.AdminUserDTO;
import com.dominator.bookify.mapper.UserMapper;
import com.dominator.bookify.model.User;
import com.dominator.bookify.model.UserStatus;
import com.dominator.bookify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.dominator.bookify.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<AdminUserDTO> getAllUsers(String fullNameLike, String emailLike) {
        List<User> users;

        boolean hasName = fullNameLike != null && !fullNameLike.isBlank();
        boolean hasEmail = emailLike!= null &&!emailLike.isBlank();

        if (hasName && hasEmail) {
            users = userRepository
                    .findAllByFullNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
                            fullNameLike,
                            emailLike
                    );
        } else if (hasName) {
            users = userRepository.findAllByFullNameContainingIgnoreCase(fullNameLike);
        } else if (hasEmail) {
            users = userRepository.findAllByEmailContainingIgnoreCase(emailLike);
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(userMapper::toAdminDto).collect(Collectors.toList());
    }


    public AdminUserDTO getUserById(String id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found"));
        return userMapper.toAdminDto(user);
    }

    public boolean activateUser(String id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return true;
    }

    public boolean deactivateUser(String id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        return true;
    }
}
