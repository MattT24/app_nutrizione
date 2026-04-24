package it.nutrizionista.restnutrizionista.controller;

import it.nutrizionista.restnutrizionista.dto.SystemTagDto;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.mapper.DtoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    public SystemController() {
    }

    @GetMapping("/tags")
    public ResponseEntity<List<SystemTagDto>> getStandardTags() {
        List<SystemTagDto> tags = Arrays.stream(TagStandard.values())
                                        .map(DtoMapper::toTagDto)
                                        .toList();
        return ResponseEntity.ok(tags);
    }
}
