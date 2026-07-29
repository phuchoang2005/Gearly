package com.dominator.bookify;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Full-context load needs a live MongoDB (localhost:27017) + runtime secrets. "
        + "Re-enabled in S6 with embedded/mocked Mongo + test properties per the sprint plan. "
        + "S2's safety net is the @WebMvcTest slices instead.")
class BookifyApplicationTests {

    @Test
    void contextLoads() {
    }

}
