package com.dominator.gearly;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GearlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GearlyApplication.class, args);
    }

}

// database -> repository -> model -> dto -> service -> controller => API
