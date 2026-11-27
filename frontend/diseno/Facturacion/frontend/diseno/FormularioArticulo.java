package frontend.diseno;

import backend.RegistroFacturas;
import basededatos.CatalogoClientesYArticulos;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// Esta ficha de Inventario permite agregar, modificar o eliminar artículos del catálogo.
class FormularioArticulo extends JDialog {
    private final JTextField campoCodigoArticulo = new JTextField(18);
    private final JTextField campoNombreArticulo = new JTextField(18);
    private final JTextField campoPrecioArticulo = new JTextField(18);
    private final JTextField campoExistenciaArticulo = new JTextField("0", 18);

    // En Inventario, arma la ficha y carga la información cuando se abre un artículo existente.
    FormularioArticulo(JPanel panelInventario, RegistroFacturas registroFacturas, String codigoArticuloExistente,
            Runnable actualizarInventario) {
        super(SwingUtilities.getWindowAncestor(panelInventario),
                codigoArticuloExistente == null ? "Agregar artículo" : "Editar artículo",
                Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Si el artículo es nuevo, el sistema asigna el código y deja los demás campos listos para escribir.
        boolean esArticuloNuevo = codigoArticuloExistente == null;
        campoCodigoArticulo.setText(esArticuloNuevo ? registroFacturas.siguienteCodigoArticulo() : codigoArticuloExistente);
        campoCodigoArticulo.setEditable(false);
        if (!esArticuloNuevo) {
            CatalogoClientesYArticulos.Articulo articuloExistente = registroFacturas.articulo(codigoArticuloExistente);
            campoNombreArticulo.setText(articuloExistente.nombre());
            campoPrecioArticulo.setText(articuloExistente.precio().toPlainString());
            campoExistenciaArticulo.setText(Integer.toString(articuloExistente.existencia()));
        }

        // Campos de la ficha, en el mismo orden en que aparecen en pantalla.
        JPanel panelCamposArticulo = new JPanel(new GridLayout(0, 2, 6, 4));
        panelCamposArticulo.add(new JLabel("Código:"));
        panelCamposArticulo.add(campoCodigoArticulo);
        panelCamposArticulo.add(new JLabel("Nombre:"));
        panelCamposArticulo.add(campoNombreArticulo);
        panelCamposArticulo.add(new JLabel("Precio (RD$):"));
        panelCamposArticulo.add(campoPrecioArticulo);
        panelCamposArticulo.add(new JLabel("Existencia:"));
        panelCamposArticulo.add(campoExistenciaArticulo);
        add(panelCamposArticulo, BorderLayout.CENTER);

        // Botones al pie de la ficha.
        JPanel panelBotonesArticulo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botonGuardarArticulo = new JButton("Guardar");
        JButton botonCancelarEdicion = new JButton("Cancelar");
        panelBotonesArticulo.add(botonGuardarArticulo);
        if (!esArticuloNuevo) {
            JButton botonEliminarArticulo = new JButton("Eliminar");
            botonEliminarArticulo.addActionListener(eventoEliminar ->
                    confirmarEliminacionArticulo(registroFacturas, codigoArticuloExistente, actualizarInventario));
            panelBotonesArticulo.add(botonEliminarArticulo);
        }
        panelBotonesArticulo.add(botonCancelarEdicion);
        add(panelBotonesArticulo, BorderLayout.SOUTH);

        botonGuardarArticulo.addActionListener(eventoGuardar ->
                guardarDatosArticulo(registroFacturas, esArticuloNuevo, actualizarInventario));
        botonCancelarEdicion.addActionListener(eventoCancelar -> dispose());
        getRootPane().setDefaultButton(botonGuardarArticulo);
        pack();
        setLocationRelativeTo(panelInventario);
    }

    // En Inventario, guarda la información escrita, actualiza la tabla y cierra la ficha.
    private void guardarDatosArticulo(RegistroFacturas registroFacturas, boolean esArticuloNuevo,
            Runnable actualizarInventario) {
        try {
            registroFacturas.guardarArticulo(campoCodigoArticulo.getText(), campoNombreArticulo.getText(),
                    campoPrecioArticulo.getText(), campoExistenciaArticulo.getText(), esArticuloNuevo);
            actualizarInventario.run();
            dispose();
        } catch (IllegalArgumentException | IllegalStateException errorDeValidacion) {
            JOptionPane.showMessageDialog(this, errorDeValidacion.getMessage());
        } catch (IOException errorAlGuardar) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el artículo: " + errorAlGuardar.getMessage());
        }
    }

    // En Inventario, pide permiso antes de borrar el artículo y refresca la tabla si se confirma.
    private void confirmarEliminacionArticulo(RegistroFacturas registroFacturas, String codigoArticulo,
            Runnable actualizarInventario) {
        String[] opcionesDeConfirmacion = {"Eliminar", "Cancelar"};
        int opcionElegida = JOptionPane.showOptionDialog(this,
                "¿Eliminar el artículo " + codigoArticulo + " del inventario?", "Eliminar artículo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                opcionesDeConfirmacion, opcionesDeConfirmacion[1]);
        if (opcionElegida != 0) {
            return;
        }

        try {
            registroFacturas.eliminarArticulo(codigoArticulo);
            actualizarInventario.run();
            dispose();
        } catch (IllegalArgumentException errorDeValidacion) {
            JOptionPane.showMessageDialog(this, errorDeValidacion.getMessage());
        } catch (IOException errorAlEliminar) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el artículo: " + errorAlEliminar.getMessage());
        }
    }
}
