package gestionconges.repository;

import gestionconges.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataire_IdOrderByDateCreationDesc(Long destinataireId);
    List<Notification> findByDestinataire_IdAndLectureFalseOrderByDateCreationDesc(Long destinataireId);
    long countByDestinataire_IdAndLectureFalse(Long destinataireId);
}
