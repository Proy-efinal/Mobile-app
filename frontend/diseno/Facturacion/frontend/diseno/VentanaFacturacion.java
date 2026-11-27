package frontend.diseno;

import backend.Factura;
import backend.RegistroFacturas;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.nio.file.Path;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

// Es la ventana principal. Reúne la barra de menú y los cuatro apartados de la aplicación.
public class VentanaFacturacion extends JFrame {
    private static final int ESPACIO_VERTICAL_ENTRE_APARTADOS = 18;
    private static final String PANTALLA_FACTURACION = "Facturación";
    private static final String PANTALLA_INVENTARIO = "Inventario";
    private static final String PANTALLA_CLIENTES = "Clientes";
    private static final String PANTALLA_CONFIGURACION = "Configuración";

    private final RegistroFacturas registroDeFacturas;
    private final Path carpetaDeFacturasPdf;
    private final CardLayout selectorDePantallas = new CardLayout();
    private final JPanel contenedorDePantallas = new JPanel(selectorDePantallas);
    private final JPanel pantallaDeFacturacion = new JPanel(new BorderLayout());

    // Construye la ventana, abre Facturación y fija un tamaño mínimo para que quepan los controles.
    public VentanaFacturacion(RegistroFacturas registroDeFacturas, Path carpetaDeFacturasPdf) {
        super("FACTURACIÓN");
        this.registroDeFacturas = registroDeFacturas;
        this.carpetaDeFacturasPdf = carpetaDeFacturasPdf;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        PanelInventario pantallaDeInventario = new PanelInventario(registroDeFacturas);
        PanelClientes pantallaDeClientes = new PanelClientes(registroDeFacturas);

        contenedorDePantallas.add(pantallaDeFacturacion, PANTALLA_FACTURACION);
        contenedorDePantallas.add(pantallaDeInventario, PANTALLA_INVENTARIO);
        contenedorDePantallas.add(pantallaDeClientes, PANTALLA_CLIENTES);
        contenedorDePantallas.add(new PanelConfiguracion(registroDeFacturas), PANTALLA_CONFIGURACION);
        setContentPane(contenedorDePantallas);
        setJMenuBar(crearBarraDeMenu(pantallaDeInventario, pantallaDeClientes));

        mostrarFacturaActual();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    // Crea los botones de arriba en el mismo orden en que se ven en la ventana.
    private JMenuBar crearBarraDeMenu(PanelInventario pantallaDeInventario, PanelClientes pantallaDeClientes) {
        JMenuBar barraDeMenu = new JMenuBar();
        JMenuItem opcionFacturacion = new JMenuItem(PANTALLA_FACTURACION);
        opcionFacturacion.addActionListener(eventoDeMenu -> mostrarFacturaActual());
        barraDeMenu.add(opcionFacturacion);

        JMenuItem opcionInventario = new JMenuItem(PANTALLA_INVENTARIO);
        opcionInventario.addActionListener(eventoDeMenu -> {
            pantallaDeInventario.actualizar();
            selectorDePantallas.show(contenedorDePantallas, PANTALLA_INVENTARIO);
        });
        barraDeMenu.add(opcionInventario);

        JMenuItem opcionClientes = new JMenuItem(PANTALLA_CLIENTES);
        opcionClientes.addActionListener(eventoDeMenu -> {
            pantallaDeClientes.actualizar();
            selectorDePantallas.show(contenedorDePantallas, PANTALLA_CLIENTES);
        });
        barraDeMenu.add(opcionClientes);

        JMenuItem opcionConfiguracion = new JMenuItem(PANTALLA_CONFIGURACION);
        opcionConfiguracion.addActionListener(eventoDeMenu ->
                selectorDePantallas.show(contenedorDePantallas, PANTALLA_CONFIGURACION));
        barraDeMenu.add(opcionConfiguracion);
        return barraDeMenu;
    }

    // Arma la factura seleccionada de arriba abajo y coloca los botones del registro al final.
    private void mostrarFacturaActual() {
        Factura facturaActiva = registroDeFacturas.facturaActual();
        PanelRegistroFacturas controlesDelRegistro = new PanelRegistroFacturas(
                registroDeFacturas, carpetaDeFacturasPdf, this::mostrarFacturaActual);
        JPanel apartadosDeLaFactura = new JPanel();
        apartadosDeLaFactura.setLayout(new BoxLayout(apartadosDeLaFactura, BoxLayout.Y_AXIS));

        // Primero se muestran el tipo de factura, el NCF y la fecha.
        apartadosDeLaFactura.add(new PanelTipoFactura(registroDeFacturas.tipoActual(),
                registroDeFacturas.ncfActual(), facturaActiva.fecha(), tipoElegido -> {
                    registroDeFacturas.seleccionarTipo(tipoElegido);
                    mostrarFacturaActual();
                }));
        apartadosDeLaFactura.add(Box.createVerticalStrut(ESPACIO_VERTICAL_ENTRE_APARTADOS));

        // Debajo se coloca el buscador y la información del cliente.
        apartadosDeLaFactura.add(new PanelCliente(registroDeFacturas, controlesDelRegistro::actualizarEstado));
        apartadosDeLaFactura.add(Box.createVerticalStrut(ESPACIO_VERTICAL_ENTRE_APARTADOS));

        // La tabla central permite revisar los artículos de la factura.
        PanelArticulos tablaDeArticulos = new PanelArticulos(facturaActiva);
        apartadosDeLaFactura.add(tablaDeArticulos);
        apartadosDeLaFactura.add(Box.createVerticalStrut(ESPACIO_VERTICAL_ENTRE_APARTADOS));

        // Al final van los pagos y los totales, seguidos del registro de facturas.
        apartadosDeLaFactura.add(new PanelCierre(facturaActiva, tablaDeArticulos, controlesDelRegistro::actualizarEstado));
        pantallaDeFacturacion.removeAll();
        pantallaDeFacturacion.add(apartadosDeLaFactura, BorderLayout.CENTER);
        pantallaDeFacturacion.add(controlesDelRegistro, BorderLayout.SOUTH);
        pantallaDeFacturacion.revalidate();
        pantallaDeFacturacion.repaint();
        selectorDePantallas.show(contenedorDePantallas, PANTALLA_FACTURACION);
    }
}
