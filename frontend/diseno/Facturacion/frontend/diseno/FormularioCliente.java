package frontend.diseno;

import backend.RegistroFacturas;
import basededatos.Cliente;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// Esta ficha permite editar en Clientes y registrar nuevos clientes desde Facturación.
class FormularioCliente extends JDialog {
    private final JTextField campoNombreCliente = new JTextField(22);
    private final JTextField campoTelefonoCliente = new JTextField(22);
    private final JTextField campoDocumentoCliente = new JTextField(22);
    private final JTextArea campoNotaCliente = new JTextArea(3, 22);

    // En Clientes, arma la ficha con datos vacíos para agregar o con datos existentes para editar.
    FormularioCliente(JPanel panelQueAbreLaFicha, RegistroFacturas registroFacturas, Cliente clienteExistente,
            Consumer<Cliente> informarClienteGuardado, Runnable informarClienteEliminado) {
        super(SwingUtilities.getWindowAncestor(panelQueAbreLaFicha),
                clienteExistente == null ? "Nuevo cliente" : "Editar cliente",
                Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        boolean esClienteNuevo = clienteExistente == null;
        if (!esClienteNuevo) {
            campoNombreCliente.setText(clienteExistente.nombre());
            campoTelefonoCliente.setText(clienteExistente.telefono());
            campoDocumentoCliente.setText(clienteExistente.documento());
            campoNotaCliente.setText(clienteExistente.nota());
        }

        // Datos principales de la ficha.
        JPanel columnaEtiquetasCliente = new JPanel(new GridLayout(0, 1));
        JPanel columnaCamposCliente = new JPanel(new GridLayout(0, 1));
        columnaEtiquetasCliente.add(new JLabel("Nombre:"));
        columnaCamposCliente.add(campoNombreCliente);
        columnaEtiquetasCliente.add(new JLabel("Teléfono:"));
        columnaCamposCliente.add(campoTelefonoCliente);
        columnaEtiquetasCliente.add(new JLabel("RNC o cédula:"));
        columnaCamposCliente.add(campoDocumentoCliente);

        JPanel panelDatosCliente = new JPanel(new BorderLayout(5, 0));
        panelDatosCliente.add(columnaEtiquetasCliente, BorderLayout.WEST);
        panelDatosCliente.add(columnaCamposCliente, BorderLayout.CENTER);

        // La nota se muestra debajo porque admite varias líneas.
        JPanel contenidoFichaCliente = new JPanel(new BorderLayout());
        contenidoFichaCliente.add(panelDatosCliente, BorderLayout.NORTH);
        JPanel panelNotaCliente = new JPanel(new BorderLayout());
        panelNotaCliente.add(new JLabel("Nota:"), BorderLayout.NORTH);
        campoNotaCliente.setLineWrap(true);
        campoNotaCliente.setWrapStyleWord(true);
        panelNotaCliente.add(new JScrollPane(campoNotaCliente), BorderLayout.CENTER);
        contenidoFichaCliente.add(panelNotaCliente, BorderLayout.CENTER);
        add(contenidoFichaCliente, BorderLayout.CENTER);

        // Botones al pie de la ficha.
        JPanel panelBotonesCliente = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botonGuardarCliente = new JButton(esClienteNuevo ? "Agregar" : "Guardar");
        JButton botonRegresar = new JButton("Regresar");
        panelBotonesCliente.add(botonGuardarCliente);
        if (!esClienteNuevo) {
            JButton botonEliminarCliente = new JButton("Eliminar");
            botonEliminarCliente.addActionListener(eventoEliminar ->
                    confirmarEliminacionCliente(registroFacturas, clienteExistente, informarClienteEliminado));
            panelBotonesCliente.add(botonEliminarCliente);
        }
        panelBotonesCliente.add(botonRegresar);
        add(panelBotonesCliente, BorderLayout.SOUTH);

        botonGuardarCliente.addActionListener(eventoGuardar ->
                guardarDatosCliente(registroFacturas, clienteExistente, informarClienteGuardado));
        botonRegresar.addActionListener(eventoRegresar -> dispose());
        getRootPane().setDefaultButton(botonGuardarCliente);
        pack();
        setLocationRelativeTo(panelQueAbreLaFicha);
    }

    // En Clientes, guarda la información y avisa a la pantalla que abrió la ficha.
    private void guardarDatosCliente(RegistroFacturas registroFacturas, Cliente clienteExistente,
            Consumer<Cliente> informarClienteGuardado) {
        try {
            Cliente clienteGuardado = registroFacturas.guardarCliente(clienteExistente, campoNombreCliente.getText(),
                    campoTelefonoCliente.getText(), campoDocumentoCliente.getText(), campoNotaCliente.getText());
            informarClienteGuardado.accept(clienteGuardado);
        } catch (IllegalArgumentException errorDeValidacion) {
            JOptionPane.showMessageDialog(this, errorDeValidacion.getMessage());
            return;
        } catch (IOException errorAlGuardar) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el cliente: " + errorAlGuardar.getMessage());
            return;
        }
        dispose();
    }

    // En Clientes, pide permiso antes de borrar un cliente y refresca la tabla si se confirma.
    private void confirmarEliminacionCliente(RegistroFacturas registroFacturas, Cliente clienteExistente,
            Runnable informarClienteEliminado) {
        String[] opcionesDeConfirmacion = {"Eliminar", "Cancelar"};
        int opcionElegida = JOptionPane.showOptionDialog(this,
                "¿Eliminar el cliente " + clienteExistente.nombre() + "?", "Eliminar cliente",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                opcionesDeConfirmacion, opcionesDeConfirmacion[1]);
        if (opcionElegida != 0) {
            return;
        }

        try {
            registroFacturas.eliminarCliente(clienteExistente);
            informarClienteEliminado.run();
            dispose();
        } catch (IllegalArgumentException | IOException errorAlEliminar) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el cliente: " + errorAlEliminar.getMessage());
        }
    }
}
