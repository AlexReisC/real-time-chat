package chat.auth_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import chat.auth_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    
}
