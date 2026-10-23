package gestionconges.repository;

import gestionconges.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByEntreprise_IdOrderByDateCreationDesc(Long entrepriseId);
    List<Invitation> findByEntreprise_IdAndUtiliseeFalse(Long entrepriseId);
    boolean existsByEmailAndEntreprise_IdAndUtiliseeFalse(String email, Long entrepriseId);
}
