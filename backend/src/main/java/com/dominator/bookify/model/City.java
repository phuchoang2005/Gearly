package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cities")
public class City {
    @Id
    private String _id;

    private Integer id;
    private String name;

    @Field("state_id")
    private Integer stateId;

    @Field("state_code")
    private String stateCode;

    @Field("state_name")
    private String stateName;

    @Field("country_id")
    private Integer countryId;

    @Field("country_code")
    private String countryCode;

    @Field("country_name")
    private String countryName;
}
