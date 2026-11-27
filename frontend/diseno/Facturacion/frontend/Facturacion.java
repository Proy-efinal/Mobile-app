package frontend;

import backend.RegistroFacturas;
import frontend.diseno.VentanaFacturacion;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// Inicia la aplicación y conecta la ventana con los datos guardados en esta PC.
public class Facturacion {
    // Es la primera instrucción que ejecuta Java al abrir el programa.
    public static void main(String[] argumentosDeInicio) {
        SwingUtilities.invokeLater(Facturacion::abrirVentanaPrincipal);
    }

    // Carga el registro local, prepara la carpeta de PDF y muestra la ventana.
    private static void abrirVentanaPrincipal() {
        try {
            Path ubicacionDelPrograma = Path.of(Facturacion.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path carpetaDelProyecto = Files.isRegularFile(ubicacionDelPrograma)
                    ? ubicacionDelPrograma.getParent() : Path.of("").toAbsolutePath();
            Path carpetaDeDatos = carpetaDelProyecto.resolve("base de datos");
            Path archivoDelRegistro = carpetaDeDatos.resolve("registro.properties");

            RegistroFacturas registroDeFacturas = new RegistroFacturas(archivoDelRegistro);
            Path carpetaDeFacturasPdf = carpetaDeDatos.resolve("facturas");
            VentanaFacturacion ventanaPrincipal = new VentanaFacturacion(registroDeFacturas, carpetaDeFacturasPdf);
            ventanaPrincipal.setVisible(true);
        } catch (Exception errorDeInicio) {
            JOptionPane.showMessageDialog(null, "No se pudo abrir el registro: " + errorDeInicio.getMessage());
        }
    }
}
