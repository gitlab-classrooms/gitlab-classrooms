package fr.univ_lille.gitlab.classrooms.adapters.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the JPA adapters.
 */
@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.jpa.generate-ddl=false", "spring.jpa.hibernate.ddl-auto=validate"})
class PostgresqlJPAAdaptersTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void jpaContextLoads(@Autowired EntityManagerFactory entityManagerFactory) {
        // if the context loads, it means that the schema created by flyway is valid.
        assertThat(entityManagerFactory.isOpen()).isTrue();
    }
}
