package com.jeet.db;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;


public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Load the base configuration
            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml");

            // Apply overrides from environment variables (if present)
            if (System.getenv("${DB_URL_BOOKS}") != null) {
                registryBuilder.applySetting("hibernate.connection.url", System.getenv("DB_URL"));
            }

            StandardServiceRegistry standardRegistry = registryBuilder.build();
            Metadata metadata = new MetadataSources(standardRegistry).getMetadataBuilder().build();

            return metadata.getSessionFactoryBuilder().build();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}