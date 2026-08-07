package com.dominator.gearly.catalog.infrastructure;

import com.dominator.gearly.catalog.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Spring Data's half of the catalog adapter — the derived queries that need no help.
 *
 * <p>Package-private on purpose: it is an implementation detail of
 * {@link MongoProductRepository}, and ArchUnit's
 * {@code spring_data_repositories_live_only_in_infrastructure} is what stops anything outside
 * this package from injecting it directly the way {@code ProductService} used to.
 *
 * <p>Gone with the interface it replaces: {@code String title(String)}, a stray method with no
 * Spring Data prefix that would have failed query derivation the moment anything called it.
 * Nothing did. S13 lists it as dead code; it dies here because this interface is being written
 * from scratch and there was no reason to copy it across.
 */
interface SpringDataProductRepository extends MongoRepository<Product, String> {

    List<Product> findByTitleContainingIgnoreCase(String title);
}
