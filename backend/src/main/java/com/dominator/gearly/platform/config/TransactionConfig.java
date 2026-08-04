package com.dominator.gearly.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Makes {@code @Transactional} actually do something.
 *
 * <p>Before this bean existed the codebase carried seven {@code @Transactional}
 * annotations and no transaction manager, so every one of them was inert: Spring had
 * nothing to begin a transaction with, and a partially-completed write stayed written.
 * The concrete failure this closes is in order placement — an order could be saved and
 * then a mid-loop stock decrement could fail, leaving the order placed against stock that
 * was only partially reserved.
 *
 * <p><b>This bean has a hard runtime prerequisite:</b> MongoDB only supports
 * multi-document transactions on a replica set or a sharded cluster. Against a standalone
 * {@code mongod}, every transactional method now fails with <em>"Transaction numbers are
 * only allowed on a replica set member or mongos"</em> rather than silently half-writing.
 * That is the intended trade — a loud failure beats silent corruption — but it does mean
 * a plain {@code docker run mongo} is no longer a working local setup. {@code make up}
 * starts a single-node replica set and {@code make mongo-up} waits for it to elect a
 * primary; see {@code backend/README.md} "Transactions".
 *
 * <p>Integration tests get this for free: Testcontainers' {@code MongoDBContainer} always
 * starts a single-node replica set.
 *
 * <p>Declaring a {@link PlatformTransactionManager} bean is also what triggers Spring
 * Boot's {@code TransactionAutoConfiguration} to switch on
 * {@code @EnableTransactionManagement}, so no separate annotation is needed.
 */
@Configuration
public class TransactionConfig {

    @Bean
    public PlatformTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
