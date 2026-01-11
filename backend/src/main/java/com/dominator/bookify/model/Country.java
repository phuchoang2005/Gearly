package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "countries")
public class Country {
    @Id
    private String _id;
    private Integer id;
    private String name;
    private String iso2;
}
