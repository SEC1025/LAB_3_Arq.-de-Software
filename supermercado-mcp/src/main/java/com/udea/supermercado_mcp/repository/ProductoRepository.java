package com.udea.supermercado_mcp.repository;

import java.util.List;

import com.udea.supermercado_mcp.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByNombreContainingIgnoreCase(String fragmento);
}
