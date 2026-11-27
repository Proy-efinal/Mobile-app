package frontend.diseno;

import backend.Factura;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

// En Facturación, muestra los artículos y permite agregar o quitar líneas de la factura.
class PanelArticulos extends JPanel {
    private final JTextField campoCodigoArticulo = new JTextField(9);
    private final JTextField campoCantidadUnidades = new JTextField(5);
    private final ModeloTabla modeloTablaArticulos = new ModeloTabla(
            "Seleccionar", "Código", "Nombre del artículo", "Unidades", "ITBIS", "Precio unitario");
    private final Factura facturaActual;
    private Runnable accionAlCambiarArticulos = () -> {};

    // En Artículos, coloca la entrada de datos, la lista de códigos, la tabla y el botón de eliminar.
    PanelArticulos(Factura facturaActual) {
        this.facturaActual = facturaActual;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new TituloApartado("Tabla de facturación"));

        JPanel filaEntradaArticulo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaEntradaArticulo.add(new JLabel("Código:"));
        filaEntradaArticulo.add(campoCodigoArticulo);
        filaEntradaArticulo.add(new JLabel("Cantidad:"));
        filaEntradaArticulo.add(campoCantidadUnidades);
        JButton botonAgregarArticulo = new JButton("Agregar");
        filaEntradaArticulo.add(botonAgregarArticulo);
        add(filaEntradaArticulo);

        JPanel filaAyudaCodigos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String primerosCodigosDisponibles = String.join(", ",
                facturaActual.codigosDeArticulos().stream().limit(3).toList());
        filaAyudaCodigos.add(new JLabel(primerosCodigosDisponibles.isEmpty()
                ? "Agrega artículos en Inventario."
                : "Códigos disponibles: " + primerosCodigosDisponibles));
        add(filaAyudaCodigos);

        JTable tablaArticulos = new JTable(modeloTablaArticulos);
        tablaArticulos.setPreferredScrollableViewportSize(new Dimension(650, tablaArticulos.getRowHeight() * 5));
        tablaArticulos.getColumnModel().getColumn(0).setMaxWidth(90);
        facturaActual.articulos().forEach(this::mostrarLineaDeArticulo);
        add(new JScrollPane(tablaArticulos));

        JPanel filaAccionesArticulos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaAccionesArticulos.add(new BotonEliminarFilas("Eliminar artículos", modeloTablaArticulos,
                facturaActual::eliminarArticulo, () -> accionAlCambiarArticulos.run()));
        add(filaAccionesArticulos);

        botonAgregarArticulo.addActionListener(eventoBoton -> agregarArticuloDesdeLosCampos());
        campoCantidadUnidades.addActionListener(eventoTeclado -> agregarArticuloDesdeLosCampos());
    }

    // En Artículos, envía el código y la cantidad a la factura y, si se aceptan, muestra la nueva línea.
    private void agregarArticuloDesdeLosCampos() {
        Factura.LineaArticulo lineaAgregada;
        try {
            lineaAgregada = facturaActual.agregarArticulo(
                    campoCodigoArticulo.getText(), campoCantidadUnidades.getText());
        } catch (IllegalArgumentException errorDatosArticulo) {
            JOptionPane.showMessageDialog(this, errorDatosArticulo.getMessage());
            return;
        }

        mostrarLineaDeArticulo(lineaAgregada);
        accionAlCambiarArticulos.run();
        campoCodigoArticulo.setText("");
        campoCantidadUnidades.setText("");
        campoCodigoArticulo.requestFocusInWindow();
    }

    // En Artículos, muestra en la tabla una línea registrada, con su casilla aún sin marcar.
    private void mostrarLineaDeArticulo(Factura.LineaArticulo lineaArticuloFacturada) {
        modeloTablaArticulos.addRow(new Object[] {false, lineaArticuloFacturada.codigo(),
                lineaArticuloFacturada.nombre(), lineaArticuloFacturada.unidades(),
                Dinero.formatearPesosDominicanos(lineaArticuloFacturada.itbis()), Dinero.formatearPesosDominicanos(lineaArticuloFacturada.precioUnitario())});
    }

    // En Artículos, define qué hacer para refrescar la pantalla después de agregar o quitar líneas.
    void definirAccionAlCambiarArticulos(Runnable accionDeActualizacion) {
        accionAlCambiarArticulos = accionDeActualizacion;
    }
}
