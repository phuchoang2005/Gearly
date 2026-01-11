package com.dominator.bookify.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "shippingInformation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingInformation {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Address address;
}