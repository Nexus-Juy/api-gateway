package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las reglas de acceso por rol (S3-07), por rol, ruta y método.
 */
class ReglasDeAccesoTest {

    private final ReglasDeAcceso reglas = new ReglasDeAcceso();

    /**
     * Tabla completa de la issue: cada fila es método, path, rol y si puede pasar.
     *
     * @param metodo   método HTTP
     * @param path     path pedido
     * @param rol      rol del token
     * @param permitido resultado esperado
     */
    @ParameterizedTest(name = "{0} {1} con {2} -> {3}")
    @CsvSource({
            "POST,   /api/v1/repartidores,                ADMIN,      true",
            "POST,   /api/v1/repartidores,                CLIENTE,    false",
            "POST,   /api/v1/repartidores,                REPARTIDOR, false",
            "PUT,    /api/v1/repartidores/abc,            ADMIN,      true",
            "PUT,    /api/v1/repartidores/abc,            CLIENTE,    false",
            "PATCH,  /api/v1/repartidores/abc/estado,     ADMIN,      true",
            "PATCH,  /api/v1/repartidores/abc/estado,     REPARTIDOR, false",
            "DELETE, /api/v1/repartidores/abc,            CLIENTE,    false",
            "GET,    /api/v1/repartidores,                ADMIN,      true",
            "GET,    /api/v1/repartidores,                CLIENTE,    true",
            "GET,    /api/v1/repartidores/abc/disponibilidad, REPARTIDOR, true",
            "GET,    /api/v1/envios,                      ADMIN,      true",
            "GET,    /api/v1/envios,                      CLIENTE,    true",
            "GET,    /api/v1/envios,                      REPARTIDOR, true",
            "POST,   /api/v1/envios,                      REPARTIDOR, true",
            "PATCH,  /api/v1/envios/abc/estado,           CLIENTE,    true",
            "GET,    /api/v1/envios/seguimiento/LT-123,   OTRO_ROL,   false"
    })
    void reglasPorRolRutaYMetodo(String metodo, String path, String rol, boolean permitido) {
        UsuarioAutenticado usuario = new UsuarioAutenticado("usuario-1", List.of(rol));

        boolean resultado = reglas.permite(HttpMethod.valueOf(metodo), PathContainer.parsePath(path), usuario);

        assertThat(resultado).isEqualTo(permitido);
    }

    /** Un token sin roles no pasa por ninguna ruta protegida. */
    @Test
    @DisplayName("Sin roles no se pasa, ni siquiera a una consulta")
    void sinRolesNoPasa() {
        UsuarioAutenticado usuario = new UsuarioAutenticado("usuario-1", List.of());

        boolean resultado = reglas.permite(HttpMethod.GET, PathContainer.parsePath("/api/v1/envios"), usuario);

        assertThat(resultado).isFalse();
    }

    /** Una ruta sin regla propia acepta a cualquiera de los tres roles. */
    @Test
    @DisplayName("Una ruta sin regla propia acepta a los tres roles")
    void rutaSinReglaAceptaLosTresRoles() {
        UsuarioAutenticado usuario = new UsuarioAutenticado("usuario-1", List.of(Roles.REPARTIDOR));

        boolean resultado = reglas.permite(HttpMethod.GET, PathContainer.parsePath("/api/v1/otra"), usuario);

        assertThat(resultado).isTrue();
    }
}
