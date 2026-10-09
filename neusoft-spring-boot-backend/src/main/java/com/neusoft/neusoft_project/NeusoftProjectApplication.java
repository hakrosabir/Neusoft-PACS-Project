package com.neusoft.neusoft_project;

import org.mybatis.spring.annotation.MapperScan; // <--- IMPORT THIS
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// This tells Spring where your database interfaces are:
@MapperScan("com.neusoft.neusoft_project.mapper")
public class NeusoftProjectApplication {

	public static void main(String[] args) {
		// FIX: Disable PDFBox font caching to prevent timeouts on Windows
		System.setProperty("org.apache.pdfbox.font.cache", "disable");

		SpringApplication.run(NeusoftProjectApplication.class, args);
	}

}