package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtro global que valida el JWT antes de enrutar la petición (S3-06).
 * <p>
 * Lee el header {@code Authorization: Bearer <token>}. Si el token falta, está mal formado, vencido o con
 * la firma alterada, responde 401 desde el gateway y la petición no llega al servicio. Si es válido, deja
 * el usuario y sus roles guardados en la petición (para el filtro de roles) y reenvía el header
 * {@code Authorization} sin tocarlo, porque los servicios siguen validando el token por su cuenta.
 * </p>
 * <p>
 * Las rutas de {@code /api/v1/auth/**} (login y registro) y las preflight de CORS no piden token.
 * </p>
 */
@Component
public class FiltroJwtGlobal implements GlobalFilter, Ordered {

    /** Clave con la que el usuario del token queda guardado en los atributos de la petición. */
    public static final String ATRIBUTO_USUARIO = "logitrack.usuarioAutenticado";

    /** Corre antes que los filtros que reenvían la petición al servicio. */
    public static final int ORDEN = -100;

    private static final String PREFIJO_BEARER = "Bearer ";

    private static final List<PathPattern> RUTAS_PUBLICAS = List.of(
            PathPatternParser.defaultInstance.parse("/api/v1/auth/**"),
            PathPatternParser.defaultInstance.parse("/actuator/health/**"));

    private final JwtValidador validador;

    /**
     * Crea el filtro.
     *
     * @param validador valida la firma y el vencimiento del token
     */
    public FiltroJwtGlobal(JwtValidador validador) {
        this.validador = validador;
    }

    /**
     * Deja pasar las rutas públicas y exige un token válido en todas las demás.
     *
     * @param exchange petición en curso
     * @param chain    resto de los filtros del gateway
     * @return la continuación de la petición, o la respuesta 401
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (CorsUtils.isPreFlightRequest(request) || esRutaPublica(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return RespuestaProblemDetail.noAutorizado(exchange,
                    "Se requiere un token JWT válido en el header Authorization (Bearer <token>).");
        }

        UsuarioAutenticado usuario;
        try {
            usuario = validador.validar(header.substring(PREFIJO_BEARER.length()).trim());
        } catch (JwtException | IllegalArgumentException ex) {
            return RespuestaProblemDetail.noAutorizado(exchange,
                    "El token JWT no es válido o está vencido.");
        }

        exchange.getAttributes().put(ATRIBUTO_USUARIO, usuario);
        return chain.filter(exchange);
    }

    /**
     * Orden del filtro dentro de la cadena del gateway.
     *
     * @return {@link #ORDEN}
     */
    @Override
    public int getOrder() {
        return ORDEN;
    }

    private static boolean esRutaPublica(ServerHttpRequest request) {
        PathContainer path = request.getPath().pathWithinApplication();
        return RUTAS_PUBLICAS.stream().anyMatch(patron -> patron.matches(path));
    }
}
