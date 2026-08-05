package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Address;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

/**
 * Where an order is going and who to ask for at the door — captured at placement time, like
 * every other snapshot on an order. A later edit to the customer's profile address does not
 * redirect a parcel that has already shipped.
 *
 * <p>Inside the {@link Order} boundary; its {@code @Document(collection = "shippingInformation")}
 * came off in the move, that collection having never existed.
 *
 * <p>The name and contact fields stay loose {@code String}s rather than becoming
 * {@code PersonName}, {@code EmailAddress} and {@code PhoneNumber}. Those value objects
 * validate, and an order is a historical record: a delivery address captured two years ago
 * with a phone number that no longer parses must still be readable. S12 decides whether to
 * validate them on the way in, where rejecting bad input is useful.
 */
@Getter
public class ShippingInformation {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final Address address;

    @PersistenceCreator
    @JsonCreator
    public ShippingInformation(@JsonProperty("firstName") String firstName,
                               @JsonProperty("lastName") String lastName,
                               @JsonProperty("email") String email,
                               @JsonProperty("phoneNumber") String phoneNumber,
                               @JsonProperty("address") Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
}
