package basededatos;

// Guarda el nombre, teléfono, RNC o cédula y nota de un cliente.
public record Cliente(String nombre, String telefono, String documento, String nota) {
    // Usa el nombre como texto visible cuando se muestra el cliente en un selector.
    @Override
    public String toString() {
        return nombre;
    }
}
