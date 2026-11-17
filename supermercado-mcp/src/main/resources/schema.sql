CREATE DATABASE IF NOT EXISTS supermercado_mcp
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE supermercado_mcp;

CREATE TABLE IF NOT EXISTS producto (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(255) NOT NULL,
  precio DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL,
  unidad VARCHAR(50) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedido (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cliente VARCHAR(255) NOT NULL,
  direccion VARCHAR(255) NOT NULL,
  telefono VARCHAR(50) NULL,
  estado VARCHAR(30) NOT NULL,
  fecha VARCHAR(20) NOT NULL,
  version BIGINT DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS item_pedido (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pedido_id BIGINT NOT NULL,
  producto_id BIGINT NOT NULL,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
  CONSTRAINT fk_item_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO producto (nombre, precio, stock, unidad)
SELECT * FROM (
  SELECT 'Leche entera' AS nombre, 4.50 AS precio, 40 AS stock, 'litro' AS unidad
  UNION ALL SELECT 'Pan tajado', 3.20, 30, 'unidad'
  UNION ALL SELECT 'Arroz', 5.80, 50, 'libra'
  UNION ALL SELECT 'Huevos', 8.90, 25, 'docena'
  UNION ALL SELECT 'Aceite', 12.40, 20, 'litro'
  UNION ALL SELECT 'Azucar', 3.70, 35, 'libra'
) AS semilla
WHERE NOT EXISTS (SELECT 1 FROM producto);
