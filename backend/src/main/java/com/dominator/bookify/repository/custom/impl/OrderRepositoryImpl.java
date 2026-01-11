package com.dominator.bookify.repository.custom.impl;

import com.dominator.bookify.model.Order;
import com.dominator.bookify.repository.custom.OrderRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Order> searchOrders(String userId,
                                    String status,
                                    String searchTerm,
                                    Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("orderStatus").is(status.trim()));
        }

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            List<Criteria> orList = new ArrayList<>();
            String normalized = searchTerm.trim();
            String regex = ".*" + Pattern.quote(normalized) + ".*";

            try {
                double amount = Double.parseDouble(normalized);
                orList.add(Criteria.where("totalAmount").is(amount));
            } catch (NumberFormatException ignored) {}

            orList.add(Criteria.where("_id").is(normalized));
            orList.add(Criteria.where("items.title").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.firstName").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.lastName").regex(regex, "i"));

            String[] nameParts = normalized.split("\\s+");
            if (nameParts.length >= 2) {
                String part1 = Pattern.quote(nameParts[0]);
                String part2 = Pattern.quote(nameParts[1]);
                orList.add(new Criteria().andOperator(
                        Criteria.where("shippingInformation.firstName").regex(part1, "i"),
                        Criteria.where("shippingInformation.lastName").regex(part2, "i")
                ));
                orList.add(new Criteria().andOperator(
                        Criteria.where("shippingInformation.firstName").regex(part2, "i"),
                        Criteria.where("shippingInformation.lastName").regex(part1, "i")
                ));
            }

            orList.add(Criteria.where("shippingInformation.email").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.phoneNumber").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.address.street").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.address.city").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.address.state").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.address.postalCode").regex(regex, "i"));
            orList.add(Criteria.where("shippingInformation.address.country").regex(regex, "i"));
            orList.add(Criteria.where("orderStatus").regex(regex, "i"));

            query.addCriteria(new Criteria().orOperator(orList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Order.class);
        query.with(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()));
        List<Order> orders = mongoTemplate.find(query, Order.class);

        return new PageImpl<>(orders, pageable, total);
    }
}
