package com.udea.supermercado_mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.udea.supermercado_mcp.service.PedidoToolsService;
import com.udea.supermercado_mcp.service.PedidoToolsService.PedidoVista;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SupermercadoMcpApplicationTests {

	@Autowired
	private PedidoToolsService pedidos;

	@Test
	void creaUnPedidoADomicilioYDescuentaStock() {
		assertFalse(pedidos.listarProductos().isEmpty());
		int stockAntes = pedidos.buscarProducto("Leche").getFirst().stock();

		PedidoVista pedido = pedidos.crearPedido("Ana", "Calle 10", "3000000000");
		PedidoVista conProducto = pedidos.agregarProducto(pedido.id(), "Leche entera", 2);

		assertEquals(PedidoToolsService.PENDIENTE, conProducto.estado());
		assertEquals("9.00", conProducto.total());
		assertEquals(stockAntes - 2, pedidos.buscarProducto("Leche entera").getFirst().stock());

		String cancelacion = pedidos.cancelarPedido(pedido.id());
		assertTrue(cancelacion.contains("cancelado"));
		assertEquals(stockAntes, pedidos.buscarProducto("Leche entera").getFirst().stock());
	}
}
