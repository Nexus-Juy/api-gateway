package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtro global que revisa los roles del usuario antes de enrutar (S3-07).
 * <p>
 * Corre después de {@link FiltroJwtGlobal} y usa el usuario que ese filtro dejó guardado en la petición.
 * Si el rol no alcanza según {@link ReglasDeAcceso}, responde 403 desde el gateway y la petición no llega
 * al servicio. Las rutas públicas no tienen usuario guardado, así que pasan sin revisar roles.
 * </p>
 */
@Component
public class FiltroRolesGlobal implements GlobalFilter, Ordered {

    /** Justo después del filtro de JWT. */
    public static final int ORDEN = FiltroJwtGlobal.ORDEN + 10;

    private final ReglasDeAcceso reglas;

    /**
     * Crea el filtro.
     *
     * @param reglas qué roles pueden usar cada ruta
     */
    public FiltroRolesGlobal(ReglasDeAcceso reglas) {
        this.reglas = reglas;
    }

    /**
     * Deja pasar la petición solo si el rol del usuario alcanza.
     *
     * @param exchange petición en curso
     * @param chain    resto de los filtros del gateway
     * @return la continuación de la petición, o la respuesta 403
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        UsuarioAutenticado usuario = exchange.getAttribute(FiltroJwtGlobal.ATRIBUTO_USUARIO);
        if (usuario == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        if (reglas.permite(request.getMethod(), request.getPath().pathWithinApplication(), usuario)) {
            return chain.filter(exchange);
        }
        return RespuestaProblemDetail.accesoDenegado(exchange,
                "El rol del token no tiene permiso para realizar esta operación.");
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
}
