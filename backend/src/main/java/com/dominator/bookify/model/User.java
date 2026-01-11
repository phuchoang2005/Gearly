package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String profileAvatar;

    private String firstName;

    private String lastName;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    private Address address;

    private String role = "CUSTOMER";

    private boolean verified = false;

    private List<String> favorites; // Book IDs

    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
