package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.PlicometriaRepository;

@Service
public class PlicometriaService {

	@Autowired private PlicometriaRepository repo;
}
