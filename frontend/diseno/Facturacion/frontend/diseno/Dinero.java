package frontend.diseno;

import java.math.BigDecimal;

// Da el mismo aspecto a los importes en las pantallas y en el PDF.
class Dinero {
    // Presenta un importe con el símbolo RD$ y dos decimales.
    static String formatearPesosDominicanos(BigDecimal importeEnPesos) {
        return "RD$ " + importeEnPesos.setScale(2).toPlainString();
    }
}
