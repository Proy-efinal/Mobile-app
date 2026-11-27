package frontend.diseno;

import backend.RegistroFacturas;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// En Configuración, permite cambiar los datos de la empresa y la tasa de ITBIS.
class PanelConfiguracion extends JPanel {
    private final RegistroFacturas registroFacturas;
    private final JTextField campoNombreEmpresa = new JTextField(30);
    private final JTextField campoDireccionEmpresa = new JTextField(30);
    private final JTextField campoRncEmpresa = new JTextField(16);
    private final JTextField campoPorcentajeItbis = new JTextField(8);
    private final JLabel mensajeEstadoConfiguracion = new JLabel();

    // En Configuración, arma la pantalla con los datos de la empresa y las opciones de facturación.
    PanelConfiguracion(RegistroFacturas registroFacturas) {
        super(new BorderLayout());
        this.registroFacturas = registroFacturas;

        JPanel contenidoConfiguracion = new JPanel();
        contenidoConfiguracion.setLayout(new BoxLayout(contenidoConfiguracion, BoxLayout.Y_AXIS));

        // Datos de la empresa que aparecerán en la factura.
        contenidoConfiguracion.add(new TituloApartado("Datos de la empresa"));
        contenidoConfiguracion.add(crearFilaCampo("Nombre:", campoNombreEmpresa));
        contenidoConfiguracion.add(crearFilaCampo("Dirección:", campoDireccionEmpresa));
        contenidoConfiguracion.add(crearFilaCampo("RNC:", campoRncEmpresa));
        contenidoConfiguracion.add(Box.createVerticalStrut(18));

        // Tasa que se aplica al calcular el impuesto de las facturas.
        contenidoConfiguracion.add(new TituloApartado("Facturación"));
        contenidoConfiguracion.add(crearFilaCampo("ITBIS fijo (%):", campoPorcentajeItbis));

        // Botón para guardar y mensaje con el resultado.
        JPanel panelBotonesConfiguracion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonGuardarConfiguracion = new JButton("Guardar configuración");
        botonGuardarConfiguracion.addActionListener(eventoGuardar -> guardarConfiguracion());
        panelBotonesConfiguracion.add(botonGuardarConfiguracion);
        panelBotonesConfiguracion.add(mensajeEstadoConfiguracion);
        contenidoConfiguracion.add(panelBotonesConfiguracion);
        add(contenidoConfiguracion, BorderLayout.NORTH);

        cargarConfiguracionGuardada();

        // En Configuración, detecta cuándo se edita algún campo después de guardar.
        DocumentListener detectarEdicionConfiguracion = new DocumentListener() {
            // En Configuración, borra la confirmación anterior al escribir.
            @Override
            public void insertUpdate(DocumentEvent eventoDeEdicion) {
                mensajeEstadoConfiguracion.setText("");
            }

            // En Configuración, borra la confirmación anterior al eliminar texto.
            @Override
            public void removeUpdate(DocumentEvent eventoDeEdicion) {
                mensajeEstadoConfiguracion.setText("");
            }

            // En Configuración, borra la confirmación anterior ante otro cambio en el campo.
            @Override
            public void changedUpdate(DocumentEvent eventoDeEdicion) {
                mensajeEstadoConfiguracion.setText("");
            }
        };
        for (JTextField campoEditable : new JTextField[] {
                campoNombreEmpresa, campoDireccionEmpresa, campoRncEmpresa, campoPorcentajeItbis}) {
            campoEditable.getDocument().addDocumentListener(detectarEdicionConfiguracion);
        }
    }

    // En Configuración, coloca una etiqueta junto al campo que le corresponde.
    private JPanel crearFilaCampo(String textoEtiqueta, JTextField campoEditable) {
        JPanel filaConfiguracion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel etiquetaDelCampo = new JLabel(textoEtiqueta);
        etiquetaDelCampo.setLabelFor(campoEditable);
        filaConfiguracion.add(etiquetaDelCampo);
        filaConfiguracion.add(campoEditable);
        return filaConfiguracion;
    }

    // En Configuración, lee los valores guardados y los coloca en sus campos.
    private void cargarConfiguracionGuardada() {
        RegistroFacturas.Configuracion configuracionGuardada = registroFacturas.configuracion();
        campoNombreEmpresa.setText(configuracionGuardada.empresa());
        campoDireccionEmpresa.setText(configuracionGuardada.direccion());
        campoRncEmpresa.setText(configuracionGuardada.rnc());
        campoPorcentajeItbis.setText(configuracionGuardada.porcentajeItbis().stripTrailingZeros().toPlainString());
    }

    // En Configuración, envía los datos al registro y confirma si se pudieron guardar.
    private void guardarConfiguracion() {
        try {
            registroFacturas.guardarConfiguracion(campoNombreEmpresa.getText(), campoDireccionEmpresa.getText(),
                    campoRncEmpresa.getText(), campoPorcentajeItbis.getText());
            cargarConfiguracionGuardada();
            mensajeEstadoConfiguracion.setText("Configuración guardada");
        } catch (IllegalArgumentException errorDeValidacion) {
            JOptionPane.showMessageDialog(this, errorDeValidacion.getMessage());
        } catch (IOException errorAlGuardar) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la configuración: " + errorAlGuardar.getMessage());
        }
    }
}
