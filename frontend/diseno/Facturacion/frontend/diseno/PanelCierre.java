package frontend.diseno;

import backend.Factura;
import java.awt.GridLayout;
import javax.swing.JPanel;

// En Facturación, reúne los pagos a la izquierda y los totales a la derecha.
class PanelCierre extends JPanel {
    // En el cierre, coloca los dos paneles y hace que los totales se refresquen al cambiar artículos o pagos.
    PanelCierre(Factura facturaActual, PanelArticulos panelArticulos, Runnable accionAlCambiarFactura) {
        super(new GridLayout(1, 2, 28, 0));

        PanelPagos panelPagos = new PanelPagos(facturaActual);
        add(panelPagos);

        PanelTotales panelTotales = new PanelTotales(facturaActual, accionAlCambiarFactura);
        add(panelTotales);

        panelArticulos.definirAccionAlCambiarArticulos(panelTotales::actualizarImportesDeFactura);
        panelPagos.definirAccionAlCambiarPagos(panelTotales::actualizarImportesDeFactura);
    }
}
