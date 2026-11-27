package frontend.diseno;

import backend.Factura;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

// En Facturación, muestra el descuento, el impuesto y el importe que falta pagar.
class PanelTotales extends JPanel {
    private final Factura facturaActual;
    private final Runnable accionAlActualizarFactura;
    private final JSpinner selectorPorcentajeDescuento = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0));
    private final JLabel etiquetaDescuentoEnPesos = new JLabel();
    private final JLabel etiquetaItbisEnPesos = new JLabel();
    private final JLabel etiquetaBalancePendiente = new JLabel();
    private final JLabel etiquetaTotalFacturado = new JLabel();

    // En Totales, coloca el descuento editable, el ITBIS fijo y los importes calculados.
    PanelTotales(Factura facturaActual, Runnable accionAlActualizarFactura) {
        this.accionAlActualizarFactura = accionAlActualizarFactura;
        this.facturaActual = facturaActual;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new TituloApartado("Totales"));

        selectorPorcentajeDescuento.setValue(facturaActual.descuento().doubleValue());
        agregarFilaDeTotal("Descuento (%):", selectorPorcentajeDescuento);
        JFormattedTextField campoEditableDescuento =
                ((JSpinner.NumberEditor) selectorPorcentajeDescuento.getEditor()).getTextField();
        ((DefaultFormatter) campoEditableDescuento.getFormatter()).setCommitsOnValidEdit(true);

        JTextField campoItbisFijo = new JTextField(facturaActual.porcentajeItbis().toPlainString() + "%", 6);
        campoItbisFijo.setEditable(false);
        agregarFilaDeTotal("ITBIS fijo:", campoItbisFijo);

        agregarFilaDeTotal("Descuento en pesos:", etiquetaDescuentoEnPesos);
        agregarFilaDeTotal("ITBIS en pesos:", etiquetaItbisEnPesos);
        agregarFilaDeTotal("Balance pendiente:", etiquetaBalancePendiente);
        agregarFilaDeTotal("TOTAL FACTURADO:", etiquetaTotalFacturado);

        selectorPorcentajeDescuento.addChangeListener(eventoDescuento -> actualizarImportesDeFactura());
        actualizarImportesDeFactura();
    }

    // En Totales, coloca el nombre y el valor de cada importe en la misma fila.
    private void agregarFilaDeTotal(String nombreDelImporte, Component componenteDelValor) {
        JPanel filaDelImporte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaDelImporte.add(new JLabel(nombreDelImporte));
        filaDelImporte.add(componenteDelValor);
        add(filaDelImporte);
    }

    // En Totales, pide a la factura que recalcule los importes y muestra el resultado.
    void actualizarImportesDeFactura() {
        BigDecimal porcentajeDescuento = BigDecimal.valueOf(
                ((Number) selectorPorcentajeDescuento.getValue()).doubleValue());
        facturaActual.setDescuento(porcentajeDescuento);
        Factura.Totales importesCalculados = facturaActual.calcularTotales(porcentajeDescuento);

        etiquetaDescuentoEnPesos.setText(Dinero.formatearPesosDominicanos(importesCalculados.descuento()));
        etiquetaItbisEnPesos.setText(Dinero.formatearPesosDominicanos(importesCalculados.itbis()));
        etiquetaBalancePendiente.setText(Dinero.formatearPesosDominicanos(importesCalculados.pendiente()));
        etiquetaTotalFacturado.setText(Dinero.formatearPesosDominicanos(importesCalculados.total()));
        accionAlActualizarFactura.run();
    }
}
