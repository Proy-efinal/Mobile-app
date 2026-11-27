package backend;

import java.util.Locale;

// Tipos de factura: reúne las cinco series del programa con sus códigos y nombres.
public enum TipoFactura {
    // Crédito fiscal: identifica la serie B01.
    FISCAL("B01", "Factura de Crédito Fiscal"),
    // Consumo: identifica la serie B02.
    CONSUMO("B02", "Factura de Consumo"),
    // Comprobantes gubernamentales: identifican la serie B15.
    GUBERNAMENTAL("B15", "Comprobante Gubernamental"),
    // Notas de crédito: identifican la serie B04.
    CREDITO("B04", "Notas de Crédito"),
    // Notas de débito: identifican la serie B03.
    DEBITO("B03", "Notas de Débito");

    private final String codigoDeLaSerie;
    private final String nombreDelComprobante;

    // Tipos de factura: asocia un código de serie con el nombre del comprobante.
    TipoFactura(String codigoDeLaSerie, String nombreDelComprobante) {
        this.codigoDeLaSerie = codigoDeLaSerie;
        this.nombreDelComprobante = nombreDelComprobante;
    }

    // Registro: une el código de serie y un número de ocho dígitos para formar el NCF.
    public String ncf(int numeroSecuencial) {
        return codigoDeLaSerie + String.format(Locale.ROOT, "%08d", numeroSecuencial);
    }

    // Selector y PDF: representa el tipo con el código y el nombre del comprobante.
    @Override
    public String toString() {
        return codigoDeLaSerie + " - " + nombreDelComprobante;
    }
}
