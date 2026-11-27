package frontend.diseno;

import backend.Factura;
import backend.TipoFactura;
import basededatos.Cliente;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

// Prepara la versión imprimible de la factura en papel tamaño carta.
final class PdfFactura {
    private static final Charset CODIFICACION_DEL_PDF = Charset.forName("windows-1252");
    private static final FontRenderContext CONTEXTO_PARA_MEDIR_LETRAS = new FontRenderContext(null, true, true);
    private static final DateTimeFormatter FORMATO_DE_FECHA_Y_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int BORDE_IZQUIERDO_DEL_CONTENIDO = 48;
    private static final int BORDE_DERECHO_DEL_CONTENIDO = 564;
    private static final int LIMITE_INFERIOR_DEL_CONTENIDO = 64;

    private final String numeroDeComprobante;
    private final List<StringBuilder> paginasDelComprobante = new ArrayList<>();
    private StringBuilder instruccionesDeLaPaginaActual;
    private int alturaActualDelContenido;

    // Crea una plantilla asociada al número de comprobante que se imprimirá.
    private PdfFactura(String numeroDeComprobante) {
        this.numeroDeComprobante = numeroDeComprobante;
    }

    // Recibe los datos de la factura y genera el archivo PDF en la ruta indicada.
    static Path generar(Path rutaDelPdf, String numeroDeComprobante, Factura facturaSeleccionada,
                        String nombreDeLaEmpresa, String direccionDeLaEmpresa, String rncDeLaEmpresa)
            throws IOException {
        PdfFactura plantillaDelComprobante = new PdfFactura(numeroDeComprobante);
        plantillaDelComprobante.prepararContenidoDelComprobante(facturaSeleccionada, nombreDeLaEmpresa, direccionDeLaEmpresa, rncDeLaEmpresa);
        plantillaDelComprobante.escribirArchivoPdf(rutaDelPdf);
        return rutaDelPdf;
    }

    // Coloca los datos en el orden en que se leen, desde la empresa hasta el importe final.
    private void prepararContenidoDelComprobante(Factura facturaSeleccionada, String nombreDeLaEmpresa,
                                                 String direccionDeLaEmpresa, String rncDeLaEmpresa) {
        // Encabezado con los datos de empresa y de comprobante.
        iniciarPaginaDelComprobante();
        escribirTextoEnVariasLineas(nombreDeLaEmpresa.isBlank() ? "Factura" : nombreDeLaEmpresa, 22, true);
        escribirTextoEnVariasLineas("RNC: " + mostrarGuionSiFaltaElDato(rncDeLaEmpresa), 10, false);
        escribirTextoEnVariasLineas("Dirección: " + mostrarGuionSiFaltaElDato(direccionDeLaEmpresa), 10, false);
        alturaActualDelContenido -= 10;
        escribirTextoEnVariasLineas(obtenerNombreDelTipoDeComprobante(), 15, true);
        escribirTextoEnVariasLineas("Fecha y hora: " + facturaSeleccionada.fecha().format(FORMATO_DE_FECHA_Y_HORA), 10, false);
        alturaActualDelContenido -= 12;

        // Datos de identificación del cliente.
        escribirTituloDeApartado("Cliente", 36);
        Cliente cliente = facturaSeleccionada.cliente();
        if (cliente == null) {
            escribirTextoEnVariasLineas("Sin cliente seleccionado", 10, false);
        } else {
            escribirTextoEnVariasLineas("Nombre: " + cliente.nombre(), 10, false);
            escribirTextoEnVariasLineas("RNC / Cédula: " + mostrarGuionSiFaltaElDato(cliente.documento()), 10, false);
            if (!cliente.telefono().isBlank()) {
                escribirTextoEnVariasLineas("Teléfono: " + cliente.telefono(), 10, false);
            }
        }
        alturaActualDelContenido -= 12;

        // Artículos de la factura.
        escribirCabeceraDeLaTablaDeArticulos();
        if (facturaSeleccionada.articulos().isEmpty()) {
            escribirTextoEnVariasLineas("Sin artículos", 10, false);
        }
        for (Factura.LineaArticulo articulo : facturaSeleccionada.articulos()) {
            escribirFilaDeArticulo(articulo);
        }
        alturaActualDelContenido -= 14;

        // Pagos registrados para el comprobante.
        escribirTituloDeApartado("Pagos", 34);
        if (facturaSeleccionada.pagos().isEmpty()) {
            escribirTextoEnVariasLineas("Sin pagos registrados", 10, false);
        }
        for (Factura.Pago pago : facturaSeleccionada.pagos()) {
            reservarEspacioEnPagina(16);
            escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO + 8, alturaActualDelContenido, 10, false, pago.metodo());
            escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO - 8, alturaActualDelContenido, 10, false, Dinero.formatearPesosDominicanos(pago.monto()));
            alturaActualDelContenido -= 16;
        }
        alturaActualDelContenido -= 16;

        // Descuento, impuesto, balance y total final.
        escribirTituloDeApartado("Totales", 108);
        Factura.Totales totales = facturaSeleccionada.calcularTotales(facturaSeleccionada.descuento());
        escribirFilaDeTotales("Descuento (" + formatearPorcentaje(facturaSeleccionada.descuento()) + ")", totales.descuento(), false);
        escribirFilaDeTotales("ITBIS (" + formatearPorcentaje(facturaSeleccionada.porcentajeItbis()) + ")", totales.itbis(), false);
        escribirFilaDeTotales("Balance pendiente", totales.pendiente(), false);
        alturaActualDelContenido -= 4;
        dibujarSeparadorNegro(alturaActualDelContenido + 13);
        escribirFilaDeTotales("TOTAL FACTURADO", totales.total(), true);
    }

    // Muestra los nombres de las columnas al comenzar la tabla o continuarla en otra página.
    private void escribirCabeceraDeLaTablaDeArticulos() {
        escribirTituloDeApartado("Artículos", 57);
        escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO + 8, alturaActualDelContenido, 9, true, "Código");
        escribirTexto(118, alturaActualDelContenido, 9, true, "Nombre del artículo");
        escribirTextoAlineadoALaDerecha(359, alturaActualDelContenido, 9, true, "Unidades");
        escribirTextoAlineadoALaDerecha(451, alturaActualDelContenido, 9, true, "ITBIS");
        escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO - 8, alturaActualDelContenido, 9, true, "Precio unitario");
        dibujarSeparadorNegro(alturaActualDelContenido - 6);
        alturaActualDelContenido -= 18;
    }

    // Coloca un artículo en la tabla y divide su nombre si no cabe en una línea.
    private void escribirFilaDeArticulo(Factura.LineaArticulo articulo) {
        boolean esPrimeraLineaDelArticulo = true;
        for (String lineaDeTexto : dividirTextoParaQueQuepa(articulo.nombre(), 200, 10, false)) {
            if (alturaActualDelContenido - 15 < LIMITE_INFERIOR_DEL_CONTENIDO) {
                iniciarPaginaDelComprobante();
                escribirCabeceraDeLaTablaDeArticulos();
            }
            escribirTexto(118, alturaActualDelContenido, 10, false, lineaDeTexto);
            if (esPrimeraLineaDelArticulo) {
                escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO + 8, alturaActualDelContenido, 10, false, articulo.codigo());
                escribirTextoAlineadoALaDerecha(359, alturaActualDelContenido, 10, false, Integer.toString(articulo.unidades()));
                escribirTextoAlineadoALaDerecha(451, alturaActualDelContenido, 10, false, Dinero.formatearPesosDominicanos(articulo.itbis()));
                escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO - 8, alturaActualDelContenido, 10, false, Dinero.formatearPesosDominicanos(articulo.precioUnitario()));
                esPrimeraLineaDelArticulo = false;
            }
            alturaActualDelContenido -= 14;
        }
        dibujarSeparadorNegro(alturaActualDelContenido + 5);
        alturaActualDelContenido -= 6;
    }

    // Alinea la descripción del importe a la izquierda y su valor a la derecha.
    private void escribirFilaDeTotales(String etiqueta, BigDecimal monto, boolean destacado) {
        int tamanoDeLetra = destacado ? 13 : 10;
        escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO + 8, alturaActualDelContenido, tamanoDeLetra, destacado, etiqueta);
        escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO - 8, alturaActualDelContenido, tamanoDeLetra, destacado, Dinero.formatearPesosDominicanos(monto));
        alturaActualDelContenido -= 18;
    }

    // Abre una página nueva con el encabezado y el número de comprobante.
    private void iniciarPaginaDelComprobante() {
        instruccionesDeLaPaginaActual = new StringBuilder();
        paginasDelComprobante.add(instruccionesDeLaPaginaActual);
        escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO, 760, 10, true, "FACTURACIÓN");
        escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO, 760, 10, true, "NCF de ejemplo: " + numeroDeComprobante);
        dibujarSeparadorNegro(746);
        alturaActualDelContenido = 710;
    }

    // Abre otra página cuando lo siguiente no cabe antes del pie de página.
    private void reservarEspacioEnPagina(int alturaNecesaria) {
        if (alturaActualDelContenido - alturaNecesaria < LIMITE_INFERIOR_DEL_CONTENIDO) {
            iniciarPaginaDelComprobante();
        }
    }

    // Coloca la línea negra por encima del título de cada apartado.
    private void escribirTituloDeApartado(String titulo, int alturaMinimaDelApartado) {
        reservarEspacioEnPagina(alturaMinimaDelApartado);

        dibujarSeparadorNegro(alturaActualDelContenido + 14);
        escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO + 8, alturaActualDelContenido, 11, true, titulo);
        alturaActualDelContenido -= 25;
    }

    // Escribe un dato largo en varias líneas para mantenerlo dentro de los márgenes.
    private void escribirTextoEnVariasLineas(String contenidoDelTexto, int tamanoDeLetra, boolean usarNegrita) {
        for (String lineaDeTexto : dividirTextoParaQueQuepa(contenidoDelTexto, BORDE_DERECHO_DEL_CONTENIDO - BORDE_IZQUIERDO_DEL_CONTENIDO - 8, tamanoDeLetra, usarNegrita)) {
            reservarEspacioEnPagina(tamanoDeLetra + 5);
            escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO, alturaActualDelContenido, tamanoDeLetra, usarNegrita, lineaDeTexto);
            alturaActualDelContenido -= tamanoDeLetra + 5;
        }
    }

    // Corta el texto por espacios cuando cabe una palabra completa, o por letras si es necesario.
    private static List<String> dividirTextoParaQueQuepa(String contenidoDelTexto, int anchoMaximo, int tamanoDeLetra, boolean usarNegrita) {
        List<String> lineasQueCaben = new ArrayList<>();
        for (String parrafo : convertirATextoImprimible(contenidoDelTexto).split("\\R", -1)) {
            String textoPendiente = parrafo.strip();
            while (medirAnchoDelTexto(textoPendiente, tamanoDeLetra, usarNegrita) > anchoMaximo) {
                int posicionDelCorte = 1;
                while (posicionDelCorte < textoPendiente.length() && medirAnchoDelTexto(textoPendiente.substring(0, posicionDelCorte + 1), tamanoDeLetra, usarNegrita) <= anchoMaximo) {
                    posicionDelCorte++;
                }
                int posicionDelUltimoEspacio = textoPendiente.lastIndexOf(' ', posicionDelCorte);
                if (posicionDelUltimoEspacio > 0) {
                    posicionDelCorte = posicionDelUltimoEspacio;
                }
                lineasQueCaben.add(textoPendiente.substring(0, posicionDelCorte));
                textoPendiente = textoPendiente.substring(posicionDelCorte).stripLeading();
            }
            lineasQueCaben.add(textoPendiente);
        }
        return lineasQueCaben;
    }

    // Calcula cuánto espacio ocupa el texto con el tamaño de letra indicado.
    private static float medirAnchoDelTexto(String textoPorMedir, int tamanoDeLetra, boolean usarNegrita) {
        Font fuenteParaMedir = new Font("Arial", usarNegrita ? Font.BOLD : Font.PLAIN, tamanoDeLetra);
        return (float) fuenteParaMedir.getStringBounds(textoPorMedir, CONTEXTO_PARA_MEDIR_LETRAS).getWidth();
    }

    // Calcula dónde empieza el texto para que los importes terminen en la misma posición.
    private void escribirTextoAlineadoALaDerecha(int posicionDelBordeDerecho, int alturaEnLaPagina, int tamanoDeLetra, boolean usarNegrita, String valorMostrado) {
        String textoImprimible = convertirATextoImprimible(valorMostrado);
        int anchoDelTexto = (int) Math.ceil(medirAnchoDelTexto(textoImprimible, tamanoDeLetra, usarNegrita));
        int posicionInicialDelTexto = posicionDelBordeDerecho - anchoDelTexto;
        escribirTexto(posicionInicialDelTexto, alturaEnLaPagina, tamanoDeLetra, usarNegrita, valorMostrado);
    }

    // Añade el texto a la página en la posición y el tamaño indicados.
    private void escribirTexto(int posicionHorizontal, int alturaEnLaPagina, int tamanoDeLetra, boolean usarNegrita, String valorMostrado) {
        String textoEnHexadecimal = HexFormat.of().formatHex(valorMostrado.getBytes(CODIFICACION_DEL_PDF));
        instruccionesDeLaPaginaActual.append("BT /").append(usarNegrita ? "F2" : "F1")
                .append(' ').append(tamanoDeLetra).append(" Tf 1 0 0 1 ").append(posicionHorizontal).append(' ').append(alturaEnLaPagina)
                .append(" Tm <").append(textoEnHexadecimal).append("> Tj ET\n");
    }

    // Dibuja una línea delgada de margen a margen.
    private void dibujarSeparadorNegro(int alturaEnLaPagina) {
        instruccionesDeLaPaginaActual.append("0 G 0.45 w ").append(BORDE_IZQUIERDO_DEL_CONTENIDO).append(' ').append(alturaEnLaPagina)
                .append(" m ").append(BORDE_DERECHO_DEL_CONTENIDO).append(' ').append(alturaEnLaPagina).append(" l S\n");
    }

    // Identifica el nombre del comprobante a partir de los primeros caracteres del NCF.
    private String obtenerNombreDelTipoDeComprobante() {
        for (TipoFactura tipo : TipoFactura.values()) {
            if (numeroDeComprobante.startsWith(tipo.ncf(1).substring(0, 3))) {
                return tipo.toString().substring(6);
            }
        }
        return "Comprobante";
    }

    // Adapta los caracteres a la codificación utilizada por la fuente del PDF.
    private static String convertirATextoImprimible(String textoOriginal) {
        return new String(textoOriginal.getBytes(CODIFICACION_DEL_PDF), CODIFICACION_DEL_PDF);
    }

    // Muestra un guion cuando un dato de la empresa o del cliente está vacío.
    private static String mostrarGuionSiFaltaElDato(String datoDeLaFactura) {
        return datoDeLaFactura.isBlank() ? "-" : datoDeLaFactura;
    }

    // Muestra el porcentaje sin ceros decimales innecesarios.
    private static String formatearPorcentaje(BigDecimal valorMostrado) {
        return valorMostrado.stripTrailingZeros().toPlainString() + "%";
    }

    // Añade el pie de página y organiza los bloques que necesita el visor para abrir el PDF.
    private void escribirArchivoPdf(Path rutaDelPdf) throws IOException {
        List<String> bloquesInternosDelPdf = new ArrayList<>();
        bloquesInternosDelPdf.add("<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder referenciasDeLasPaginas = new StringBuilder();
        for (int indiceDelBloque = 0; indiceDelBloque < paginasDelComprobante.size(); indiceDelBloque++) {
            referenciasDeLasPaginas.append(5 + indiceDelBloque * 2).append(" 0 R ");
        }
        bloquesInternosDelPdf.add("<< /Type /Pages /Count " + paginasDelComprobante.size() + " /Kids [" + referenciasDeLasPaginas + "] >>");
        bloquesInternosDelPdf.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        bloquesInternosDelPdf.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        for (int indiceDelBloque = 0; indiceDelBloque < paginasDelComprobante.size(); indiceDelBloque++) {
            instruccionesDeLaPaginaActual = paginasDelComprobante.get(indiceDelBloque);
            escribirTexto(BORDE_IZQUIERDO_DEL_CONTENIDO, 28, 9, true, "PROTOTIPO · SIN VALIDEZ FISCAL");
            escribirTextoAlineadoALaDerecha(BORDE_DERECHO_DEL_CONTENIDO, 28, 9, false, "Página " + (indiceDelBloque + 1) + " de " + paginasDelComprobante.size());

            int referenciaDelContenido = 6 + indiceDelBloque * 2;
            bloquesInternosDelPdf.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + referenciaDelContenido + " 0 R >>");
            bloquesInternosDelPdf.add("<< /Length " + instruccionesDeLaPaginaActual.length() + " >>\nstream\n" + instruccionesDeLaPaginaActual + "endstream");
        }

        ByteArrayOutputStream bytesDelArchivoPdf = new ByteArrayOutputStream();
        escribirTextoAscii(bytesDelArchivoPdf, "%PDF-1.4\n");
        List<Integer> posicionesDeLosBloques = new ArrayList<>();
        for (int indiceDelBloque = 0; indiceDelBloque < bloquesInternosDelPdf.size(); indiceDelBloque++) {
            posicionesDeLosBloques.add(bytesDelArchivoPdf.size());
            escribirTextoAscii(bytesDelArchivoPdf, (indiceDelBloque + 1) + " 0 obj\n" + bloquesInternosDelPdf.get(indiceDelBloque) + "\nendobj\n");
        }

        int posicionDeLaTablaDeReferencias = bytesDelArchivoPdf.size();
        escribirTextoAscii(bytesDelArchivoPdf, "xref\n0 " + (bloquesInternosDelPdf.size() + 1) + "\n0000000000 65535 f \n");
        for (int posicionDelBloque : posicionesDeLosBloques) {
            escribirTextoAscii(bytesDelArchivoPdf, String.format(Locale.ROOT, "%010d 00000 n \n", posicionDelBloque));
        }
        escribirTextoAscii(bytesDelArchivoPdf, "trailer\n<< /Size " + (bloquesInternosDelPdf.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + posicionDeLaTablaDeReferencias + "\n%%EOF\n");
        Files.write(rutaDelPdf, bytesDelArchivoPdf.toByteArray());
    }

    // Convierte una instrucción interna del PDF en los bytes que se guardan en el archivo.
    private static void escribirTextoAscii(ByteArrayOutputStream archivoEnConstruccion, String instruccionDelPdf) {
        archivoEnConstruccion.writeBytes(instruccionDelPdf.getBytes(StandardCharsets.US_ASCII));
    }
}
