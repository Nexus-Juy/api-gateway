package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autorización por roles (S3-07) con el gateway levantado: los criterios de aceptación de la issue.
 */
class FiltroRolesGlobalTest extends GatewayTestBase {

    private static final String REPARTIDOR_NUEVO =
            "{\"nombre\":\"Lucía Fernández\",\"tipoVehiculo\":\"MOTO\",\"email\":\"lucia@logitrack.test\"}";

    private static final String ENVIO_NUEVO = "{\"clienteId\":\"abc\"}";

    /**
     * Criterio: un CLIENTE que da de alta un repartidor recibe 403 desde el gateway.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("CLIENTE hace POST /api/v1/repartidores: 403 desde el gateway")
    void clienteNoPuedeCrearRepartidor() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/repartidores",
                TokensDePrueba.bearer("CLIENTE"), REPARTIDOR_NUEVO);

        assertAccesoDenegado(respuesta);
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
    }

    /**
     * Criterio: un ADMIN que da de alta un repartidor llega a courier-service.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("ADMIN hace POST /api/v1/repartidores: llega a courier-service")
    void adminCreaRepartidor() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/repartidores",
                TokensDePrueba.bearer("ADMIN"), REPARTIDOR_NUEVO);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.COURIER.ultima().metodo()).isEqualTo("POST");
        assertThat(ServiciosFalsos.COURIER.ultima().cuerpo()).isEqualTo(REPARTIDOR_NUEVO);
    }

    /**
     * Criterio: un REPARTIDOR que registra un envío pasa el gateway; el 403 lo da shipment-service con su
     * propia regla (acá el servicio falso solo confirma que la petición llegó).
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("REPARTIDOR hace POST /api/v1/envios: el gateway lo deja pasar a shipment-service")
    void repartidorLlegaAShipment() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/envios",
                TokensDePrueba.bearer("REPARTIDOR"), ENVIO_NUEVO);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).hasSize(1);
    }

    /**
     * Las consultas de repartidores están abiertas a los tres roles.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("CLIENTE hace GET /api/v1/repartidores: llega a courier-service")
    void clienteConsultaRepartidores() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/repartidores",
                TokensDePrueba.bearer("CLIENTE"), null);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(ServiciosFalsos.COURIER.recibidas()).hasSize(1);
    }

    /**
     * Un REPARTIDOR no puede cambiar el estado de otro repartidor en courier-service.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("REPARTIDOR hace PATCH /api/v1/repartidores/{id}/estado: 403")
    void repartidorNoCambiaEstadoDeRepartidor() throws Exception {
        HttpResponse<String> respuesta = enviar("PATCH", "/api/v1/repartidores/abc/estado",
                TokensDePrueba.bearer("REPARTIDOR"), "{\"estado\":\"INACTIVO\"}");

        assertAccesoDenegado(respuesta);
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
    }

    /**
     * Un token válido pero sin roles no pasa a ninguna ruta protegida.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Token sin roles: 403")
    void tokenSinRoles() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios",
                "Bearer " + TokensDePrueba.sinRoles(), null);

        assertAccesoDenegado(respuesta);
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }

    private static void assertAccesoDenegado(HttpResponse<String> respuesta) {
        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).startsWith("application/problem+json"));
        assertThat(respuesta.body())
                .contains("\"title\":\"Acceso denegado\"")
                .contains("\"status\":403");
    }
}
