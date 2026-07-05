package user.service.Serivce;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import user.service.Entity.User;
import user.service.Repository.UserRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserFinderService {

    UserRepository repository;

    public User byEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }

    public User byNumTem(Integer num) {
        return repository.findByNumTel(num)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + num));
    }


    public User byId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + id));
    }
}