package com.epesa.demo.repository;

import com.epesa.demo.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObraRepository extends JpaRepository<Obra, String> {}
