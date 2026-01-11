// src/main/java/com/dominator/bookify/service/AdminBookService.java
package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.AdminBookDTO;
import com.dominator.bookify.dto.AdminUserDTO;
import com.dominator.bookify.dto.BookCreateDTO;
import com.dominator.bookify.dto.BookUpdateDTO;

import java.util.List;

public interface AdminUserService {
    List<AdminUserDTO> getAllUsers(String fullNameLike, String emailLike);
    AdminUserDTO getUserById(String id);
    boolean activateUser(String id);
    boolean deactivateUser(String id);
}
