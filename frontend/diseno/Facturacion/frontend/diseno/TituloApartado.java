package frontend.diseno;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

// En las pantallas Swing, presenta el encabezado visual de un apartado.
class TituloApartado extends JPanel {
    private static final float PUNTOS_ADICIONALES_TITULO = 4f;

    // En cada apartado, coloca el título a la izquierda y le aumenta el tamaño de letra.
    TituloApartado(String textoDelTitulo) {
        super(new FlowLayout(FlowLayout.LEFT));

        JLabel etiquetaTitulo = new JLabel(textoDelTitulo);
        etiquetaTitulo.setFont(etiquetaTitulo.getFont()
                .deriveFont(etiquetaTitulo.getFont().getSize2D() + PUNTOS_ADICIONALES_TITULO));
        add(etiquetaTitulo);
    }
}
