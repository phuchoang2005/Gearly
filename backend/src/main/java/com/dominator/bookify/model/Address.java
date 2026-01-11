package com.dominator.bookify.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private int cityId;
    private String state;
    private int stateId;
    private String postalCode;
    private String country;
    private int countryId;
}
