package com.neusoft.neusoft_project;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
// import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine; // Don't use this unless you added the dependency

import java.util.Collections;

public class CodeGenerator {

    public static void main(String[] args) {
        // 1. Database Configuration
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/neusoft_db?useSSL=false&serverTimezone=Asia/Shanghai");
        String username = System.getenv().getOrDefault("DB_USERNAME", "neusoft_dev");
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set DB_PASSWORD before running the code generator.");
        }

        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(url, username, password)
                // 2. Global Config
                .globalConfig(builder -> {
                    builder.author("AI_Engineer") // You can change this
                            .outputDir(projectPath + "/src/main/java")
                            .commentDate("yyyy-MM-dd")
                            .disableOpenDir();
                })
                // 3. Package Config
                .packageConfig(builder -> {
                    builder.parent("com.neusoft.neusoft_project")
                            .entity("entity")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"));
                })
                // 4. Strategy Config
                .strategyConfig(builder -> {
                    // IMPORTANT: Fixed the space in "users"
                    builder.addInclude("users", "doctor_profiles", "patient_profiles")

                            // Entity Config
                            .entityBuilder()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .enableLombok() // ENABLED THIS: crucial for cleaner code
                            .enableTableFieldAnnotation()

                            // Controller Config
                            .controllerBuilder()
                            .enableRestStyle(); // Creates @RestController
                })
                // 5. Template Engine (Use Default Velocity)
                // .templateEngine(new FreemarkerTemplateEngine()) // REMOVED: You are using Velocity
                .execute();
    }
}
