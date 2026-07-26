package com.irfan.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.irfan.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}