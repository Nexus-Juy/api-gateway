package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El gateway arranca y su health responde, sin config-server ni Eureka.
 */
class ApiGatewayApplicationTests extends GatewayTestBase {

    /** Si el contexto no arranca, este test falla antes de ejecutarse. */
    @Test
    @DisplayName("El contexto del gateway arranca")
    void contextoArranca() {
        assertThat(ServiciosFalsos.SECURITY.url()).startsWith("http://127.0.0.1:");
    }

    /**
     * Docker usa este endpoint para saber si el contenedor está sano.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("/actuator/health responde UP")
    void healthResponde() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/actuator/health", null, null);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }
}
