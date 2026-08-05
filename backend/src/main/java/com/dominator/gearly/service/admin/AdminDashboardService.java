package com.dominator.gearly.service.admin;

import org.springframework.stereotype.Service;

import com.dominator.gearly.dto.BestSellerDTO;
import com.dominator.gearly.catalog.api.ProductInLowStockDTO;
import com.dominator.gearly.dto.LoyalCustomerDTO;
import com.dominator.gearly.dto.TopAvgOrderValueUserDTO;
import com.dominator.gearly.dto.TopCategoryQuantityDTO;

import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
        private final AdminDashboardGetProductService getProductImp;
        private final AdminDashboardGetUserService getUserImp;

        public List<BestSellerDTO> getTop10BestSellingProducts() {
                return getProductImp.getTop10BestSellingProducts();
        }

        public List<TopCategoryQuantityDTO> getTop10ProductsPerCategory() {
                return getProductImp.getTop10ProductsPerCategory();
        }

        public List<LoyalCustomerDTO> getTop10LoyalCustomers() {
                return getUserImp.getTop10LoyalCustomers();
        }

        public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue() {
                return getUserImp.findUserWithHighestAvgOrderValue();
        }

        public List<ProductInLowStockDTO> getProductWithLowStock() {
                return getProductImp.getProductWithLowStock();
        }
}