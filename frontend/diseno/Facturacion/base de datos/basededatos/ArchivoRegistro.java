package basededatos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

// Lee y guarda en la PC el archivo con las facturas, los catálogos y la configuración.
public class ArchivoRegistro {
    private final Path rutaDelRegistroLocal;

    // Recibe la ubicación del archivo local. Las pruebas pueden trabajar sin un archivo.
    public ArchivoRegistro(Path rutaDelRegistroLocal) {
        this.rutaDelRegistroLocal = rutaDelRegistroLocal;
    }

    // Recupera los datos guardados o devuelve un registro vacío en la primera ejecución.
    public Properties leer() throws IOException {
        Properties datosDelRegistro = new Properties();
        if (rutaDelRegistroLocal != null && Files.exists(rutaDelRegistroLocal)) {
            try (Reader lectorDelRegistro = Files.newBufferedReader(rutaDelRegistroLocal, StandardCharsets.UTF_8)) {
                datosDelRegistro.load(lectorDelRegistro);
            }
        }
        return datosDelRegistro;
    }

    // Escribe primero en un archivo temporal y luego lo coloca en el lugar del registro anterior.
    public void guardar(Properties datosDelRegistro) throws IOException {
        if (rutaDelRegistroLocal == null) {
            return;
        }

        Path carpetaDelRegistro = rutaDelRegistroLocal.toAbsolutePath().getParent();
        Path archivoTemporalDeGuardado = Files.createTempFile(carpetaDelRegistro, "facturas-", ".tmp");
        try {
            try (Writer escritorDelRegistro = Files.newBufferedWriter(archivoTemporalDeGuardado, StandardCharsets.UTF_8)) {
                datosDelRegistro.store(escritorDelRegistro, "Facturas registradas en esta PC");
            }
            Files.move(archivoTemporalDeGuardado, rutaDelRegistroLocal, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(archivoTemporalDeGuardado);
        }
    }
}
