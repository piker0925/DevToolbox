package com.back;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

public abstract class AbstractMySQLIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("devtoolbox")
            .withUsername("devtoolbox")
            .withPassword("1234");

    static {
        MYSQL.start();
    }
}
