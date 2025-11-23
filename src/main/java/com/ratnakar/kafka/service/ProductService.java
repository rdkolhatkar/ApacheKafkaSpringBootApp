package com.ratnakar.kafka.service;

import com.ratnakar.kafka.model.ProductRestModel;

public interface ProductService {
    String createProduct(ProductRestModel productRestModel);
}
