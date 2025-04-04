package ru.mtuci.demo;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableEncryptableProperties
public class DemoApplicationT {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplicationT.class, args);
	}

}