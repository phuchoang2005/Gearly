package com.dominator.gearly.analytics.api;
import java.util.Date;
import lombok.Data;

@Data
public class LoyalCustomerDTO {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private long totalOrders;
    private double totalSpending;
    private Date firstOrder;
    private Date lastOrder;
}
