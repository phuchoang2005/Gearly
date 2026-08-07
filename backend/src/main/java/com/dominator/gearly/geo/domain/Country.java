package com.dominator.gearly.geo.domain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A country in the reference geo dataset.
 *
 * <h2>Read-only reference data</h2>
 * The geo dataset is loaded once from the seed and never written by this application — there is
 * no endpoint, service method or migration that saves one. So the aggregate exposes no way to
 * change it, which is also what {@code aggregates_expose_no_public_setters} requires of a
 * {@code @Document} in a {@code ..domain..} package. Lombok's {@code @Data} was generating a
 * public setter per field of a document nothing may modify; Spring Data writes the fields
 * reflectively and needs neither those nor the all-args constructor.
 *
 * <p>The {@code @Field} mappings and the {@code _id}/{@code id} pair are inherited exactly: the
 * dataset stores a Mongo {@code ObjectId} as {@code _id} and its own numeric key as {@code id},
 * and every lookup here joins on the numeric one.
 */
@Getter
@Document(collection = "countries")
public class Country {
    @Id
    private String _id;
    private Integer id;
    private String name;
    private String iso2;

    /** For Spring Data. */
    protected Country() {
    }
}
