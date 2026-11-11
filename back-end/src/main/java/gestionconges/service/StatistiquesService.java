package gestionconges.service;

import java.util.*;

public interface StatistiquesService {

    Map<String, Object> getStatsEntreprise(Long entrepriseId);

    Map<String, Object> getStatsEquipe(Long equipeId);

    byte[] exporterDemandesCsv(Long entrepriseId);
}