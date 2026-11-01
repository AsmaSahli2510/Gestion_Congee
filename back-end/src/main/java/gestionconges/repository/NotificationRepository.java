package gestionconges.repository;

import gestionconges.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataire_IdOrderByDateCreationDesc(Long destinataireId);

    List<Notification> findByDestinataire_IdAndLectureFalseOrderByDateCreationDesc(Long destinataireId);

    long countByDestinataire_IdAndLectureFalse(Long destinataireId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lecture = true WHERE n.destinataire.id = :destinataireId AND n.lecture = false")
    int marquerToutesCommeLuesByDestinataireId(@Param("destinataireId") Long destinataireId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lecture = true WHERE n.id = :notificationId AND n.destinataire.id = :destinataireId")
    int marquerCommeLueByIdAndDestinataireId(@Param("notificationId") Long notificationId, @Param("destinataireId") Long destinataireId);
}
