package frontend.diseno;

import java.util.function.IntConsumer;
import javax.swing.JButton;

// En las tablas de Facturación, elimina los artículos o pagos cuyas casillas se marcaron.
class BotonEliminarFilas extends JButton {
    // En las tablas, crea el botón y lo habilita al marcar por lo menos una fila.
    BotonEliminarFilas(String textoDelBoton, ModeloTabla modeloTabla,
                       IntConsumer accionParaEliminarRegistro, Runnable accionParaActualizarTotales) {
        super(textoDelBoton);
        setEnabled(false);
        modeloTabla.addTableModelListener(eventoTabla -> setEnabled(modeloTabla.hayFilasMarcadas()));
        addActionListener(eventoBoton ->
                eliminarFilasMarcadas(modeloTabla, accionParaEliminarRegistro, accionParaActualizarTotales));
    }

    // En las tablas, quita las filas marcadas de abajo hacia arriba y luego refresca los totales.
    private void eliminarFilasMarcadas(ModeloTabla modeloTabla, IntConsumer accionParaEliminarRegistro,
                                      Runnable accionParaActualizarTotales) {
        for (int indiceFila = modeloTabla.getRowCount() - 1; indiceFila >= 0; indiceFila--) {
            if (modeloTabla.estaFilaMarcada(indiceFila)) {
                accionParaEliminarRegistro.accept(indiceFila);
                modeloTabla.removeRow(indiceFila);
            }
        }
        accionParaActualizarTotales.run();
    }
}
