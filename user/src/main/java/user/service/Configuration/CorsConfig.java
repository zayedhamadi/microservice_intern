package user.service.Configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer resourceConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String uploadPath = System.getProperty("user.dir").replace("\\", "/");
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadPath + "/uploads/");
            }
        };
    }

    @PostConstruct
    public void init() {
        System.out.println(">>> UPLOAD PATH: " + System.getProperty("user.dir"));
    }
}