package com.jeet.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogBack {
    private static final Logger logger = LoggerFactory.getLogger(LogBack.class);

    public static void main(String[] args) {
        logger.debug("Debug message");
        logger.info("Info message");
        logger.error("Error message");
    }
}