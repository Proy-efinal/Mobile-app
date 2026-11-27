package frontend.diseno;

import backend.Factura;
import backend.RegistroFacturas;
import basededatos.Cliente;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

// En Facturación, permite elegir el cliente de la factura y consultar su nombre y documento.
class PanelCliente extends JPanel {
    private final JComboBox<Object> selectorCliente = new JComboBox<>();
    private final JTextField campoNombreCliente = new JTextField(20);
    private final JTextField campoDocumentoCliente = new JTextField(15);
    private final Factura facturaActual;
    private final Runnable accionAlCambiarCliente;

    // En Información del cliente, prepara el buscador, el botón Nuevo y los datos de solo lectura.
    PanelCliente(RegistroFacturas registroFacturas, Runnable accionAlCambiarCliente) {
        this.accionAlCambiarCliente = accionAlCambiarCliente;
        this.facturaActual = registroFacturas.facturaActual();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new TituloApartado("Información del cliente"));

        JPanel filaBusquedaCliente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBusquedaCliente.add(new JLabel("Buscar cliente:"));
        selectorCliente.setEditable(true);
        selectorCliente.setPrototypeDisplayValue("Nombre de cliente de ejemplo");
        facturaActual.clientes().forEach(selectorCliente::addItem);
        selectorCliente.setSelectedIndex(-1);
        filaBusquedaCliente.add(selectorCliente);
        JButton botonNuevoCliente = new JButton("Nuevo");
        filaBusquedaCliente.add(botonNuevoCliente);
        add(filaBusquedaCliente);

        JPanel filaDatosCliente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaDatosCliente.add(new JLabel("Nombre:"));
        campoNombreCliente.setEditable(false);
        filaDatosCliente.add(campoNombreCliente);
        filaDatosCliente.add(new JLabel("RNC o cédula:"));
        campoDocumentoCliente.setEditable(false);
        filaDatosCliente.add(campoDocumentoCliente);
        add(filaDatosCliente);

        selectorCliente.setSelectedItem(facturaActual.cliente());
        aplicarSeleccionDeCliente();
        selectorCliente.addActionListener(eventoSeleccion -> aplicarSeleccionDeCliente());
        botonNuevoCliente.addActionListener(eventoBoton -> new FormularioCliente(
                this, registroFacturas, null, this::incorporarClienteNuevoAlSelector, null).setVisible(true));
    }

    // En Información del cliente, acepta un cliente elegido de la lista o busca el texto escrito.
    private void aplicarSeleccionDeCliente() {
        Object opcionEscritaOElegida = selectorCliente.getSelectedItem();
        if (opcionEscritaOElegida instanceof Cliente clienteSeleccionado) {
            mostrarClienteSeleccionado(clienteSeleccionado);
            return;
        }

        facturaActual.setCliente(null);
        accionAlCambiarCliente.run();
        campoNombreCliente.setText("");
        campoDocumentoCliente.setText("");
        if (opcionEscritaOElegida == null || opcionEscritaOElegida.toString().trim().isEmpty()) {
            return;
        }

        Cliente clienteEncontrado = facturaActual.buscarCliente(opcionEscritaOElegida.toString());
        if (clienteEncontrado != null) {
            selectorCliente.setSelectedItem(clienteEncontrado);
            mostrarClienteSeleccionado(clienteEncontrado);
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró el cliente.");
            selectorCliente.setSelectedIndex(-1);
        }
    }

    // En Información del cliente, asigna el cliente a la factura y llena sus datos en la pantalla.
    private void mostrarClienteSeleccionado(Cliente clienteSeleccionado) {
        facturaActual.setCliente(clienteSeleccionado);
        accionAlCambiarCliente.run();
        campoNombreCliente.setText(clienteSeleccionado.nombre());
        campoDocumentoCliente.setText(clienteSeleccionado.documento());
    }

    // En Información del cliente, incorpora al selector el cliente que acaba de guardar el formulario.
    private void incorporarClienteNuevoAlSelector(Cliente clienteNuevo) {
        selectorCliente.addItem(clienteNuevo);
        selectorCliente.setSelectedItem(clienteNuevo);
    }
}
