package com.udea.supermercado_mcp.repository;

import java.util.List;

import com.udea.supermercado_mcp.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

	List<ItemPedido> findByPedidoId(Long pedidoId);
}
