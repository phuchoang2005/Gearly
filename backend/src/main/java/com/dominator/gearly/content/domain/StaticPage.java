package com.dominator.gearly.content.domain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A slug-addressed page of standing copy — "About Us", "Shipping Policy", the terms.
 *
 * <p>Read-only for the same reason as {@link BlogPost}, and with the same consequence: no
 * setters, no all-args constructor, nothing that lets a page be altered by whoever happens to
 * hold a reference to it. Editing happens in the database.
 */
@Getter
@Document(collection = "pages")
public class StaticPage {

    @Id
    private String id;

    private String title;

    private String slug;

    private String content;

    private Instant lastModified;

    /** For Spring Data. */
    protected StaticPage() {
    }
}
