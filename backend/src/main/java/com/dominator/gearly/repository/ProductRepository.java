package com.dominator.gearly.repository;

import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.repository.custom.ProductRepositoryCustom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {
    List<ProductSummaryDTO> findByOrderByAverageRatingDesc(Pageable pageable);
    List<Product> findByTitleContainingIgnoreCase(String title);
    List<Product> findByCategoryIdsIn(List<ObjectId> categoryIds);

    String title(String title);
}
