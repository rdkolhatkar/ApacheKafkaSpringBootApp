package com.ratnakar.kafka.controller;

import com.ratnakar.kafka.model.ProductRestModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class KafkaController {
    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductRestModel productRestModel){
        return ResponseEntity.status(HttpStatus.CREATED).body("");
    }
}
