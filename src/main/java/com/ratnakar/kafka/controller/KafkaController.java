package com.ratnakar.kafka.controller;

import com.ratnakar.kafka.exception.ErrorMessage;
import com.ratnakar.kafka.model.ProductRestModel;
import com.ratnakar.kafka.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/products")
public class KafkaController {

    ProductService productService;

    public KafkaController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/create")
    public ResponseEntity<Object> createProduct(@RequestBody ProductRestModel productRestModel) {
        String productID = null;
        try {
            productID = productService.createProduct(productRestModel);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage(new Date(), e.getMessage(), "/products/create"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productID);
    }
    @GetMapping("/response/200")
    public ResponseEntity<String> successResponse() {
        return ResponseEntity
                .status(HttpStatus.OK)           // HTTP 200
                .body("Request processed successfully");
    }
    @GetMapping("/response/500")
    public ResponseEntity<String> errorResponse() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)   // HTTP 500
                .body("Something went wrong on the server");
    }
}
