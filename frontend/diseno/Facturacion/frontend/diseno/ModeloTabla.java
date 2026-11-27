package frontend.diseno;

import javax.swing.table.DefaultTableModel;

// En las tablas de artículos y pagos, guarda las filas y permite marcar cuáles se pueden quitar.
class ModeloTabla extends DefaultTableModel {
    private static final int COLUMNA_CASILLA_SELECCION = 0;

    // En las tablas, prepara las columnas con sus nombres y deja la lista de filas vacía.
    ModeloTabla(String... nombresDeLasColumnas) {
        super(nombresDeLasColumnas, 0);
    }

    // En las tablas, indica a Swing que la primera columna muestra casillas para marcar.
    @Override
    public Class<?> getColumnClass(int indiceColumna) {
        return indiceColumna == COLUMNA_CASILLA_SELECCION ? Boolean.class : super.getColumnClass(indiceColumna);
    }

    // En las tablas, permite editar únicamente la casilla de cada fila.
    @Override
    public boolean isCellEditable(int indiceFila, int indiceColumna) {
        return indiceColumna == COLUMNA_CASILLA_SELECCION;
    }

    // En las tablas, indica si el usuario marcó la casilla de una fila concreta.
    boolean estaFilaMarcada(int indiceFila) {
        return Boolean.TRUE.equals(getValueAt(indiceFila, COLUMNA_CASILLA_SELECCION));
    }

    // En las tablas, comprueba si hay al menos una fila marcada que se pueda quitar.
    boolean hayFilasMarcadas() {
        for (int indiceFila = 0; indiceFila < getRowCount(); indiceFila++) {
            if (estaFilaMarcada(indiceFila)) {
                return true;
            }
        }
        return false;
    }
}
