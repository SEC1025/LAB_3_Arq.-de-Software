package com.udea.tarea_mcp.service;

import java.util.List;

import com.udea.tarea_mcp.model.Tarea;
import com.udea.tarea_mcp.repository.TareaRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TareaToolsService {

	private final TareaRepository repo;

	public TareaToolsService(TareaRepository repo) {
		this.repo = repo;
	}

	@Tool(description = "Devuelve la lista completa de tareas almacenadas.")
	public List<Tarea> listarTareas() {
		return repo.findAll();
	}

	@Tool(description = "Devuelve únicamente las tareas pendientes.")
	public List<Tarea> listarPendientes() {
		return repo.findByCompletado(false);
	}

	@Tool(description = "Busca tareas cuyo título contiene el fragmento indicado.")
	public List<Tarea> buscarTareas(
			@ToolParam(description = "Fragmento de texto a buscar en el título") String fragmento) {
		return repo.findByTituloContainingIgnoreCase(fragmento);
	}

	@Tool(description = "Crea una nueva tarea con título y fecha; queda pendiente.")
	@Transactional
	public Tarea crearTarea(
			@ToolParam(description = "Título descriptivo de la tarea") String titulo,
			@ToolParam(description = "Fecha, por ejemplo 2026-11-20") String fecha) {
		if (titulo == null || titulo.isBlank()) {
			throw new IllegalArgumentException("El título es obligatorio");
		}
		return repo.save(new Tarea(titulo.trim(), fecha, false));
	}

	@Tool(description = "Marca como completada la tarea indicada por su id.")
	@Transactional
	public Tarea marcarCompletada(
			@ToolParam(description = "Identificador único de la tarea") Long id) {
		Tarea tarea = repo.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("No existe la tarea " + id));
		tarea.setCompletado(true);
		return repo.save(tarea);
	}

	@Tool(description = "Actualiza el título o fecha de una tarea existente.")
	@Transactional
	public Tarea actualizarTarea(
			@ToolParam(description = "Id de la tarea") Long id,
			@ToolParam(description = "Nuevo título; null para no cambiar") String titulo,
			@ToolParam(description = "Nueva fecha; null para no cambiar") String fecha) {
		Tarea tarea = repo.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("No existe la tarea " + id));
		if (titulo != null && !titulo.isBlank()) {
			tarea.setTitulo(titulo.trim());
		}
		if (fecha != null && !fecha.isBlank()) {
			tarea.setFecha(fecha.trim());
		}
		return repo.save(tarea);
	}

	@Tool(description = "Elimina la tarea con el id indicado.")
	@Transactional
	public String eliminarTarea(
			@ToolParam(description = "Id de la tarea a eliminar") Long id) {
		if (!repo.existsById(id)) {
			return "La tarea " + id + " no existe.";
		}
		repo.deleteById(id);
		return "Tarea " + id + " eliminada correctamente.";
	}
}
