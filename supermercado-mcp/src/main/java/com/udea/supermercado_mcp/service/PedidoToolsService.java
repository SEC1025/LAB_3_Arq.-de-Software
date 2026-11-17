package com.udea.supermercado_mcp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.udea.supermercado_mcp.model.ItemPedido;
import com.udea.supermercado_mcp.model.Pedido;
import com.udea.supermercado_mcp.model.Producto;
import com.udea.supermercado_mcp.repository.ItemPedidoRepository;
import com.udea.supermercado_mcp.repository.PedidoRepository;
import com.udea.supermercado_mcp.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PedidoToolsService {

	public static final String PENDIENTE = "PENDIENTE";
	public static final String CANCELADO = "CANCELADO";

	private final ProductoRepository productos;
	private final PedidoRepository pedidos;
	private final ItemPedidoRepository items;

	public PedidoToolsService(ProductoRepository productos, PedidoRepository pedidos, ItemPedidoRepository items) {
		this.productos = productos;
		this.pedidos = pedidos;
		this.items = items;
	}

	public record ProductoVista(Long id, String nombre, String precio, Integer stock, String unidad) {
	}

	public record ItemVista(String producto, int cantidad, String precioUnitario, String subtotal) {
	}

	public record PedidoVista(
			Long id,
			String cliente,
			String direccion,
			String telefono,
			String estado,
			String fecha,
			String total,
			List<ItemVista> items) {
	}

	@Tool(description = "Devuelve el catalogo de productos del supermercado con precio y stock.")
	@Transactional(readOnly = true)
	public List<ProductoVista> listarProductos() {
		return productos.findAll().stream().map(this::vista).toList();
	}

	@Tool(description = "Busca productos cuyo nombre contiene el fragmento indicado.")
	@Transactional(readOnly = true)
	public List<ProductoVista> buscarProducto(
			@ToolParam(description = "Fragmento del nombre, por ejemplo leche") String fragmento) {
		if (fragmento == null || fragmento.isBlank()) {
			throw new IllegalArgumentException("Indica el nombre del producto a buscar");
		}
		return productos.findByNombreContainingIgnoreCase(fragmento.trim()).stream().map(this::vista).toList();
	}

	@Tool(description = "Crea un pedido a domicilio pendiente. Devuelve el id para agregar productos.")
	@Transactional
	public PedidoVista crearPedido(
			@ToolParam(description = "Nombre de quien recibe el pedido") String cliente,
			@ToolParam(description = "Direccion de entrega") String direccion,
			@ToolParam(description = "Telefono de contacto; puede ir vacio") String telefono) {
		if (cliente == null || cliente.isBlank()) {
			throw new IllegalArgumentException("El nombre del cliente es obligatorio");
		}
		if (direccion == null || direccion.isBlank()) {
			throw new IllegalArgumentException("La direccion de entrega es obligatoria");
		}
		Pedido pedido = new Pedido();
		pedido.setCliente(cliente.trim());
		pedido.setDireccion(direccion.trim());
		pedido.setTelefono(telefono == null ? "" : telefono.trim());
		pedido.setEstado(PENDIENTE);
		pedido.setFecha(LocalDate.now().toString());
		return detalle(pedidos.save(pedido));
	}

	@Tool(description = "Agrega unidades de un producto a un pedido pendiente y descuenta el stock.")
	@Transactional
	public PedidoVista agregarProducto(
			@ToolParam(description = "Id del pedido") Long pedidoId,
			@ToolParam(description = "Nombre del producto, por ejemplo Pan tajado") String nombreProducto,
			@ToolParam(description = "Cantidad de unidades a pedir") Integer cantidad) {
		if (cantidad == null || cantidad < 1) {
			throw new IllegalArgumentException("La cantidad debe ser al menos 1");
		}
		Pedido pedido = pedidoPendiente(pedidoId);
		List<Producto> coincidencias = productos.findByNombreContainingIgnoreCase(nombreProducto == null ? "" : nombreProducto.trim());
		if (coincidencias.isEmpty()) {
			throw new IllegalArgumentException("No hay un producto que coincida con " + nombreProducto);
		}
		if (coincidencias.size() > 1) {
			throw new IllegalArgumentException("Hay varios productos parecidos. Usa el nombre completo");
		}
		Producto producto = coincidencias.getFirst();
		if (producto.getStock() < cantidad) {
			throw new IllegalArgumentException("Stock insuficiente de " + producto.getNombre() + ". Quedan " + producto.getStock());
		}
		producto.setStock(producto.getStock() - cantidad);
		productos.save(producto);

		ItemPedido item = new ItemPedido();
		item.setPedido(pedido);
		item.setProducto(producto);
		item.setCantidad(cantidad);
		item.setPrecioUnitario(producto.getPrecio());
		items.save(item);
		return detalle(pedido);
	}

	@Tool(description = "Muestra un pedido con sus productos, cantidades y total.")
	@Transactional(readOnly = true)
	public PedidoVista consultarPedido(
			@ToolParam(description = "Id del pedido") Long pedidoId) {
		return detalle(pedidos.findById(pedidoId)
				.orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId)));
	}

	@Tool(description = "Lista los pedidos a domicilio, incluyendo cancelados.")
	@Transactional(readOnly = true)
	public List<PedidoVista> listarPedidos() {
		return pedidos.findAll().stream().map(this::detalle).toList();
	}

	@Tool(description = "Cancela un pedido pendiente y devuelve las unidades al stock. Confirma el id antes de usarla.")
	@Transactional
	public String cancelarPedido(
			@ToolParam(description = "Id del pedido a cancelar") Long pedidoId) {
		Pedido pedido = pedidoPendiente(pedidoId);
		for (ItemPedido item : items.findByPedidoId(pedido.getId())) {
			Producto producto = item.getProducto();
			producto.setStock(producto.getStock() + item.getCantidad());
			productos.save(producto);
		}
		pedido.setEstado(CANCELADO);
		pedidos.save(pedido);
		return "Pedido " + pedido.getId() + " cancelado. El stock fue devuelto.";
	}

	private Pedido pedidoPendiente(Long pedidoId) {
		Pedido pedido = pedidos.findById(pedidoId)
				.orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId));
		if (!PENDIENTE.equals(pedido.getEstado())) {
			throw new IllegalArgumentException("El pedido " + pedidoId + " esta " + pedido.getEstado() + " y no se puede modificar");
		}
		return pedido;
	}

	private PedidoVista detalle(Pedido pedido) {
		List<ItemVista> lineas = items.findByPedidoId(pedido.getId()).stream().map(item -> {
			BigDecimal subtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
			return new ItemVista(item.getProducto().getNombre(), item.getCantidad(), dinero(item.getPrecioUnitario()), dinero(subtotal));
		}).toList();
		BigDecimal total = items.findByPedidoId(pedido.getId()).stream()
				.map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new PedidoVista(pedido.getId(), pedido.getCliente(), pedido.getDireccion(), pedido.getTelefono(),
				pedido.getEstado(), pedido.getFecha(), dinero(total), lineas);
	}

	private ProductoVista vista(Producto producto) {
		return new ProductoVista(producto.getId(), producto.getNombre(), dinero(producto.getPrecio()), producto.getStock(), producto.getUnidad());
	}

	private String dinero(BigDecimal valor) {
		return valor.setScale(2).toPlainString();
	}
}
