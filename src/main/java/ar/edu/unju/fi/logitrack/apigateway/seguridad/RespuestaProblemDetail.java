package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Arma las respuestas de error del gateway en formato {@code ProblemDetail} (RFC 7807).
 * <p>
 * Usa los mismos campos y títulos que los microservicios ({@code No autorizado} para 401 y
 * {@code Acceso denegado} para 403), así quien consume la API recibe el mismo formato venga el error del
 * gateway o del servicio.
 * </p>
 */
public final class RespuestaProblemDetail {

    private RespuestaProblemDetail() {
    }

    /**
     * Corta la petición con un 401.
     *
     * @param exchange petición en curso
     * @param detalle  explicación para quien hizo la petición
     * @return la escritura de la respuesta
     */
    public static Mono<Void> noAutorizado(ServerWebExchange exchange, String detalle) {
        return escribir(exchange, HttpStatus.UNAUTHORIZED, "No autorizado", detalle);
    }

    /**
     * Corta la petición con un 403.
     *
     * @param exchange petición en curso
     * @param detalle  explicación para quien hizo la petición
     * @return la escritura de la respuesta
     */
    public static Mono<Void> accesoDenegado(ServerWebExchange exchange, String detalle) {
        return escribir(exchange, HttpStatus.FORBIDDEN, "Acceso denegado", detalle);
    }

    private static Mono<Void> escribir(ServerWebExchange exchange, HttpStatus status, String titulo,
                                       String detalle) {
        String path = exchange.getRequest().getPath().value();
        String json = "{"
                + campo("type", "about:blank") + ","
                + campo("title", titulo) + ","
                + "\"status\":" + status.value() + ","
                + campo("detail", detalle) + ","
                + campo("instance", path) + ","
                + campo("timestamp", Instant.now().toString()) + ","
                + campo("path", path)
                + "}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String campo(String nombre, String valor) {
        return "\"" + nombre + "\":\"" + escapar(valor) + "\"";
    }

    private static String escapar(String valor) {
        StringBuilder resultado = new StringBuilder(valor.length());
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                default -> {
                    if (caracter < 0x20) {
                        resultado.append("\\u%04x".formatted((int) caracter));
                    } else {
                        resultado.append(caracter);
                    }
                }
            }
        }
        return resultado.toString();
    }
}
