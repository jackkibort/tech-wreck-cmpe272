package edu.sjsu.securehr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the SecureHR Employee API. */
@SpringBootApplication
public class SecureHrApiApplication {

  /**
   * Starts the application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(SecureHrApiApplication.class, args);
  }
}
