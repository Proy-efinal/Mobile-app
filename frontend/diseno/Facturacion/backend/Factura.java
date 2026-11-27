package backend;

import basededatos.Cliente;
import basededatos.CatalogoClientesYArticulos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Facturación: reúne los datos y los cálculos de una sola factura.
public class Factura {
    public static final BigDecimal ITBIS_INICIAL = new BigDecimal("18");
    private static final int DECIMALES_MONEDA = 2;

    private final CatalogoClientesYArticulos catalogoDeClientesYArticulos;
    private final List<Pago> pagosRegistrados = new ArrayList<>();
    private final List<LineaArticulo> articulosFacturados = new ArrayList<>();

    private final LocalDateTime fechaYHoraEmision;
    private Cliente clienteSeleccionado;
    private BigDecimal porcentajeDescuentoAplicado = BigDecimal.ZERO;
    private BigDecimal porcentajeItbisAplicado;

    // Facturación: inicia una factura con su fecha y la tasa de ITBIS asignada.
    Factura(CatalogoClientesYArticulos catalogoDeClientesYArticulos, LocalDateTime fechaYHoraEmision,
            BigDecimal porcentajeItbisInicial) {
        this.catalogoDeClientesYArticulos = catalogoDeClientesYArticulos;
        this.fechaYHoraEmision = fechaYHoraEmision;
        porcentajeItbisAplicado = porcentajeItbisInicial;
    }

    // Encabezado: devuelve la fecha y hora que tendrá el comprobante.
    public LocalDateTime fecha() {
        return fechaYHoraEmision;
    }

    // Cliente: devuelve la persona o empresa seleccionada para esta factura.
    public Cliente cliente() {
        return clienteSeleccionado;
    }

    // Cliente: asigna el cliente elegido; si no hay selección, deja la factura sin cliente.
    public void setCliente(Cliente clienteElegido) {
        clienteSeleccionado = clienteElegido;
    }

    // Cliente: devuelve los clientes disponibles para elegir en el catálogo.
    public List<Cliente> clientes() {
        return catalogoDeClientesYArticulos.clientes();
    }

    // Cliente: busca por nombre o documento, dando prioridad a las coincidencias exactas.
    public Cliente buscarCliente(String textoIngresado) {
        String textoDeBusqueda = textoIngresado.trim().toLowerCase(Locale.ROOT);
        Cliente primeraCoincidenciaParcial = null;
        for (Cliente clienteDelCatalogo : catalogoDeClientesYArticulos.clientes()) {
            String nombreEnMinusculas = clienteDelCatalogo.nombre().toLowerCase(Locale.ROOT);
            String documentoDelCliente = clienteDelCatalogo.documento();
            if (nombreEnMinusculas.equals(textoDeBusqueda) || documentoDelCliente.equals(textoDeBusqueda)) {
                return clienteDelCatalogo;
            }
            if (primeraCoincidenciaParcial == null
                    && (nombreEnMinusculas.contains(textoDeBusqueda) || documentoDelCliente.contains(textoDeBusqueda))) {
                primeraCoincidenciaParcial = clienteDelCatalogo;
            }
        }
        return primeraCoincidenciaParcial;
    }

    // Artículos: devuelve los códigos que todavía están disponibles en Inventario.
    public List<String> codigosDeArticulos() {
        return catalogoDeClientesYArticulos.codigosDeArticulos();
    }

    // Artículos: entrega una copia de las filas para mostrarlas o guardarlas.
    public List<LineaArticulo> articulos() {
        return List.copyOf(articulosFacturados);
    }

    // Artículos: valida código y cantidad, calcula el ITBIS de la fila y agrega el artículo.
    public LineaArticulo agregarArticulo(String codigoIngresado, String cantidadIngresada) {
        String codigoArticulo = codigoIngresado.trim();
        if (!codigoArticulo.matches("[1-9][0-9]{3}")) {
            throw new IllegalArgumentException("Escribe un código numérico entre 1000 y 9999.");
        }
        CatalogoClientesYArticulos.Articulo articuloDelCatalogo = catalogoDeClientesYArticulos.buscarArticulo(codigoArticulo);
        if (articuloDelCatalogo == null) {
            throw new IllegalArgumentException("No se encontró el artículo. Usa uno de los códigos de prueba.");
        }

        int unidadesFacturadas;
        try {
            unidadesFacturadas = Integer.parseInt(cantidadIngresada.trim());
        } catch (NumberFormatException errorDeCantidad) {
            throw new IllegalArgumentException("Escribe una cantidad entera mayor que cero.");
        }
        if (unidadesFacturadas <= 0) {
            throw new IllegalArgumentException("Escribe una cantidad entera mayor que cero.");
        }

        BigDecimal importeDeLaFila = articuloDelCatalogo.precio().multiply(BigDecimal.valueOf(unidadesFacturadas));
        BigDecimal itbisDeLaFila = calcularItbis(importeDeLaFila);
        LineaArticulo articuloFacturado = new LineaArticulo(codigoArticulo, articuloDelCatalogo.nombre(),
                unidadesFacturadas, itbisDeLaFila, articuloDelCatalogo.precio());
        articulosFacturados.add(articuloFacturado);
        return articuloFacturado;
    }

    // Artículos: quita una fila de esta factura sin eliminar el producto del inventario.
    public void eliminarArticulo(int posicionDeLaFila) {
        articulosFacturados.remove(posicionDeLaFila);
    }

    // Pagos: entrega una copia de los pagos para la tabla y el comprobante.
    public List<Pago> pagos() {
        return List.copyOf(pagosRegistrados);
    }

    // Pagos: verifica que el monto sea válido y registra el método y la cantidad pagada.
    public Pago agregarPago(String metodoDePago, String montoIngresado) {
        String montoConPuntoDecimal = montoIngresado.trim().replace(',', '.');
        if (!montoConPuntoDecimal.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Escribe un monto mayor que cero con hasta dos decimales.");
        }
        BigDecimal importeDelPago = new BigDecimal(montoConPuntoDecimal);
        if (importeDelPago.signum() <= 0) {
            throw new IllegalArgumentException("Escribe un monto mayor que cero.");
        }

        Pago pagoRegistrado = new Pago(metodoDePago, importeDelPago);
        pagosRegistrados.add(pagoRegistrado);
        return pagoRegistrado;
    }

    // Pagos: quita el pago que corresponde a la fila seleccionada.
    public void eliminarPago(int posicionDeLaFila) {
        pagosRegistrados.remove(posicionDeLaFila);
    }

    // Totales: devuelve el porcentaje de descuento seleccionado.
    public BigDecimal descuento() {
        return porcentajeDescuentoAplicado;
    }

    // Totales: guarda el porcentaje de descuento que se usará en la factura.
    public void setDescuento(BigDecimal porcentajeDescuentoElegido) {
        porcentajeDescuentoAplicado = porcentajeDescuentoElegido;
    }

    // Totales: devuelve la tasa de ITBIS con la que se calcula esta factura.
    public BigDecimal porcentajeItbis() {
        return porcentajeItbisAplicado;
    }

    // Totales: aplica una nueva tasa y vuelve a calcular el ITBIS de cada fila del borrador.
    void cambiarItbis(BigDecimal nuevoPorcentajeItbis) {
        if (porcentajeItbisAplicado.compareTo(nuevoPorcentajeItbis) == 0) {
            return;
        }

        porcentajeItbisAplicado = nuevoPorcentajeItbis;
        articulosFacturados.replaceAll(articuloFacturado -> {
            BigDecimal importeDeLaFila = articuloFacturado.precioUnitario()
                    .multiply(BigDecimal.valueOf(articuloFacturado.unidades()));
            return new LineaArticulo(articuloFacturado.codigo(), articuloFacturado.nombre(),
                    articuloFacturado.unidades(), calcularItbis(importeDeLaFila), articuloFacturado.precioUnitario());
        });
    }

    // Totales: suma los artículos, aplica descuento e impuesto y resta lo que ya se pagó.
    public Totales calcularTotales(BigDecimal porcentajeDescuento) {
        BigDecimal subtotalDeArticulos = BigDecimal.ZERO;
        for (LineaArticulo articuloFacturado : articulosFacturados) {
            BigDecimal importeDeLaFila = articuloFacturado.precioUnitario()
                    .multiply(BigDecimal.valueOf(articuloFacturado.unidades()));
            subtotalDeArticulos = subtotalDeArticulos.add(importeDeLaFila);
        }

        BigDecimal montoDelDescuento = redondearMoneda(subtotalDeArticulos.multiply(porcentajeDescuento)
                .movePointLeft(2));
        BigDecimal subtotalConDescuento = subtotalDeArticulos.subtract(montoDelDescuento);
        BigDecimal montoDelItbis = calcularItbis(subtotalConDescuento);
        BigDecimal totalConItbis = subtotalConDescuento.add(montoDelItbis);
        BigDecimal sumaDePagos = BigDecimal.ZERO;
        for (Pago pagoRegistrado : pagosRegistrados) {
            sumaDePagos = sumaDePagos.add(pagoRegistrado.monto());
        }

        BigDecimal balancePendiente = totalConItbis.subtract(sumaDePagos).max(BigDecimal.ZERO);
        return new Totales(montoDelDescuento, montoDelItbis, balancePendiente, totalConItbis);
    }

    // Totales: convierte la tasa porcentual de ITBIS en un valor en pesos.
    private BigDecimal calcularItbis(BigDecimal montoAntesDelItbis) {
        return redondearMoneda(montoAntesDelItbis.multiply(porcentajeItbisAplicado).movePointLeft(2));
    }

    // Totales: deja el importe con dos decimales, como se muestra en el comprobante.
    private static BigDecimal redondearMoneda(BigDecimal importeMonetario) {
        return importeMonetario.setScale(DECIMALES_MONEDA, RoundingMode.HALF_UP);
    }

    // Registro: recupera una fila guardada con su precio histórico sin consultar el catálogo actual.
    void restaurarArticulo(LineaArticulo articuloGuardado) {
        articulosFacturados.add(articuloGuardado);
    }

    // Registro: crea una copia que conserva los datos de la factura al momento de guardarla.
    Factura copia() {
        Factura copiaDeLaFactura = new Factura(catalogoDeClientesYArticulos, fechaYHoraEmision, porcentajeItbisAplicado);
        copiaDeLaFactura.clienteSeleccionado = clienteSeleccionado;
        copiaDeLaFactura.porcentajeDescuentoAplicado = porcentajeDescuentoAplicado;
        copiaDeLaFactura.articulosFacturados.addAll(articulosFacturados);
        copiaDeLaFactura.pagosRegistrados.addAll(pagosRegistrados);
        return copiaDeLaFactura;
    }

    // Registro: compara con la versión guardada para saber si existen cambios pendientes.
    boolean mismoContenido(Factura versionGuardada) {
        return Objects.equals(clienteSeleccionado, versionGuardada.clienteSeleccionado)
                && porcentajeDescuentoAplicado.compareTo(versionGuardada.porcentajeDescuentoAplicado) == 0
                && porcentajeItbisAplicado.compareTo(versionGuardada.porcentajeItbisAplicado) == 0
                && articulosFacturados.equals(versionGuardada.articulosFacturados)
                && pagosRegistrados.equals(versionGuardada.pagosRegistrados);
    }

    // Artículos: agrupa los datos de una fila facturada, incluyendo el precio usado ese día.
    public record LineaArticulo(String codigo, String nombre, int unidades, BigDecimal itbis,
                                BigDecimal precioUnitario) {}

    // Pagos: agrupa el medio de pago y el importe entregado.
    public record Pago(String metodo, BigDecimal monto) {}

    // Totales: reúne el descuento, el impuesto, el saldo pendiente y el total a mostrar.
    public record Totales(BigDecimal descuento, BigDecimal itbis, BigDecimal pendiente, BigDecimal total) {}
}
