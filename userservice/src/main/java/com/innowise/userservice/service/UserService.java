package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import org.springframework.data.domain.Page;

public interface UserService {

    /**
     * Create a new user;
     *
     * @param userRequestDto data of the user to create;
     * @return returns the result of creating user.
     */
    UserResponseDto createUser(UserRequestDto userRequestDto);

    /**
     * Get an existing user by ID;
     *
     * @param id ID of the user to get;
     * @return returns the user if it exists.
     */
    UserResponseDto getUserById(Long id);

    /**
     * Retrieves a paginated list of users with optional filtering;
     *
     * @param name optional  name to filter results;
     * @param surname optional surname to filter results;
     * @param page a number of page with records, that will be returned;
     * @param size the number of records per page (must be > 0);
     * @return the users, that who match the given criteria.
     */
    Page<UserResponseDto> getAllUsers(String name, String surname, int page, int size);

    /**
     * Update a user's data;
     *
     * @param id ID of the user to update;
     * @param userRequestDto new data to be uploaded;
     * @return returns the result of updating the user.
     */
    UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);

    /**
     * Activate/deactivate a user;
     *
     * @param id ID of a user;
     * @param isActive new state to be uploaded;
     * @return returns the result of changing the user's state.
     */
    UserResponseDto setUserActive(Long id, Boolean isActive);
}
