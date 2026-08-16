package service.recrutement.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import service.recrutement.Entity.CalendarInterview;
public interface CalendarInterviewRepository extends MongoRepository<CalendarInterview, String> {}