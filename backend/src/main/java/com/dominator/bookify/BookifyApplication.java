package com.dominator.bookify;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookifyApplication.class, args);
    }

}

// database -> repository -> model -> dto -> service -> controller => API
