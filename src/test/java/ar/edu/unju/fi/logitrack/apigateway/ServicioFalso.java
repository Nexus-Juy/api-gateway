package ar.edu.unju.fi.logitrack.apigateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Microservicio de mentira para los tests del gateway.
 * <p>
 * Es un servidor HTTP mínimo (el que viene con el JDK) que contesta 200 con su nombre y guarda cada
 * petición que le llega. Con eso los tests comprueban a qué servicio mandó el gateway cada ruta, con qué
 * path y con qué header {@code Authorization}, sin necesitar Eureka ni los servicios reales.
 * </p>
 */
final class ServicioFalso {

    /**
     * Lo que recibió el servicio falso en una petición.
     *
     * @param metodo        método HTTP
     * @param path          path pedido, tal cual llegó
     * @param authorization valor del header {@code Authorization}, o {@code null} si no vino
     * @param cuerpo        cuerpo de la petición
     */
    record Peticion(String metodo, String path, String authorization, String cuerpo) {
    }

    private final String nombre;
    private final HttpServer servidor;
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();

    private ServicioFalso(String nombre) throws IOException {
        this.nombre = nombre;
        this.servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.servidor.createContext("/", this::atender);
        this.servidor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(0)));
    }

    /**
     * Levanta un servicio falso en un puerto libre.
     *
     * @param nombre nombre que devuelve en sus respuestas (por ejemplo {@code shipment-service})
     * @return el servicio ya escuchando
     */
    static ServicioFalso iniciar(String nombre) {
        try {
            return new ServicioFalso(nombre);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo levantar el servicio falso " + nombre, ex);
        }
    }

    /**
     * URL base del servicio falso, para usarla como destino de una ruta del gateway.
     *
     * @return por ejemplo {@code http://127.0.0.1:54321}
     */
    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    /**
     * Peticiones recibidas desde la última limpieza.
     *
     * @return lista en orden de llegada
     */
    List<Peticion> recibidas() {
        return List.copyOf(recibidas);
    }

    /**
     * Última petición recibida.
     *
     * @return la petición, o {@code null} si no llegó ninguna
     */
    Peticion ultima() {
        return recibidas.isEmpty() ? null : recibidas.getLast();
    }

    /** Olvida las peticiones recibidas, para empezar cada test de cero. */
    void limpiar() {
        recibidas.clear();
    }

    private void atender(HttpExchange exchange) throws IOException {
        String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        recibidas.add(new Peticion(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                cuerpo));

        byte[] respuesta = ("{\"servicio\":\"" + nombre + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, respuesta.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(respuesta);
        }
    }
}
