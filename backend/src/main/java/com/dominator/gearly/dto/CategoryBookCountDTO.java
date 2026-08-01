package com.dominator.gearly.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBookCountDTO {
    private String id;
    private String name;
    private int bookCount;
}
