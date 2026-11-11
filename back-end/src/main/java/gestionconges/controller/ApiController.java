package gestionconges.controller;

import gestionconges.enums.StatutConge;
import gestionconges.enums.TypeConge;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ApiController {

    @GetMapping("/api/test")
    @ResponseBody
    public Map<String, String> testerApi() {
        return Map.of(
            "message", "API LeaveFlow / Gestion des Congés opérationnelle",
            "statut", "OK"
        );
    }

    @GetMapping("/api/types")
    @ResponseBody
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
    @ResponseBody
    public List<String> getStatuts() {
        return List.of("SOUMISE", "APPROUVÉE", "REJETÉE", "ANNULÉE");
    }
}