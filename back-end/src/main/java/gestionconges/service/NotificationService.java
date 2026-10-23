package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Notification;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification creerNotification(Entreprise entreprise, Utilisateur destinataire, String titre, String message, String lien) {
        Notification notification = new Notification(entreprise, destinataire, titre, message, lien);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(Long utilisateurId) {
        return notificationRepository.findByDestinataire_IdOrderByDateCreationDesc(utilisateurId);
    }

    public List<Notification> getNotificationsNonLues(Long utilisateurId) {
        return notificationRepository.findByDestinataire_IdAndLectureFalseOrderByDateCreationDesc(utilisateurId);
    }

    public long getNombreNonLues(Long utilisateurId) {
        return notificationRepository.countByDestinataire_IdAndLectureFalse(utilisateurId);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId, Long utilisateurId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            if (notif.getDestinataire().getId().equals(utilisateurId)) {
                notif.setLecture(true);
                notificationRepository.save(notif);
            }
        });
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        List<Notification> nonLues = notificationRepository.findByDestinataire_IdAndLectureFalseOrderByDateCreationDesc(utilisateurId);
        for (Notification n : nonLues) {
            n.setLecture(true);
        }
        notificationRepository.saveAll(nonLues);
    }
}
