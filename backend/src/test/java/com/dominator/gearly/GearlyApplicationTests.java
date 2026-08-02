package com.dominator.gearly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Full application-context smoke test (S6). Wires the whole Spring graph against a
 * real, throwaway MongoDB started by Testcontainers — {@code @ServiceConnection}
 * overrides {@code spring.data.mongodb.uri}, and the {@code test} profile supplies
 * the otherwise-required secrets (see {@code application-test.properties}).
 *
 * <p>{@code disabledWithoutDocker = true} makes the class self-skip when no Docker
 * daemon is reachable, so {@code mvn test} still passes offline; run it with Colima
 * (or any Docker) up to actually exercise the context load.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class GearlyApplicationTests {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Test
    void contextLoads() {
    }
}
