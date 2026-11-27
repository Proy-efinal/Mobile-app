package frontend.diseno;

import backend.Factura;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

// En Facturación, registra, muestra y permite quitar pagos de la factura actual.
class PanelPagos extends JPanel {
    private final JComboBox<String> selectorMetodoPago = new JComboBox<>(new String[] {
        "Efectivo", "Tarjeta", "Transferencia", "Cheque"
    });
    private final JTextField campoMontoPago = new JTextField(9);
    private final ModeloTabla modeloTablaPagos = new ModeloTabla("Seleccionar", "Método de pago", "Monto");
    private final Factura facturaActual;
    private Runnable accionAlCambiarPagos = () -> {};

    // En Método de pago, organiza el selector, el monto, la tabla y el botón de eliminar.
    PanelPagos(Factura facturaActual) {
        this.facturaActual = facturaActual;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new TituloApartado("Método de pago"));

        JPanel filaMetodoPago = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaMetodoPago.add(new JLabel("Método:"));
        filaMetodoPago.add(selectorMetodoPago);
        add(filaMetodoPago);

        JPanel filaMontoPago = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaMontoPago.add(new JLabel("Monto (RD$):"));
        filaMontoPago.add(campoMontoPago);
        JButton botonRegistrarPago = new JButton("Pagar");
        filaMontoPago.add(botonRegistrarPago);
        add(filaMontoPago);

        JTable tablaPagos = new JTable(modeloTablaPagos);
        tablaPagos.getColumnModel().getColumn(0).setMaxWidth(90);
        facturaActual.pagos().forEach(this::mostrarPagoRegistrado);
        tablaPagos.getColumnModel().getColumn(2).setCellRenderer(new FormatoMontoEnTabla());
        tablaPagos.setPreferredScrollableViewportSize(new Dimension(305, tablaPagos.getRowHeight() * 3));
        add(new JScrollPane(tablaPagos));

        JPanel filaAccionesPagos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaAccionesPagos.add(new BotonEliminarFilas("Eliminar pagos", modeloTablaPagos,
                facturaActual::eliminarPago, () -> accionAlCambiarPagos.run()));
        add(filaAccionesPagos);

        botonRegistrarPago.addActionListener(eventoBoton -> registrarPagoDesdeLosCampos());
        campoMontoPago.addActionListener(eventoTeclado -> registrarPagoDesdeLosCampos());
    }

    // En Método de pago, envía el método y el monto a la factura y muestra el pago si se acepta.
    private void registrarPagoDesdeLosCampos() {
        Factura.Pago pagoRegistrado;
        try {
            String metodoPagoElegido = (String) selectorMetodoPago.getSelectedItem();
            pagoRegistrado = facturaActual.agregarPago(metodoPagoElegido, campoMontoPago.getText());
        } catch (IllegalArgumentException errorDatosPago) {
            JOptionPane.showMessageDialog(this, errorDatosPago.getMessage());
            return;
        }

        mostrarPagoRegistrado(pagoRegistrado);
        accionAlCambiarPagos.run();
        campoMontoPago.setText("");
    }

    // En Método de pago, agrega a la tabla el método y monto de un pago registrado.
    private void mostrarPagoRegistrado(Factura.Pago pagoRegistrado) {
        modeloTablaPagos.addRow(new Object[] {false, pagoRegistrado.metodo(), pagoRegistrado.monto()});
    }

    // En Método de pago, define cómo se refresca la pantalla después de agregar o quitar pagos.
    void definirAccionAlCambiarPagos(Runnable accionDeActualizacion) {
        accionAlCambiarPagos = accionDeActualizacion;
    }

    // En la tabla de pagos, convierte los importes en texto con el símbolo de pesos dominicanos.
    private static class FormatoMontoEnTabla extends DefaultTableCellRenderer {
        // En la tabla de pagos, presenta el importe de una celda o la deja vacía si no tiene valor.
        @Override
        protected void setValue(Object montoRegistrado) {
            super.setValue(montoRegistrado == null ? "" : Dinero.formatearPesosDominicanos((BigDecimal) montoRegistrado));
        }
    }
}
