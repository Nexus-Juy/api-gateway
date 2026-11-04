package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtro global de JWT (S3-06) con el gateway levantado: los 401 salen del gateway y la petición no llega
 * al servicio; con un token válido la petición llega con el mismo header {@code Authorization}.
 */
class FiltroJwtGlobalTest extends GatewayTestBase {

    private static final String LOGIN = "{\"usernameOrEmail\":\"admin.demo\",\"password\":\"admin123\"}";

    /**
     * Con un token válido la petición llega a shipment-service y el header viaja sin cambios.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Token válido: la petición llega al servicio con el mismo Authorization")
    void tokenValidoLlegaAlServicio() throws Exception {
        String authorization = TokensDePrueba.bearer("ADMIN");

        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", authorization, null);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.SHIPMENT.ultima().authorization()).isEqualTo(authorization);
    }

    /**
     * Criterio de aceptación: sin token, 401 desde el gateway.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Sin token: 401 desde el gateway y no llega a shipment-service")
    void sinToken() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", null, null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    /**
     * Un header que no es Bearer no sirve.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Authorization que no es Bearer: 401")
    void headerQueNoEsBearer() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/repartidores", "Basic dXN1YXJpbzpjbGF2ZQ==", null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
    }

    /**
     * Texto que no es un JWT.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Token mal formado: 401")
    void tokenMalFormado() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", "Bearer esto-no-es-un-token", null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    /**
     * Bearer sin nada después.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Bearer vacío: 401")
    void bearerVacio() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", "Bearer ", null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    /**
     * Token con la firma correcta pero vencido.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Token vencido: 401")
    void tokenVencido() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios", "Bearer " + TokensDePrueba.vencido(), null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    /**
     * Token firmado con otro secreto.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Token con la firma alterada: 401")
    void tokenConFirmaAlterada() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios",
                "Bearer " + TokensDePrueba.conOtraFirma(), null);

        assertNoAutorizado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    /**
     * Criterio de aceptación: el login por el gateway funciona sin token.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Ruta pública: el login llega a security-service sin token")
    void loginSinToken() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/auth/login", null, LOGIN);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.SECURITY.ultima().path()).isEqualTo("/api/v1/auth/login");
    }

    /**
     * En una ruta pública, un token vencido no molesta: el gateway no lo revisa.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Ruta pública: un token vencido no impide el login")
    void loginConTokenVencido() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/auth/login",
                "Bearer " + TokensDePrueba.vencido(), LOGIN);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.SECURITY.recibidas()).hasSize(1);
    }

    private static void assertNoAutorizado(HttpResponse<String> respuesta) {
        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).startsWith("application/problem+json"));
        assertThat(respuesta.body())
                .contains("\"title\":\"No autorizado\"")
                .contains("\"status\":401");
    }
}
