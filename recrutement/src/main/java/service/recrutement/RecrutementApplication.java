package service.recrutement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@Configuration
@EnableAsync
public class RecrutementApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecrutementApplication.class, args);
	}

}
