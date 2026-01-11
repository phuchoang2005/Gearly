package com.dominator.bookify.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "image")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Image {
    private String url;
    private String alt;
}