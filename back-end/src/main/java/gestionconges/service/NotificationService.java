package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Notification;
import gestionconges.entity.Utilisateur;
import gestionconges.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(@Autowired NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification creerNotification(Entreprise entreprise, Utilisateur destinataire, String titre, String message, String lien) {
        Notification notification = new Notification(entreprise, destinataire, titre, message, lien);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long utilisateurId) {
        return notificationRepository.findByDestinataire_IdOrderByDateCreationDesc(utilisateurId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsNonLues(Long utilisateurId) {
        return notificationRepository.findByDestinataire_IdAndLectureFalseOrderByDateCreationDesc(utilisateurId);
    }

    @Transactional(readOnly = true)
    public long getNombreNonLues(Long utilisateurId) {
        return notificationRepository.countByDestinataire_IdAndLectureFalse(utilisateurId);
    }

    @Transactional
    public void marquerCommeLue(Long notificationId, Long utilisateurId) {
        notificationRepository.marquerCommeLueByIdAndDestinataireId(notificationId, utilisateurId);
    }

    @Transactional
    public void marquerToutesCommeLues(Long utilisateurId) {
        notificationRepository.marquerToutesCommeLuesByDestinataireId(utilisateurId);
    }
}
