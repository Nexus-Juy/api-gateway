package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Set;

/**
 * Qué roles pueden usar cada ruta del gateway (S3-07).
 * <p>
 * Son las reglas generales; la autorización fina y la de pertenencia (un cliente solo ve sus envíos, un
 * repartidor solo los asignados) siguen en cada servicio. Las reglas se revisan en orden y se aplica la
 * primera que coincide con el path y el método:
 * </p>
 * <ul>
 *   <li>{@code POST}, {@code PUT}, {@code PATCH} y {@code DELETE} en {@code /api/v1/repartidores/**}: solo
 *       {@code ADMIN}.</li>
 *   <li>Cualquier otro método en {@code /api/v1/repartidores/**} (las consultas): los tres roles.</li>
 *   <li>{@code /api/v1/envios/**}: los tres roles.</li>
 * </ul>
 * <p>
 * Si ninguna regla coincide, alcanza con tener uno de los tres roles.
 * </p>
 */
@Component
public class ReglasDeAcceso {

    /**
     * Una regla de acceso.
     *
     * @param patron  paths a los que se aplica
     * @param metodos métodos a los que se aplica; vacío quiere decir todos
     * @param roles   roles que pueden pasar
     */
    private record Regla(PathPattern patron, Set<HttpMethod> metodos, Set<String> roles) {

        boolean aplica(HttpMethod metodo, PathContainer path) {
            return patron.matches(path) && (metodos.isEmpty() || metodos.contains(metodo));
        }
    }

    private static final Set<HttpMethod> ESCRITURA =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final List<Regla> reglas = List.of(
            regla("/api/v1/repartidores/**", ESCRITURA, Set.of(Roles.ADMIN)),
            regla("/api/v1/repartidores/**", Set.of(), Roles.TODOS),
            regla("/api/v1/envios/**", Set.of(), Roles.TODOS));

    /**
     * Indica si el usuario puede hacer esa petición.
     *
     * @param metodo  método HTTP de la petición
     * @param path    path de la petición
     * @param usuario usuario que viene en el token
     * @return {@code true} si alguno de sus roles alcanza para la ruta y el método
     */
    public boolean permite(HttpMethod metodo, PathContainer path, UsuarioAutenticado usuario) {
        Set<String> rolesPermitidos = reglas.stream()
                .filter(regla -> regla.aplica(metodo, path))
                .findFirst()
                .map(Regla::roles)
                .orElse(Roles.TODOS);
        return usuario.tieneAlgunRol(rolesPermitidos);
    }

    private static Regla regla(String patron, Set<HttpMethod> metodos, Set<String> roles) {
        return new Regla(PathPatternParser.defaultInstance.parse(patron), metodos, roles);
    }
}
