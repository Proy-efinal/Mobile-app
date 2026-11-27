package frontend.diseno;

import backend.RegistroFacturas;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

// En Facturación, permite recorrer, guardar y abrir en PDF las facturas de la serie seleccionada.
class PanelRegistroFacturas extends JPanel {
    private final RegistroFacturas registroFacturas;
    private final JLabel etiquetaEstadoGuardado = new JLabel();

    // En el registro, coloca los botones y el estado en el orden en que se ven.
    PanelRegistroFacturas(RegistroFacturas registroFacturas, Path carpetaPdfsFacturas,
                          Runnable accionAlNavegarFacturas) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.registroFacturas = registroFacturas;

        add(new JLabel("Registro de facturas"));
        JButton botonFacturaAnterior = crearBotonCompacto("Anterior");
        JButton botonFacturaSiguiente = crearBotonCompacto("Siguiente");
        JButton botonGuardarFactura = crearBotonCompacto("Guardar");
        JButton botonNuevaFactura = crearBotonCompacto("Nuevo");
        JButton botonAbrirFacturaPdf = crearBotonCompacto("ABRIR FACTURA");
        add(botonFacturaAnterior);
        add(botonFacturaSiguiente);
        add(botonGuardarFactura);
        add(botonNuevaFactura);
        add(botonAbrirFacturaPdf);
        add(etiquetaEstadoGuardado);

        botonFacturaAnterior.setEnabled(registroFacturas.hayAnterior());
        botonFacturaSiguiente.setEnabled(registroFacturas.haySiguiente());
        botonFacturaAnterior.addActionListener(eventoBoton -> mostrarFacturaAnterior(accionAlNavegarFacturas));
        botonFacturaSiguiente.addActionListener(eventoBoton -> mostrarFacturaSiguiente(accionAlNavegarFacturas));
        botonGuardarFactura.addActionListener(eventoBoton -> guardarFacturaActual());
        botonNuevaFactura.addActionListener(eventoBoton -> mostrarNuevaFactura(accionAlNavegarFacturas));
        botonAbrirFacturaPdf.addActionListener(eventoBoton -> abrirFacturaComoPdf(carpetaPdfsFacturas));
        actualizarEstado();
    }

    // En el registro, crea un botón que cabe en la barra inferior de Facturación.
    private JButton crearBotonCompacto(String textoDelBoton) {
        JButton botonCompacto = new JButton(textoDelBoton);
        botonCompacto.setMargin(new Insets(2, 5, 2, 5));
        botonCompacto.setFont(botonCompacto.getFont().deriveFont(botonCompacto.getFont().getSize2D() - 1f));
        return botonCompacto;
    }

    // En el registro, solicita la factura anterior y refresca la pantalla.
    private void mostrarFacturaAnterior(Runnable accionAlNavegarFacturas) {
        registroFacturas.anterior();
        accionAlNavegarFacturas.run();
    }

    // En el registro, solicita la factura siguiente y refresca la pantalla.
    private void mostrarFacturaSiguiente(Runnable accionAlNavegarFacturas) {
        registroFacturas.siguiente();
        accionAlNavegarFacturas.run();
    }

    // En el registro, guarda la factura actual y confirma en pantalla si se guardó.
    private void guardarFacturaActual() {
        try {
            registroFacturas.guardar();
            actualizarEstado();
        } catch (IOException errorAlGuardarFactura) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar la factura: " + errorAlGuardarFactura.getMessage());
        }
    }

    // En el registro, solicita el próximo comprobante y muestra si hay que guardar antes.
    private void mostrarNuevaFactura(Runnable accionAlNavegarFacturas) {
        try {
            registroFacturas.nueva();
            accionAlNavegarFacturas.run();
        } catch (IllegalStateException errorAlCrearFactura) {
            JOptionPane.showMessageDialog(this, errorAlCrearFactura.getMessage());
        }
    }

    // En el registro, genera el PDF y lo abre en el visor de la computadora.
    private void abrirFacturaComoPdf(Path carpetaPdfsFacturas) {
        Path archivoPdfFactura;
        try {
            RegistroFacturas.Configuracion configuracionEmpresa = registroFacturas.configuracion();
            String numeroComprobanteFiscal = registroFacturas.ncfActual();
            Files.createDirectories(carpetaPdfsFacturas);
            Path rutaDelPdf = carpetaPdfsFacturas.resolve(numeroComprobanteFiscal + ".pdf");
            archivoPdfFactura = PdfFactura.generar(rutaDelPdf, numeroComprobanteFiscal,
                    registroFacturas.facturaActual(), configuracionEmpresa.empresa(),
                    configuracionEmpresa.direccion(), configuracionEmpresa.rnc());
        } catch (IOException errorAlGenerarPdf) {
            JOptionPane.showMessageDialog(this, "No se pudo generar el PDF: " + errorAlGenerarPdf.getMessage());
            return;
        }

        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new UnsupportedOperationException();
            }
            Desktop.getDesktop().open(archivoPdfFactura.toFile());
        } catch (IOException | UnsupportedOperationException errorAlAbrirVisor) {
            JOptionPane.showMessageDialog(this,
                    "El PDF quedó guardado en:\n" + archivoPdfFactura.toAbsolutePath());
        }
    }

    // En el registro, indica si la factura que se ve coincide con su última versión guardada.
    void actualizarEstado() {
        etiquetaEstadoGuardado.setText(registroFacturas.estaGuardada() ? "Guardada" : "Sin guardar");
    }
}
