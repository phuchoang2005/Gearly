package com.dominator.bookify.service.admin;

import java.util.List;

import com.dominator.bookify.dto.LoyalCustomerDTO;
import com.dominator.bookify.dto.TopAvgOrderValueUserDTO;

public interface AdminDashboardGetUserInterface {
    public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue();

    public List<LoyalCustomerDTO> getTop10LoyalCustomers();

}
