package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Simple acknowledgement body: {@code {"message": "..."}}. Both frontends read {@code response.data.message}. */
@Getter
@AllArgsConstructor
public class MessageResponse {
    private String message;
}
