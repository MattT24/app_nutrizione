package it.nutrizionista.restnutrizionista.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "misurazioni_antropometriche")
@EntityListeners(AuditingEntityListener.class)
public class MisurazioneAntropometrica {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
