package gestionconges.api;

import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    @GetMapping("/api/test")
    public Map<String, String> testerApi() {
        return Map.of(
            "message", "API LeaveFlow / Gestion des Congés opérationnelle",
            "statut", "OK"
        );
    }

    @GetMapping("/api/types")
    public List<Map<String, Object>> getTypes() {
        return Arrays.stream(TypeConge.values())
                .map(t -> Map.of(
                        "name", (Object) t.getLibelle(),
                        "code", t.name(),
                        "deduct", t.isDeduitDuSolde(),
                        "justificatifRequis", t.isJustificatifRequis()
                ))
                .toList();
    }

    @GetMapping("/api/statuts")
    public List<String> getStatuts() {
        return List.of("SOUMISE", "APPROUVÉE", "REJETÉE", "ANNULÉE");
    }
}