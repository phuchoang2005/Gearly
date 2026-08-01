package com.dominator.gearly.repository;
import com.dominator.gearly.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProductsInStockRepository extends MongoRepository<Product,String> {
    @Query("{'stock' : { $lt : 10 }}")
    List<Product> findProductsWithLowStock();
}
