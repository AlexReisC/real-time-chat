package chat.auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    private String name;

    public enum Values {
        ADMIN(1L),
        BASIC(2L);

        Long roleId;

        Values(Long roleId) {
            this.roleId = roleId;
        }

        public long getRoleId(){
            return this.roleId;
        }
    }
}
