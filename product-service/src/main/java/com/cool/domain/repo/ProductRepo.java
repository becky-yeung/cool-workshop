package com.cool.domain.repo;

import com.cool.domain.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepo extends MongoRepository<Product, String> {
    Optional<Product> findProductBySkuCode(String skuCode);
}
