package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Base de los tests del gateway: lo levanta en un puerto libre y le arma las mismas tres rutas de
 * {@code api-gateway.yml}, pero apuntando a los {@link ServiciosFalsos} en lugar de a {@code lb://...}.
 * <p>
 * Las peticiones se hacen con el cliente HTTP del JDK, como lo haría Postman o Apidog desde afuera.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class GatewayTestBase {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${local.server.port}")
    private int puerto;

    /**
     * Rutas del gateway para los tests: mismos paths que en {@code api-gateway.yml}, con los servicios
     * falsos como destino.
     *
     * @param registro propiedades que se agregan al contexto de Spring del test
     */
    @DynamicPropertySource
    static void rutasHaciaServiciosFalsos(DynamicPropertyRegistry registro) {
        ruta(registro, 0, "security-service", ServiciosFalsos.SECURITY, "/api/v1/auth/**");
        ruta(registro, 1, "courier-service", ServiciosFalsos.COURIER, "/api/v1/repartidores/**");
        ruta(registro, 2, "shipment-service", ServiciosFalsos.SHIPMENT, "/api/v1/envios/**");
    }

    private static void ruta(DynamicPropertyRegistry registro, int indice, String id,
                             ServicioFalso servicio, String path) {
        String prefijo = "spring.cloud.gateway.server.webflux.routes[" + indice + "].";
        registro.add(prefijo + "id", () -> id);
        registro.add(prefijo + "uri", servicio::url);
        registro.add(prefijo + "predicates[0]", () -> "Path=" + path);
    }

    /** Cada test arranca con los servicios falsos sin peticiones registradas. */
    @BeforeEach
    void limpiarServiciosFalsos() {
        ServiciosFalsos.limpiar();
    }

    /**
     * Hace una petición al gateway.
     *
     * @param metodo        método HTTP
     * @param path          path a pedir, por ejemplo {@code /api/v1/envios}
     * @param authorization valor del header {@code Authorization}, o {@code null} para no mandarlo
     * @param cuerpo        cuerpo JSON, o {@code null} si la petición no lleva cuerpo
     * @return la respuesta del gateway
     * @throws IOException          si falla la conexión
     * @throws InterruptedException si se interrumpe la espera
     */
    protected HttpResponse<String> enviar(String metodo, String path, String authorization, String cuerpo)
            throws IOException, InterruptedException {
        return enviar(metodo, path, authorization, cuerpo, Map.of());
    }

    /**
     * Hace una petición al gateway con headers adicionales.
     *
     * @param metodo        método HTTP
     * @param path          path a pedir
     * @param authorization valor del header {@code Authorization}, o {@code null} para no mandarlo
     * @param cuerpo        cuerpo JSON, o {@code null} si la petición no lleva cuerpo
     * @param headers       otros headers (por ejemplo {@code Origin})
     * @return la respuesta del gateway
     * @throws IOException          si falla la conexión
     * @throws InterruptedException si se interrumpe la espera
     */
    protected HttpResponse<String> enviar(String metodo, String path, String authorization, String cuerpo,
                                          Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publicador = cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(cuerpo);

        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + path))
                .timeout(Duration.ofSeconds(10))
                .method(metodo, publicador);
        if (authorization != null) {
            peticion.header("Authorization", authorization);
        }
        if (cuerpo != null) {
            peticion.header("Content-Type", "application/json");
        }
        headers.forEach(peticion::header);

        return CLIENTE.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}
