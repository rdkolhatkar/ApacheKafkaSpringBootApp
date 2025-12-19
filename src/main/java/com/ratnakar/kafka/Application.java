package com.ratnakar.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    /*
    Below getRestTemplate method will return the instance of RestTemplate, This will create the new instance of RestTemplate HTTP Client
    And it will Put it into Spring Application Context, We can now inject this getRestTemplate() Object in our Kafka listener
    */
    @Bean
    RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
