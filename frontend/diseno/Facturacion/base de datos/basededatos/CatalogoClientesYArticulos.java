package basededatos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Mantiene los catálogos disponibles mientras la aplicación está abierta. Incluye datos iniciales de ejemplo.
public class CatalogoClientesYArticulos {
    private final List<Cliente> catalogoDeClientes = new ArrayList<>();
    private final Map<String, Articulo> catalogoDeArticulosPorCodigo = new LinkedHashMap<>();

    // Prepara los clientes y artículos de ejemplo con los que se puede probar la aplicación.
    public CatalogoClientesYArticulos() {
        catalogoDeClientes.add(new Cliente("Ana Pérez", "809-555-0101", "000-0000001-0", ""));
        catalogoDeClientes.add(new Cliente("Comercial Duarte", "809-555-0102", "000000002", ""));
        catalogoDeArticulosPorCodigo.put("1000", new Articulo("Cuaderno", new BigDecimal("100.00"), 0));
        catalogoDeArticulosPorCodigo.put("2500", new Articulo("Bolígrafo", new BigDecimal("25.00"), 0));
        catalogoDeArticulosPorCodigo.put("9999", new Articulo("Carpeta", new BigDecimal("75.00"), 0));
    }

    // Entrega una copia de la lista de clientes para mostrarla o buscar en ella.
    public List<Cliente> clientes() {
        return List.copyOf(catalogoDeClientes);
    }

    // Incorpora al catálogo un cliente recuperado de un registro antiguo.
    public void agregarCliente(Cliente clienteRecuperado) {
        catalogoDeClientes.add(clienteRecuperado);
    }

    // Reemplaza la lista de clientes con la versión cargada o guardada.
    public void reemplazarClientes(List<Cliente> clientesActualizados) {
        catalogoDeClientes.clear();
        catalogoDeClientes.addAll(clientesActualizados);
    }

    // Busca en Inventario el artículo que corresponde a un código.
    public Articulo buscarArticulo(String codigoDelArticulo) {
        return catalogoDeArticulosPorCodigo.get(codigoDelArticulo);
    }

    // Entrega los códigos de los artículos que están disponibles en el catálogo.
    public List<String> codigosDeArticulos() {
        return List.copyOf(catalogoDeArticulosPorCodigo.keySet());
    }

    // Agrega un artículo nuevo o reemplaza los datos del artículo que ya usa ese código.
    public void guardarArticulo(String codigoDelArticulo, Articulo datosDelArticulo) {
        catalogoDeArticulosPorCodigo.put(codigoDelArticulo, datosDelArticulo);
    }

    // Retira un artículo del catálogo para que deje de ofrecerse en las facturas nuevas.
    public void eliminarArticulo(String codigoDelArticulo) {
        catalogoDeArticulosPorCodigo.remove(codigoDelArticulo);
    }

    // Agrupa los datos de un artículo del inventario. Su código se guarda en el catálogo.
    public record Articulo(String nombre, BigDecimal precio, int existencia) {}
}
