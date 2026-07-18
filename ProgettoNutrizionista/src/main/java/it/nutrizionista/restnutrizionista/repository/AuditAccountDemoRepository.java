package it.nutrizionista.restnutrizionista.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.nutrizionista.restnutrizionista.entity.AuditAccountDemo;

public interface AuditAccountDemoRepository extends JpaRepository<AuditAccountDemo, Long> {
}
