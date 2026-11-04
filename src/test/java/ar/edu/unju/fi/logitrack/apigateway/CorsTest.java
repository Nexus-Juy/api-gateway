package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS centralizado en el gateway (S3-08).
 * <p>
 * Un navegador, antes de hacer un {@code POST} con token desde otro origen, manda una petición
 * {@code OPTIONS} (preflight). El gateway la contesta él mismo, sin pedir token y sin molestar a los
 * servicios.
 * </p>
 */
class CorsTest extends GatewayTestBase {

    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";

    private static final String ORIGEN_NO_PERMITIDO = "http://sitio-desconocido.example";

    /**
     * Criterio: un OPTIONS con un origen permitido responde con los headers CORS.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Preflight desde un origen permitido: responde con los headers CORS y sin pedir token")
    void preflightOrigenPermitido() throws Exception {
        HttpResponse<String> respuesta = preflight(ORIGEN_PERMITIDO, "POST");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).hasValue(ORIGEN_PERMITIDO);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Methods"))
                .hasValueSatisfying(metodos -> assertThat(metodos).contains("POST"));
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Headers"))
                .hasValueSatisfying(headers -> assertThat(headers.toLowerCase())
                        .contains("authorization")
                        .contains("content-type"));
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
    }

    /**
     * Criterio: un OPTIONS con un origen no permitido no incluye los headers CORS.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Preflight desde un origen no permitido: sin headers CORS")
    void preflightOrigenNoPermitido() throws Exception {
        HttpResponse<String> respuesta = preflight(ORIGEN_NO_PERMITIDO, "POST");

        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(respuesta.statusCode()).isNotEqualTo(200);
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
    }

    /**
     * DELETE no está entre los métodos permitidos para CORS.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Preflight con un método no permitido (DELETE): sin headers CORS")
    void preflightMetodoNoPermitido() throws Exception {
        HttpResponse<String> respuesta = preflight(ORIGEN_PERMITIDO, "DELETE");

        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    /**
     * Después del preflight, la petición real también lleva el header CORS en la respuesta.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Petición real desde un origen permitido: llega al servicio y vuelve con el header CORS")
    void peticionRealConOrigenPermitido() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", TokensDePrueba.bearer("CLIENTE"), null,
                Map.of("Origin", ORIGEN_PERMITIDO));

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).hasValue(ORIGEN_PERMITIDO);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).hasSize(1);
    }

    private HttpResponse<String> preflight(String origen, String metodo) throws Exception {
        return enviar("OPTIONS", "/api/v1/repartidores", null, null, Map.of(
                "Origin", origen,
                "Access-Control-Request-Method", metodo,
                "Access-Control-Request-Headers", "authorization,content-type"));
    }
}
