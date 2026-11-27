package backend;

import basededatos.ArchivoRegistro;
import basededatos.Cliente;
import basededatos.CatalogoClientesYArticulos;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

// Gestión de datos: conecta el registro de facturas, los catálogos y la configuración con el archivo local.
public class RegistroFacturas {
    private final CatalogoClientesYArticulos catalogoDeClientesYArticulos = new CatalogoClientesYArticulos();
    private final ArchivoRegistro archivoDelRegistro;
    private final Properties propiedadesDelRegistro;
    private final Map<TipoFactura, TreeMap<Integer, Factura>> facturasGuardadasPorTipo = new EnumMap<>(TipoFactura.class);
    private final Map<TipoFactura, TreeMap<Integer, Factura>> facturasAbiertasPorTipo = new EnumMap<>(TipoFactura.class);
    private final Map<TipoFactura, Integer> ultimoNumeroConsultadoPorTipo = new EnumMap<>(TipoFactura.class);
    private TipoFactura tipoDeFacturaSeleccionado = TipoFactura.FISCAL;
    private int numeroDeFacturaActual;

    // Inicio: abre el archivo local, recupera catálogos y facturas y muestra el primer tipo.
    public RegistroFacturas(Path rutaDelArchivo) throws IOException {
        archivoDelRegistro = new ArchivoRegistro(rutaDelArchivo);
        propiedadesDelRegistro = archivoDelRegistro.leer();
        cargarClientes();
        cargarFacturasGuardadas();
        cargarInventario();
        seleccionarTipo(tipoDeFacturaSeleccionado);
    }

    // Inicio: organiza las facturas guardadas por tipo y número de comprobante.
    private void cargarFacturasGuardadas() {
        for (TipoFactura tipoDeFactura : TipoFactura.values()) {
            facturasGuardadasPorTipo.put(tipoDeFactura, new TreeMap<>());
            facturasAbiertasPorTipo.put(tipoDeFactura, new TreeMap<>());
            int cantidadDeFacturasGuardadas = leerNumeroEntero(propiedadesDelRegistro, tipoDeFactura.name() + ".cantidad");
            for (int numeroDeFacturaGuardada = 1;
                    numeroDeFacturaGuardada <= cantidadDeFacturasGuardadas; numeroDeFacturaGuardada++) {
                Factura facturaRecuperada = leerFactura(tipoDeFactura, numeroDeFacturaGuardada);
                facturasGuardadasPorTipo.get(tipoDeFactura).put(numeroDeFacturaGuardada, facturaRecuperada);
            }
        }
    }

    // Inventario: recupera los artículos modificados y respeta las eliminaciones guardadas.
    private void cargarInventario() {
        for (String claveDelInventario : propiedadesDelRegistro.stringPropertyNames()) {
            if (claveDelInventario.startsWith("inventario.") && claveDelInventario.endsWith(".eliminado")) {
                catalogoDeClientesYArticulos.eliminarArticulo(claveDelInventario.split("\\.")[1]);
            } else if (claveDelInventario.startsWith("inventario.") && claveDelInventario.endsWith(".nombre")) {
                String codigoDelArticulo = claveDelInventario.split("\\.")[1];
                String prefijoDelArticulo = "inventario." + codigoDelArticulo + ".";
                CatalogoClientesYArticulos.Articulo articuloGuardado = new CatalogoClientesYArticulos.Articulo(
                        propiedadesDelRegistro.getProperty(claveDelInventario),
                        new BigDecimal(propiedadesDelRegistro.getProperty(prefijoDelArticulo + "precio")),
                        leerNumeroEntero(propiedadesDelRegistro, prefijoDelArticulo + "existencia"));
                catalogoDeClientesYArticulos.guardarArticulo(codigoDelArticulo, articuloGuardado);
            }
        }
    }

    // Registro: indica el tipo de factura seleccionado en la ventana.
    public TipoFactura tipoActual() {
        return tipoDeFacturaSeleccionado;
    }

    // Registro: entrega la factura que se está consultando o editando.
    public Factura facturaActual() {
        return facturasAbiertasPorTipo.get(tipoDeFacturaSeleccionado).get(numeroDeFacturaActual);
    }

    // Registro: forma el número del comprobante que se muestra en pantalla.
    public String ncfActual() {
        return tipoDeFacturaSeleccionado.ncf(numeroDeFacturaActual);
    }

    // Registro: cambia de tipo de factura y vuelve a la última que se consultó de ese tipo.
    public void seleccionarTipo(TipoFactura tipoElegido) {
        tipoDeFacturaSeleccionado = tipoElegido;
        TreeMap<Integer, Factura> facturasGuardadasDelTipo = facturasGuardadasPorTipo.get(tipoElegido);
        int ultimoNumeroGuardado = facturasGuardadasDelTipo.isEmpty() ? 1 : facturasGuardadasDelTipo.lastKey();
        int numeroPorAbrir = ultimoNumeroConsultadoPorTipo.getOrDefault(tipoElegido, ultimoNumeroGuardado);
        abrirFacturaPorNumero(numeroPorAbrir);
    }

    // Registro: abre un número existente o prepara un borrador si todavía no se ha guardado.
    private void abrirFacturaPorNumero(int numeroPorAbrir) {
        numeroDeFacturaActual = numeroPorAbrir;
        ultimoNumeroConsultadoPorTipo.put(tipoDeFacturaSeleccionado, numeroPorAbrir);
        TreeMap<Integer, Factura> facturasAbiertasDelTipo = facturasAbiertasPorTipo.get(tipoDeFacturaSeleccionado);
        if (facturasAbiertasDelTipo.containsKey(numeroPorAbrir)) {
            return;
        }

        Factura versionGuardada = facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado).get(numeroPorAbrir);
        Factura facturaPorAbrir;
        if (versionGuardada != null) {
            facturaPorAbrir = versionGuardada.copia();
        } else {
            facturaPorAbrir = new Factura(catalogoDeClientesYArticulos, LocalDateTime.now(),
                    configuracion().porcentajeItbis());
        }
        facturasAbiertasDelTipo.put(numeroPorAbrir, facturaPorAbrir);
    }

    // Registro: revisa si existe una factura anterior dentro del tipo seleccionado.
    public boolean hayAnterior() {
        return facturasAbiertasPorTipo.get(tipoDeFacturaSeleccionado).containsKey(numeroDeFacturaActual - 1)
                || facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado).containsKey(numeroDeFacturaActual - 1);
    }

    // Registro: vuelve a la factura anterior cuando está disponible.
    public void anterior() {
        if (hayAnterior()) {
            abrirFacturaPorNumero(numeroDeFacturaActual - 1);
        }
    }

    // Registro: revisa si existe una factura posterior dentro del tipo seleccionado.
    public boolean haySiguiente() {
        return facturasAbiertasPorTipo.get(tipoDeFacturaSeleccionado).containsKey(numeroDeFacturaActual + 1)
                || facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado).containsKey(numeroDeFacturaActual + 1);
    }

    // Registro: pasa a la factura siguiente cuando está disponible.
    public void siguiente() {
        if (haySiguiente()) {
            abrirFacturaPorNumero(numeroDeFacturaActual + 1);
        }
    }

    // Registro: exige guardar primero y abre el próximo número disponible para este tipo de factura.
    public void nueva() {
        TreeMap<Integer, Factura> facturasGuardadasDelTipo = facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado);
        if (facturasGuardadasDelTipo.isEmpty() || !estaGuardada()) {
            throw new IllegalStateException("Guarda la factura actual antes de abrir una nueva.");
        }

        int siguienteNumeroDisponible = facturasGuardadasDelTipo.lastKey() + 1;
        if (siguienteNumeroDisponible > 99_999_999) {
            throw new IllegalStateException("Se agotó la secuencia de este tipo de factura.");
        }
        abrirFacturaPorNumero(siguienteNumeroDisponible);
    }

    // Registro: comprueba si la factura actual coincide con la última versión guardada.
    public boolean estaGuardada() {
        Factura versionGuardada = facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado).get(numeroDeFacturaActual);
        return versionGuardada != null && facturaActual().mismoContenido(versionGuardada);
    }

    // Registro: guarda en disco una copia de la factura actual y actualiza su secuencia.
    public void guardar() throws IOException {
        Factura copiaParaGuardar = facturaActual().copia();
        Properties propiedadesActualizadas = new Properties();
        propiedadesActualizadas.putAll(propiedadesDelRegistro);
        escribirFactura(propiedadesActualizadas, tipoDeFacturaSeleccionado, numeroDeFacturaActual, copiaParaGuardar);
        String claveDeCantidadDeFacturas = tipoDeFacturaSeleccionado.name() + ".cantidad";
        propiedadesActualizadas.setProperty(claveDeCantidadDeFacturas, Integer.toString(Math.max(numeroDeFacturaActual,
                leerNumeroEntero(propiedadesDelRegistro, claveDeCantidadDeFacturas))));
        guardarCambiosEnArchivo(propiedadesActualizadas);
        facturasGuardadasPorTipo.get(tipoDeFacturaSeleccionado).put(numeroDeFacturaActual, copiaParaGuardar);
    }

    // Clientes: entrega la lista que se muestra en el catálogo.
    public List<Cliente> clientes() {
        return catalogoDeClientesYArticulos.clientes();
    }

    // Clientes: recupera del archivo local la lista de clientes guardada.
    private void cargarClientes() {
        if (!propiedadesDelRegistro.containsKey("clientes.cantidad")) {
            return;
        }
        List<Cliente> clientesGuardados = new ArrayList<>();
        int cantidadDeClientes = leerNumeroEntero(propiedadesDelRegistro, "clientes.cantidad");
        for (int posicionDelCliente = 0; posicionDelCliente < cantidadDeClientes; posicionDelCliente++) {
            clientesGuardados.add(leerCliente("clientes." + posicionDelCliente + "."));
        }
        catalogoDeClientesYArticulos.reemplazarClientes(clientesGuardados);
    }

    // Clientes: valida y guarda un cliente nuevo o reemplaza sus datos en el catálogo.
    public Cliente guardarCliente(Cliente clienteOriginal, String nombreIngresado, String telefonoIngresado,
                                  String documentoIngresado, String notaIngresada) throws IOException {
        if (nombreIngresado.isBlank() || documentoIngresado.isBlank()) {
            throw new IllegalArgumentException("Escribe el nombre y el RNC o cédula.");
        }
        Cliente clienteActualizado = new Cliente(nombreIngresado.trim(), telefonoIngresado.trim(),
                documentoIngresado.trim(), notaIngresada.trim());
        List<Cliente> catalogoActualizado = new ArrayList<>(catalogoDeClientesYArticulos.clientes());
        int posicionDelClienteOriginal = clienteOriginal == null ? -1 : catalogoActualizado.indexOf(clienteOriginal);
        if (clienteOriginal != null && posicionDelClienteOriginal == -1) {
            throw new IllegalArgumentException("El cliente ya no está disponible para editar.");
        }
        for (int posicionRevisada = 0; posicionRevisada < catalogoActualizado.size(); posicionRevisada++) {
            Cliente clienteEnRevision = catalogoActualizado.get(posicionRevisada);
            if (posicionRevisada != posicionDelClienteOriginal
                    && clienteEnRevision.documento().equalsIgnoreCase(clienteActualizado.documento())) {
                throw new IllegalArgumentException("Ya existe un cliente con ese RNC o cédula.");
            }
        }
        if (posicionDelClienteOriginal == -1) {
            catalogoActualizado.add(clienteActualizado);
        } else {
            catalogoActualizado.set(posicionDelClienteOriginal, clienteActualizado);
        }

        guardarCatalogoClientes(catalogoActualizado);

        if (clienteOriginal != null) {
            for (TipoFactura tipoDeFactura : TipoFactura.values()) {
                facturasAbiertasPorTipo.get(tipoDeFactura).forEach((numeroDeFactura, facturaAbierta) -> {
                    if (!facturasGuardadasPorTipo.get(tipoDeFactura).containsKey(numeroDeFactura)
                            && clienteOriginal.equals(facturaAbierta.cliente())) {
                        facturaAbierta.setCliente(clienteActualizado);
                    }
                });
            }
        }
        return clienteActualizado;
    }

    // Clientes: elimina el cliente del catálogo y lo desmarca en los borradores que lo usan.
    public void eliminarCliente(Cliente clienteEliminado) throws IOException {
        List<Cliente> catalogoActualizado = new ArrayList<>(catalogoDeClientesYArticulos.clientes());
        if (!catalogoActualizado.remove(clienteEliminado)) {
            throw new IllegalArgumentException("El cliente ya no está disponible.");
        }
        guardarCatalogoClientes(catalogoActualizado);
        for (TipoFactura tipoDeFactura : TipoFactura.values()) {
            facturasAbiertasPorTipo.get(tipoDeFactura).forEach((numeroDeFactura, facturaAbierta) -> {
                if (!facturasGuardadasPorTipo.get(tipoDeFactura).containsKey(numeroDeFactura)
                            && clienteEliminado.equals(facturaAbierta.cliente())) {
                    facturaAbierta.setCliente(null);
                }
            });
        }
    }

    // Clientes: escribe el catálogo completo en el archivo y lo actualiza en memoria.
    private void guardarCatalogoClientes(List<Cliente> clientesDelCatalogo) throws IOException {
        Properties propiedadesActualizadas = new Properties();
        propiedadesActualizadas.putAll(propiedadesDelRegistro);
        propiedadesActualizadas.stringPropertyNames().stream()
                .filter(claveGuardada -> claveGuardada.startsWith("clientes."))
                .forEach(propiedadesActualizadas::remove);
        propiedadesActualizadas.setProperty("clientes.cantidad", Integer.toString(clientesDelCatalogo.size()));
        for (int posicionDelCliente = 0; posicionDelCliente < clientesDelCatalogo.size(); posicionDelCliente++) {
            String prefijoDelCliente = "clientes." + posicionDelCliente + ".";
            escribirCliente(propiedadesActualizadas, prefijoDelCliente, clientesDelCatalogo.get(posicionDelCliente));
        }
        guardarCambiosEnArchivo(propiedadesActualizadas);
        catalogoDeClientesYArticulos.reemplazarClientes(clientesDelCatalogo);
    }

    // Inventario: entrega los códigos ordenados para mostrarlos en la tabla.
    public List<String> codigosInventario() {
        return catalogoDeClientesYArticulos.codigosDeArticulos().stream().sorted().toList();
    }

    // Inventario: busca un artículo a partir de su código.
    public CatalogoClientesYArticulos.Articulo articulo(String codigoDelArticulo) {
        return catalogoDeClientesYArticulos.buscarArticulo(codigoDelArticulo);
    }

    // Inventario: busca el primer código libre entre 1000 y 9999.
    public String siguienteCodigoArticulo() {
        for (int numeroDeCodigoCandidato = 1000; numeroDeCodigoCandidato <= 9999; numeroDeCodigoCandidato++) {
            String codigoCandidato = Integer.toString(numeroDeCodigoCandidato);
            if (articulo(codigoCandidato) == null) {
                return codigoCandidato;
            }
        }
        throw new IllegalStateException("No quedan códigos de artículo disponibles.");
    }

    // Inventario: valida los datos y guarda un artículo nuevo o modifica uno existente.
    public void guardarArticulo(String codigoDelArticulo, String nombreDelArticulo, String precioIngresado,
                                String existenciaIngresada, boolean esArticuloNuevo) throws IOException {
        codigoDelArticulo = esArticuloNuevo ? siguienteCodigoArticulo() : codigoDelArticulo.trim();
        nombreDelArticulo = nombreDelArticulo.trim();
        String precioConPuntoDecimal = precioIngresado.trim().replace(',', '.');
        String textoDeExistencia = existenciaIngresada.trim();
        if (!codigoDelArticulo.matches("[1-9][0-9]{3}")) {
            throw new IllegalArgumentException("Escribe un código entre 1000 y 9999.");
        }
        if (nombreDelArticulo.isEmpty() || !precioConPuntoDecimal.matches("[0-9]+(\\.[0-9]{1,2})?")
                || !textoDeExistencia.matches("[0-9]+")) {
            throw new IllegalArgumentException("Escribe un nombre, un precio válido y una existencia entera.");
        }
        int cantidadEnExistencia;
        try {
            cantidadEnExistencia = Integer.parseInt(textoDeExistencia);
        } catch (NumberFormatException errorDeCantidad) {
            throw new IllegalArgumentException("La existencia es demasiado alta.");
        }
        CatalogoClientesYArticulos.Articulo articuloParaGuardar = new CatalogoClientesYArticulos.Articulo(nombreDelArticulo,
                new BigDecimal(precioConPuntoDecimal), cantidadEnExistencia);
        String prefijoDelArticulo = "inventario." + codigoDelArticulo + ".";
        Properties propiedadesActualizadas = new Properties();
        propiedadesActualizadas.putAll(propiedadesDelRegistro);
        propiedadesActualizadas.remove(prefijoDelArticulo + "eliminado");
        propiedadesActualizadas.setProperty(prefijoDelArticulo + "nombre", articuloParaGuardar.nombre());
        propiedadesActualizadas.setProperty(prefijoDelArticulo + "precio", articuloParaGuardar.precio().toPlainString());
        propiedadesActualizadas.setProperty(prefijoDelArticulo + "existencia",
                Integer.toString(articuloParaGuardar.existencia()));
        guardarCambiosEnArchivo(propiedadesActualizadas);
        catalogoDeClientesYArticulos.guardarArticulo(codigoDelArticulo, articuloParaGuardar);
    }

    // Inventario: elimina el artículo del catálogo y registra el borrado para futuros reinicios.
    public void eliminarArticulo(String codigoDelArticulo) throws IOException {
        if (articulo(codigoDelArticulo) == null) {
            throw new IllegalArgumentException("El artículo ya no está disponible.");
        }
        String prefijoDelArticulo = "inventario." + codigoDelArticulo + ".";
        Properties propiedadesActualizadas = new Properties();
        propiedadesActualizadas.putAll(propiedadesDelRegistro);
        propiedadesActualizadas.stringPropertyNames().stream()
                .filter(claveGuardada -> claveGuardada.startsWith(prefijoDelArticulo))
                .forEach(propiedadesActualizadas::remove);
        propiedadesActualizadas.setProperty(prefijoDelArticulo + "eliminado", "true");
        guardarCambiosEnArchivo(propiedadesActualizadas);
        catalogoDeClientesYArticulos.eliminarArticulo(codigoDelArticulo);
    }

    // Configuración: lee el nombre de la empresa, dirección, RNC y tasa de ITBIS.
    public Configuracion configuracion() {
        return new Configuracion(propiedadesDelRegistro.getProperty("config.empresa", ""),
                propiedadesDelRegistro.getProperty("config.direccion", ""), propiedadesDelRegistro.getProperty("config.rnc", ""),
                new BigDecimal(propiedadesDelRegistro.getProperty("config.itbis", Factura.ITBIS_INICIAL.toPlainString())));
    }

    // Configuración: valida y guarda los ajustes y cambia el impuesto solo en los borradores.
    public void guardarConfiguracion(String nombreDeLaEmpresa, String direccionDeLaEmpresa,
                                     String rncDeLaEmpresa, String porcentajeItbisIngresado) throws IOException {
        String porcentajeConPuntoDecimal = porcentajeItbisIngresado.trim().replace(',', '.');
        if (!porcentajeConPuntoDecimal.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Escribe un ITBIS válido entre 0% y 100%, con hasta dos decimales.");
        }
        BigDecimal porcentajeItbisValidado = new BigDecimal(porcentajeConPuntoDecimal).stripTrailingZeros();
        if (porcentajeItbisValidado.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El ITBIS debe estar entre 0% y 100%.");
        }

        Properties propiedadesActualizadas = new Properties();
        propiedadesActualizadas.putAll(propiedadesDelRegistro);
        propiedadesActualizadas.setProperty("config.empresa", nombreDeLaEmpresa.trim());
        propiedadesActualizadas.setProperty("config.direccion", direccionDeLaEmpresa.trim());
        propiedadesActualizadas.setProperty("config.rnc", rncDeLaEmpresa.trim());
        propiedadesActualizadas.setProperty("config.itbis", porcentajeItbisValidado.toPlainString());
        guardarCambiosEnArchivo(propiedadesActualizadas);

        // Las facturas todavía sin guardar adoptan la tasa nueva.
        for (TipoFactura tipoDeFactura : TipoFactura.values()) {
            facturasAbiertasPorTipo.get(tipoDeFactura).forEach((numeroDeFactura, facturaAbierta) -> {
                if (!facturasGuardadasPorTipo.get(tipoDeFactura).containsKey(numeroDeFactura)) {
                    facturaAbierta.cambiarItbis(porcentajeItbisValidado);
                }
            });
        }
    }

    // Configuración: reúne los datos de la empresa y el porcentaje fijo de ITBIS.
    public record Configuracion(String empresa, String direccion, String rnc, BigDecimal porcentajeItbis) {}

    // Almacenamiento: escribe los cambios en el archivo antes de reemplazar la copia en memoria.
    private void guardarCambiosEnArchivo(Properties propiedadesActualizadas) throws IOException {
        archivoDelRegistro.guardar(propiedadesActualizadas);
        propiedadesDelRegistro.clear();
        propiedadesDelRegistro.putAll(propiedadesActualizadas);
    }

    // Almacenamiento: convierte un valor guardado a entero y usa cero si no existe.
    private static int leerNumeroEntero(Properties propiedadesOrigen, String claveGuardada) {
        return Integer.parseInt(propiedadesOrigen.getProperty(claveGuardada, "0"));
    }

    // Almacenamiento: forma la clave inicial que distingue una factura de las demás en el archivo.
    private static String construirPrefijoDeFactura(TipoFactura tipoDeFactura, int numeroDeFactura) {
        return tipoDeFactura.name() + "." + numeroDeFactura + ".";
    }

    // Almacenamiento: reconstruye los datos de un cliente a partir de las claves guardadas.
    private Cliente leerCliente(String prefijoDelCliente) {
        return new Cliente(propiedadesDelRegistro.getProperty(prefijoDelCliente + "nombre"),
                propiedadesDelRegistro.getProperty(prefijoDelCliente + "telefono"),
                propiedadesDelRegistro.getProperty(prefijoDelCliente + "documento"),
                propiedadesDelRegistro.getProperty(prefijoDelCliente + "nota"));
    }

    // Almacenamiento: prepara las cuatro claves de un cliente para guardar sus datos.
    private static void escribirCliente(Properties propiedadesDestino, String prefijoDelCliente,
                                        Cliente clienteParaGuardar) {
        propiedadesDestino.setProperty(prefijoDelCliente + "nombre", clienteParaGuardar.nombre());
        propiedadesDestino.setProperty(prefijoDelCliente + "telefono", clienteParaGuardar.telefono());
        propiedadesDestino.setProperty(prefijoDelCliente + "documento", clienteParaGuardar.documento());
        propiedadesDestino.setProperty(prefijoDelCliente + "nota", clienteParaGuardar.nota());
    }

    // Almacenamiento: recupera una factura con su cliente, artículos, pagos e importes históricos.
    private Factura leerFactura(TipoFactura tipoDeFactura, int numeroDeFactura) {
        String prefijoDeLaFactura = construirPrefijoDeFactura(tipoDeFactura, numeroDeFactura);
        BigDecimal porcentajeItbisGuardado = new BigDecimal(propiedadesDelRegistro.getProperty(prefijoDeLaFactura + "itbis",
                Factura.ITBIS_INICIAL.toPlainString()));
        LocalDateTime fechaDeLaFactura = LocalDateTime.parse(propiedadesDelRegistro.getProperty(prefijoDeLaFactura + "fecha"));
        Factura facturaRecuperada = new Factura(catalogoDeClientesYArticulos, fechaDeLaFactura, porcentajeItbisGuardado);
        facturaRecuperada.setDescuento(new BigDecimal(propiedadesDelRegistro.getProperty(prefijoDeLaFactura + "descuento")));
        if (propiedadesDelRegistro.containsKey(prefijoDeLaFactura + "cliente.nombre")) {
            Cliente clienteDeLaFactura = leerCliente(prefijoDeLaFactura + "cliente.");
            facturaRecuperada.setCliente(clienteDeLaFactura);
            if (!propiedadesDelRegistro.containsKey("clientes.cantidad") && clientes().stream().noneMatch(
                    clienteDelCatalogo -> clienteDelCatalogo.documento().equalsIgnoreCase(clienteDeLaFactura.documento()))) {
                catalogoDeClientesYArticulos.agregarCliente(clienteDeLaFactura);
            }
        }
        int cantidadDeArticulos = leerNumeroEntero(propiedadesDelRegistro, prefijoDeLaFactura + "articulos");
        for (int posicionGuardada = 0; posicionGuardada < cantidadDeArticulos; posicionGuardada++) {
            String prefijoDelArticulo = prefijoDeLaFactura + "articulo." + posicionGuardada + ".";
            facturaRecuperada.restaurarArticulo(new Factura.LineaArticulo(
                    propiedadesDelRegistro.getProperty(prefijoDelArticulo + "codigo"),
                    propiedadesDelRegistro.getProperty(prefijoDelArticulo + "nombre"),
                    leerNumeroEntero(propiedadesDelRegistro, prefijoDelArticulo + "unidades"),
                    new BigDecimal(propiedadesDelRegistro.getProperty(prefijoDelArticulo + "itbis")),
                    new BigDecimal(propiedadesDelRegistro.getProperty(prefijoDelArticulo + "precio"))));
        }
        int cantidadDePagos = leerNumeroEntero(propiedadesDelRegistro, prefijoDeLaFactura + "pagos");
        for (int posicionGuardada = 0; posicionGuardada < cantidadDePagos; posicionGuardada++) {
            String prefijoDelPago = prefijoDeLaFactura + "pago." + posicionGuardada + ".";
            facturaRecuperada.agregarPago(propiedadesDelRegistro.getProperty(prefijoDelPago + "metodo"),
                    propiedadesDelRegistro.getProperty(prefijoDelPago + "monto"));
        }
        return facturaRecuperada;
    }

    // Almacenamiento: prepara los campos de la factura para guardarlos bajo su tipo y número.
    private void escribirFactura(Properties propiedadesDestino, TipoFactura tipoDeFactura,
                                 int numeroDeFactura, Factura facturaParaGuardar) {
        String prefijoDeLaFactura = construirPrefijoDeFactura(tipoDeFactura, numeroDeFactura);
        propiedadesDestino.stringPropertyNames().stream()
                .filter(claveGuardada -> claveGuardada.startsWith(prefijoDeLaFactura))
                .forEach(propiedadesDestino::remove);
        propiedadesDestino.setProperty(prefijoDeLaFactura + "fecha", facturaParaGuardar.fecha().toString());
        propiedadesDestino.setProperty(prefijoDeLaFactura + "descuento", facturaParaGuardar.descuento().toPlainString());
        propiedadesDestino.setProperty(prefijoDeLaFactura + "itbis", facturaParaGuardar.porcentajeItbis().toPlainString());
        Cliente clienteDeLaFactura = facturaParaGuardar.cliente();
        if (clienteDeLaFactura != null) {
            escribirCliente(propiedadesDestino, prefijoDeLaFactura + "cliente.", clienteDeLaFactura);
        }
        int posicionGuardada = 0;
        for (Factura.LineaArticulo articuloFacturado : facturaParaGuardar.articulos()) {
            String prefijoDeLaFila = prefijoDeLaFactura + "articulo." + posicionGuardada++ + ".";
            propiedadesDestino.setProperty(prefijoDeLaFila + "codigo", articuloFacturado.codigo());
            propiedadesDestino.setProperty(prefijoDeLaFila + "nombre", articuloFacturado.nombre());
            propiedadesDestino.setProperty(prefijoDeLaFila + "unidades", Integer.toString(articuloFacturado.unidades()));
            propiedadesDestino.setProperty(prefijoDeLaFila + "itbis", articuloFacturado.itbis().toPlainString());
            propiedadesDestino.setProperty(prefijoDeLaFila + "precio", articuloFacturado.precioUnitario().toPlainString());
        }
        propiedadesDestino.setProperty(prefijoDeLaFactura + "articulos", Integer.toString(posicionGuardada));

        posicionGuardada = 0;
        for (Factura.Pago pagoRegistrado : facturaParaGuardar.pagos()) {
            String prefijoDeLaFila = prefijoDeLaFactura + "pago." + posicionGuardada++ + ".";
            propiedadesDestino.setProperty(prefijoDeLaFila + "metodo", pagoRegistrado.metodo());
            propiedadesDestino.setProperty(prefijoDeLaFila + "monto", pagoRegistrado.monto().toPlainString());
        }
        propiedadesDestino.setProperty(prefijoDeLaFactura + "pagos", Integer.toString(posicionGuardada));
    }
}
