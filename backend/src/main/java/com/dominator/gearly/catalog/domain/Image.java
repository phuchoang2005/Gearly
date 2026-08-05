package com.dominator.gearly.catalog.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

/**
 * A product image: where it is and what it shows.
 *
 * <p>Was {@code model.Image}, a {@code @Getter @Setter} bag carrying
 * {@code @Document(collection = "image")} — the fifth stray {@code @Document} of the refactor,
 * after the four S10 dropped. There is no {@code image} collection and never was; images are
 * only ever embedded in a product. Immutable now, so {@code ProductMapper.copyImages} — which
 * existed solely to stop a caller mutating a shared instance — is gone with it.
 *
 * <p>Serializes and stores as the same {@code {url, alt}} it always did.
 */
@Getter
@EqualsAndHashCode
public class Image {

    private final String url;
    private final String alt;

    @PersistenceCreator
    @JsonCreator
    public Image(@JsonProperty("url") String url, @JsonProperty("alt") String alt) {
        this.url = url;
        this.alt = alt;
    }
}
