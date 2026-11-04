package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Valida los JWT que emite security-service (S3-06).
 * <p>
 * Revisa la firma HS256 y el vencimiento con el mismo secreto que usan los servicios
 * ({@code JWT_SECRET}). El secreto no tiene valor por defecto: si falta, o tiene menos de 32 caracteres,
 * el gateway no arranca.
 * </p>
 */
@Component
public class JwtValidador {

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;

    /** Nombre del claim donde security-service guarda los roles. */
    static final String CLAIM_ROLES = "roles";

    private final SecretKey clave;

    /**
     * Prepara la clave de firma a partir del secreto compartido.
     *
     * @param secreto valor de {@code jwt.secret}, que en Docker llega por la variable {@code JWT_SECRET}
     * @throws IllegalStateException si el secreto falta o es demasiado corto
     */
    public JwtValidador(@Value("${jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (variable JWT_SECRET) debe tener al menos %d caracteres."
                            .formatted(LONGITUD_MINIMA_SECRETO_BYTES));
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida el token y saca el usuario y sus roles.
     *
     * @param token el JWT, sin el prefijo {@code Bearer}
     * @return el usuario del token
     * @throws JwtException             si el token está mal formado, vencido, con la firma alterada o no es HS256
     * @throws IllegalArgumentException si el token está vacío
     */
    public UsuarioAutenticado validar(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(clave)
                .build()
                .parseClaimsJws(token);

        if (!SignatureAlgorithm.HS256.getValue().equals(jws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("El token tiene que estar firmado con HS256.");
        }

        Claims claims = jws.getBody();
        return new UsuarioAutenticado(claims.getSubject(), leerRoles(claims.get(CLAIM_ROLES)));
    }

    private static List<String> leerRoles(Object valor) {
        if (valor instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
