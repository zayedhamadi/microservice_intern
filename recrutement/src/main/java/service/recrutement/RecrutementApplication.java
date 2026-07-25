package service.recrutement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableMongoRepositories(basePackages = "recrutement.repository")
//@EnableFeignClients
public class RecrutementApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecrutementApplication.class, args);
	}

}
