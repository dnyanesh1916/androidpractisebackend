package com.attendease.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Configuration
public class DatabaseCheckConfig {

    @Bean
    CommandLineRunner checkDatabase(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();

                System.out.println("DATABASE CHECK");
                System.out.println("Database: "
                        + metaData.getDatabaseProductName());
                System.out.println("URL: "
                        + metaData.getURL());
                System.out.println("Username: "
                        + metaData.getUserName());
            }
        };
    }
}