package frontend.diseno;

import backend.RegistroFacturas;
import basededatos.Cliente;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// En Clientes, muestra el catálogo y permite abrir las fichas para agregar o editar clientes.
class PanelClientes extends JPanel {
    private final RegistroFacturas registroFacturas;
    private final JTextField campoBusquedaClientes = new JTextField(20);

    // En Clientes, guarda las filas y columnas que se muestran en la tabla del catálogo.
    private final DefaultTableModel modeloTablaClientes = new DefaultTableModel(
            new String[] {"Nombre", "Teléfono", "RNC o cédula", "Nota"}, 0) {
        // En Clientes, deja la tabla en modo lectura para que los cambios se hagan en la ficha.
        @Override
        public boolean isCellEditable(int filaCliente, int columnaCliente) {
            return false;
        }
    };

    private final JTable tablaClientes = new JTable(modeloTablaClientes);
    private final TableRowSorter<DefaultTableModel> filtroTablaClientes = new TableRowSorter<>(modeloTablaClientes);
    private List<Cliente> clientesEnCatalogo = List.of();

    // En Clientes, arma la pantalla, primero el encabezado, luego la tabla y por último sus acciones.
    PanelClientes(RegistroFacturas registroFacturas) {
        super(new BorderLayout());
        this.registroFacturas = registroFacturas;

        // Título, campo de búsqueda y botón para agregar.
        JPanel encabezadoClientes = new JPanel(new BorderLayout());
        encabezadoClientes.add(new TituloApartado("Clientes"), BorderLayout.NORTH);
        JPanel controlesClientes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel etiquetaBuscarClientes = new JLabel("🔍 Buscar:");
        etiquetaBuscarClientes.setLabelFor(campoBusquedaClientes);
        controlesClientes.add(etiquetaBuscarClientes);
        controlesClientes.add(campoBusquedaClientes);
        JButton botonAgregarCliente = new JButton("Agregar cliente");
        controlesClientes.add(botonAgregarCliente);
        encabezadoClientes.add(controlesClientes, BorderLayout.SOUTH);
        add(encabezadoClientes, BorderLayout.NORTH);

        // Tabla del catálogo.
        tablaClientes.setRowSorter(filtroTablaClientes);
        tablaClientes.setPreferredScrollableViewportSize(new Dimension(650, 180));
        add(new JScrollPane(tablaClientes), BorderLayout.CENTER);

        // Acciones del buscador, del botón Agregar y de cada fila.
        // En Clientes, revisa el campo de búsqueda cada vez que cambia su texto.
        campoBusquedaClientes.getDocument().addDocumentListener(new DocumentListener() {
            // En Clientes, vuelve a filtrar al escribir caracteres.
            @Override
            public void insertUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarClientes();
            }

            // En Clientes, vuelve a filtrar al borrar caracteres.
            @Override
            public void removeUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarClientes();
            }

            // En Clientes, vuelve a filtrar si el documento del campo notifica otro cambio.
            @Override
            public void changedUpdate(DocumentEvent eventoDeBusqueda) {
                filtrarClientes();
            }
        });
        botonAgregarCliente.addActionListener(eventoAgregar -> abrirFichaCliente(null));

        // En Clientes, escucha clics para abrir la ficha del cliente elegido.
        tablaClientes.addMouseListener(new MouseAdapter() {
            // En Clientes, localiza el cliente real aunque las filas estén filtradas u ordenadas.
            @Override
            public void mouseClicked(MouseEvent clicEnTabla) {
                int filaVisible = tablaClientes.rowAtPoint(clicEnTabla.getPoint());
                if (filaVisible >= 0 && clicEnTabla.getButton() == MouseEvent.BUTTON1) {
                    int filaEnCatalogo = tablaClientes.convertRowIndexToModel(filaVisible);
                    abrirFichaCliente(clientesEnCatalogo.get(filaEnCatalogo));
                }
            }
        });
        actualizar();
    }

    // En Clientes, muestra las coincidencias por nombre, teléfono, RNC o cédula.
    private void filtrarClientes() {
        String textoBuscado = campoBusquedaClientes.getText().trim();
        filtroTablaClientes.setRowFilter(textoBuscado.isEmpty() ? null
                : RowFilter.regexFilter("(?iu)" + Pattern.quote(textoBuscado), 0, 1, 2));
    }

    // En Clientes, abre una ficha vacía o una ficha existente y actualiza la tabla si hay cambios.
    private void abrirFichaCliente(Cliente clienteSeleccionado) {
        new FormularioCliente(this, registroFacturas, clienteSeleccionado,
                clienteGuardado -> actualizar(), this::actualizar).setVisible(true);
    }

    // En Clientes, vuelve a cargar la tabla con los clientes que están en el catálogo.
    void actualizar() {
        clientesEnCatalogo = List.copyOf(registroFacturas.clientes());
        modeloTablaClientes.setRowCount(0);
        for (Cliente clienteCatalogo : clientesEnCatalogo) {
            modeloTablaClientes.addRow(new Object[] {clienteCatalogo.nombre(), clienteCatalogo.telefono(),
                    clienteCatalogo.documento(), clienteCatalogo.nota()});
        }
    }
}
