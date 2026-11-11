package gestionconges.service;

import gestionconges.entity.Entreprise;
import gestionconges.entity.Notification;
import gestionconges.entity.Utilisateur;

import java.util.List;

public interface NotificationService {

    Notification creerNotification(Entreprise entreprise, Utilisateur destinataire, String titre, String message, String lien);

    List<Notification> getNotifications(Long utilisateurId);

    List<Notification> getNotificationsNonLues(Long utilisateurId);

    long getNombreNonLues(Long utilisateurId);

    void marquerCommeLue(Long notificationId, Long utilisateurId);

    void marquerToutesCommeLues(Long utilisateurId);
}