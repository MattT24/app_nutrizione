package it.nutrizionista.restnutrizionista.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.nutrizionista.restnutrizionista.repository.MicroRepository;

@Service
public class MicroService {

	@Autowired private MicroRepository repo;
}
