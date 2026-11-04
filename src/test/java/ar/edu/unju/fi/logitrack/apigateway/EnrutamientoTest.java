package ar.edu.unju.fi.logitrack.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enrutamiento (S3-05): cada path llega a su microservicio, sin reescribir el path.
 */
class EnrutamientoTest extends GatewayTestBase {

    private static final String LOGIN = "{\"usernameOrEmail\":\"admin.demo\",\"password\":\"admin123\"}";

    /**
     * El login entra por el gateway y llega a security-service con el mismo cuerpo.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("/api/v1/auth/** llega a security-service")
    void authLlegaASecurity() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/v1/auth/login", null, LOGIN);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("security-service");
        ServicioFalso.Peticion recibida = ServiciosFalsos.SECURITY.ultima();
        assertThat(recibida.metodo()).isEqualTo("POST");
        assertThat(recibida.path()).isEqualTo("/api/v1/auth/login");
        assertThat(recibida.cuerpo()).isEqualTo(LOGIN);
    }

    /**
     * El listado de repartidores llega a courier-service.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("/api/v1/repartidores/** llega a courier-service")
    void repartidoresLleganACourier() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/repartidores", null, null);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("courier-service");
        assertThat(ServiciosFalsos.COURIER.ultima().path()).isEqualTo("/api/v1/repartidores");
    }

    /**
     * La consulta de un envío llega a shipment-service con el path completo.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("/api/v1/envios/** llega a shipment-service")
    void enviosLleganAShipment() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/envios/123/historial", null, null);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("shipment-service");
        assertThat(ServiciosFalsos.SHIPMENT.ultima().path()).isEqualTo("/api/v1/envios/123/historial");
    }

    /**
     * Un path que no es de ningún servicio no se reenvía a nadie.
     *
     * @throws Exception si falla la petición
     */
    @Test
    @DisplayName("Un path sin ruta responde 404 y no llega a ningún servicio")
    void pathSinRutaDa404() throws Exception {
        HttpResponse<String> respuesta = enviar("GET", "/api/v1/otra-cosa", null, null);

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(ServiciosFalsos.SECURITY.recibidas()).isEmpty();
        assertThat(ServiciosFalsos.COURIER.recibidas()).isEmpty();
        assertThat(ServiciosFalsos.SHIPMENT.recibidas()).isEmpty();
    }
}
