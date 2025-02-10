import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServidorWebMultiHilos {
    private static final int PUERTO = 8080;
    private static final String DIRECTORIO_BASE = "src";
    private static final int MAX_HILOS = 10;

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_HILOS);

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);

            while (true) {
                Socket cliente = servidor.accept();
                pool.execute(new ManejadorCliente(cliente));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static class ManejadorCliente implements Runnable {
        private Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream salida = socket.getOutputStream()
            ) {
                String linea = entrada.readLine();
                if (linea != null && linea.startsWith("GET")) {
                    String[] partes = linea.split(" ");
                    String recurso = partes[1].equals("/") ? "/index.html" : partes[1];
                    File archivo = new File(DIRECTORIO_BASE + recurso);

                    if (archivo.exists() && !archivo.isDirectory()) {
                        enviarRespuesta(salida, 200, "OK", archivo);
                    } else {
                        enviarRespuesta(salida, 404, "Not Found", new File(DIRECTORIO_BASE + "/404.html"));
                    }
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error con cliente: " + e.getMessage());
            }
        }

        private void enviarRespuesta(OutputStream salida, int codigo, String mensaje, File archivo) throws IOException {
            String tipoMime = obtenerTipoMime(archivo.getName());
            byte[] contenido = leerArchivo(archivo);

            PrintWriter cabecera = new PrintWriter(salida, true);
            cabecera.println("HTTP/1.1 " + codigo + " " + mensaje);
            cabecera.println("Content-Type: " + tipoMime);
            cabecera.println("Content-Length: " + contenido.length);
            cabecera.println();
            salida.write(contenido);
            salida.flush();
        }

        private byte[] leerArchivo(File archivo) throws IOException {
            FileInputStream fis = new FileInputStream(archivo);
            byte[] contenido = fis.readAllBytes();
            fis.close();
            return contenido;
        }

        private String obtenerTipoMime(String nombreArchivo) {
            if (nombreArchivo.endsWith(".html")) return "text/html";
            if (nombreArchivo.endsWith(".css")) return "text/css";
            if (nombreArchivo.endsWith(".js")) return "application/javascript";
            if (nombreArchivo.endsWith(".png")) return "image/png";
            if (nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }
}
