package com.ratnakar.kafka.service;

import com.ratnakar.kafka.model.ProductRestModel;

import java.util.concurrent.ExecutionException;

public interface ProductService {
    String createProduct(ProductRestModel productRestModel) throws Exception;
}
