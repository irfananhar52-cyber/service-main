package com.irfan.product.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.irfan.product.model.Product;
import com.irfan.product.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        logger.info("Product created: id={}, name={}, price={}, stock={}",
                savedProduct.getId(), savedProduct.getName(), savedProduct.getPrice(), savedProduct.getStock());
        return savedProduct;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        logger.info("Fetching product by id={}", id);
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setPrice(updatedProduct.getPrice());
        Product savedProduct = productRepository.save(existingProduct);
        logger.info("Product updated: id={}, name={}, price={}, stock={}",
                savedProduct.getId(), savedProduct.getName(), savedProduct.getPrice(), savedProduct.getStock());
        return savedProduct;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        logger.info("Product deleted: id={}", id);
    }
}