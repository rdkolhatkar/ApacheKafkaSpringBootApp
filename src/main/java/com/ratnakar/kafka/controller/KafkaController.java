package com.ratnakar.kafka.controller;

import com.ratnakar.kafka.model.ProductRestModel;
import com.ratnakar.kafka.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class KafkaController {

    ProductService productService;

    public KafkaController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createProduct(@RequestBody ProductRestModel productRestModel){
        String productID = productService.createProduct(productRestModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(productID);
    }
}
