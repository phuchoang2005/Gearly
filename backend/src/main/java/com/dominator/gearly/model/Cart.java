package com.dominator.gearly.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    private String id;

    /**
     * Optimistic-locking token. The cart is written from several paths that each
     * read-modify-write the whole line list — add, update quantity, remove, the stock
     * sync, and the guest-cart merge at login — so a concurrent pair (two tabs, or a
     * merge racing an add) could drop one side's changes entirely.
     *
     * <p>{@code @JsonIgnore}: internal, never on the wire, never client-settable.
     * Boxed, and backfilled to 0 by {@code data/seed/migrate.js} — see {@code Product}.
     */
    @Version
    @JsonIgnore
    private Long version;

    private String userId;
    private String guestId;

    private List<CartItem> items = new ArrayList<>();

    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private Date updatedAt;
}