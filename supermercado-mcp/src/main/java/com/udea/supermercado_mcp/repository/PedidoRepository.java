package com.udea.supermercado_mcp.repository;

import java.util.List;

import com.udea.supermercado_mcp.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findByEstado(String estado);
}
