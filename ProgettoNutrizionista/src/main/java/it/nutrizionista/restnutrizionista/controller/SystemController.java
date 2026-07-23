package it.nutrizionista.restnutrizionista.controller;

import it.nutrizionista.restnutrizionista.dto.SystemTagDto;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    public SystemController() {
    }

    /** Catalogo statico dei tag MDSS (enum {@code TagStandard}). B3: nessun dato utente → basta l'autenticazione. */
    @GetMapping("/tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SystemTagDto>> getStandardTags() {
        List<SystemTagDto> tags = Arrays.stream(TagStandard.values())
                                        .map(DtoMapper::toTagDto)
                                        .toList();
        return ResponseEntity.ok(tags);
    }
}
