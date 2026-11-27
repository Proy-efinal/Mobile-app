package frontend.diseno;

import backend.TipoFactura;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

// En Facturación, permite elegir el tipo de comprobante y muestra su número, fecha y hora.
class PanelTipoFactura extends JPanel {
    private static final DateTimeFormatter FORMATO_FECHA_FACTURA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA_FACTURA = DateTimeFormatter.ofPattern("HH:mm");

    // En Tipo de factura, coloca el selector arriba y deja el número, la fecha y la hora como referencia.
    PanelTipoFactura(TipoFactura tipoDeFacturaActual, String numeroComprobanteFiscal,
                     LocalDateTime fechaHoraFactura, Consumer<TipoFactura> accionAlCambiarTipo) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new TituloApartado("Tipo de factura"));

        JPanel filaSeleccionTipoFactura = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaSeleccionTipoFactura.add(new JLabel("Tipo de factura:"));
        JComboBox<TipoFactura> selectorTipoFactura = new JComboBox<>(TipoFactura.values());
        selectorTipoFactura.setSelectedItem(tipoDeFacturaActual);
        filaSeleccionTipoFactura.add(selectorTipoFactura);
        add(filaSeleccionTipoFactura);

        JPanel filaIdentificacionFactura = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaIdentificacionFactura.add(new JLabel("NCF de ejemplo:"));
        JTextField campoNumeroComprobante = new JTextField(numeroComprobanteFiscal, 12);
        campoNumeroComprobante.setEditable(false);
        filaIdentificacionFactura.add(campoNumeroComprobante);
        filaIdentificacionFactura.add(new JLabel("Fecha: " + fechaHoraFactura.format(FORMATO_FECHA_FACTURA)));
        filaIdentificacionFactura.add(new JLabel("Hora: " + fechaHoraFactura.format(FORMATO_HORA_FACTURA)));
        add(filaIdentificacionFactura);

        selectorTipoFactura.addActionListener(eventoSeleccion -> {
            TipoFactura tipoDeFacturaElegido = (TipoFactura) selectorTipoFactura.getSelectedItem();
            if (tipoDeFacturaElegido != tipoDeFacturaActual) {
                accionAlCambiarTipo.accept(tipoDeFacturaElegido);
            }
        });
    }
}
