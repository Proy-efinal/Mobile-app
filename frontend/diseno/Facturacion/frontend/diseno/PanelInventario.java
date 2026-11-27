package frontend.diseno;

import backend.RegistroFacturas;
import basededatos.CatalogoClientesYArticulos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// En Inventario, muestra los artículos y permite abrir sus fichas para agregar o editar.
class PanelInventario extends JPanel {
    private final RegistroFacturas registroFacturas;
    private final JTextField campoBusquedaArticulos = new JTextField(20);

    // En Inventario, guarda las filas y columnas que aparecen en la tabla de artículos.
    private final DefaultTableModel modeloTablaArticulos = new DefaultTableModel(
            new String[] {"Código", "Nombre del artículo", "Precio", "Existencia"}, 0) {
        // En Inventario, deja la tabla en modo lectura para que los cambios se hagan en la ficha.
        @Override
        public boolean isCellEditable(int filaArticulo, int columnaArticulo) {
            return false;
        }
    };

    private final JTable tablaArticulos = new JTable(modeloTablaArticulos);
    private final TableRowSorter<DefaultTableModel> filtroTablaArticulos = new TableRowSorter<>(modeloTablaArticulos);

    // En Inventario, arma la pantalla, primero el encabezado, luego la tabla y por último sus acciones.
    PanelInventario(RegistroFacturas registroFacturas) {
        super(new BorderLayout());
        this.registroFacturas = registroFacturas;

        // Título, campo de búsqueda y botón para agregar.
        JPanel encabezadoInventario = new JPanel(new BorderLayout());
        encabezadoInventario.add(new TituloApartado("Inventario"), BorderLayout.NORTH);
        JPanel controlesInventario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel etiquetaBuscarArticulos = new JLabel("🔍 Buscar:");
        etiquetaBuscarArticulos.setLabelFor(campoBusquedaArticulos);
        controlesInventario.add(etiquetaBuscarArticulos);
        controlesInventario.add(campoBusquedaArticulos);
        JButton botonAgregarArticulo = new JButton("Agregar artículo");
        controlesInventario.add(botonAgregarArticulo);
        encabezadoInventario.add(controlesInventario, BorderLayout.SOUTH);
        add(encabezadoInventario, BorderLayout.NORTH);

        // Tabla del catálogo.
        tablaArticulos.setRowSorter(filtroTablaArticulos);
        tablaArticulos.setPreferredScrollableViewportSize(new Dimension(650, 180));
        add(new JScrollPane(tablaArticulos), BorderLayout.CENTER);

        // Acciones del buscador, del botón Agregar y de cada fila.
        // En Inventario, revisa el campo de búsqueda cada vez que cambia su texto.
        campoBusquedaArticulos.getDocument().addDocumentListener(new DocumentListener() {
            // En Inventario, vuelve a filtrar al escribir caracteres.
            @Override
            public void insertUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarArticulos();
            }

            // En Inventario, vuelve a filtrar al borrar caracteres.
            @Override
            public void removeUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarArticulos();
            }

            // En Inventario, vuelve a filtrar si el documento del campo notifica otro cambio.
            @Override
            public void changedUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarArticulos();
            }
        });
        botonAgregarArticulo.addActionListener(eventoAgregar -> abrirFichaArticulo(null));

        // En Inventario, escucha clics para abrir la ficha del artículo elegido.
        tablaArticulos.addMouseListener(new MouseAdapter() {
            // En Inventario, localiza el artículo real aunque las filas estén filtradas u ordenadas.
            @Override
            public void mouseClicked(MouseEvent clicEnTabla) {
                int filaVisible = tablaArticulos.rowAtPoint(clicEnTabla.getPoint());
                if (filaVisible >= 0 && clicEnTabla.getButton() == MouseEvent.BUTTON1) {
                    int filaEnCatalogo = tablaArticulos.convertRowIndexToModel(filaVisible);
                    String codigoArticulo = (String) modeloTablaArticulos.getValueAt(filaEnCatalogo, 0);
                    abrirFichaArticulo(codigoArticulo);
                }
            }
        });
        actualizar();
    }

    // En Inventario, muestra los códigos o los nombres que coinciden con la búsqueda.
    private void filtrarArticulos() {
        String textoBuscado = campoBusquedaArticulos.getText().trim();
        filtroTablaArticulos.setRowFilter(textoBuscado.isEmpty() ? null
                : RowFilter.regexFilter("(?iu)" + Pattern.quote(textoBuscado), 0, 1));
    }

    // En Inventario, abre la ficha vacía para agregar o la ficha llena para editar.
    private void abrirFichaArticulo(String codigoArticulo) {
        try {
            new FormularioArticulo(this, registroFacturas, codigoArticulo, this::actualizar).setVisible(true);
        } catch (IllegalStateException errorAlAbrirFicha) {
            JOptionPane.showMessageDialog(this, errorAlAbrirFicha.getMessage());
        }
    }

    // En Inventario, vuelve a cargar la tabla con los artículos que están en el catálogo.
    void actualizar() {
        modeloTablaArticulos.setRowCount(0);
        for (String codigoArticulo : registroFacturas.codigosInventario()) {
            CatalogoClientesYArticulos.Articulo articuloCatalogo = registroFacturas.articulo(codigoArticulo);
            modeloTablaArticulos.addRow(new Object[] {codigoArticulo, articuloCatalogo.nombre(),
                    Dinero.formatearPesosDominicanos(articuloCatalogo.precio()), articuloCatalogo.existencia()});
        }
    }
}
