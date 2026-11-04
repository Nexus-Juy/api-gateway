package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del validador de JWT (S3-06), sin levantar Spring.
 */
class JwtValidadorTest {

    private static final String SECRETO = "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";

    private final JwtValidador validador = new JwtValidador(SECRETO);

    /** Sin secreto el gateway no puede arrancar. */
    @Test
    @DisplayName("Sin secreto, el validador no se crea")
    void rechazaSecretoNulo() {
        assertThatThrownBy(() -> new JwtValidador(null))
                .isInstanceOf(IllegalStateException.class);
    }

    /** HS256 necesita al menos 32 caracteres de secreto. */
    @Test
    @DisplayName("Con un secreto de menos de 32 caracteres, el validador no se crea")
    void rechazaSecretoCorto() {
        assertThatThrownBy(() -> new JwtValidador("corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    /** Un token bien firmado devuelve el usuario y sus roles. */
    @Test
    @DisplayName("Un token válido devuelve el sub y los roles")
    void leeUsuarioYRoles() {
        String token = firmar(SECRETO, "usuario-1", List.of("CLIENTE"), SignatureAlgorithm.HS256);

        UsuarioAutenticado usuario = validador.validar(token);

        assertThat(usuario.usuarioId()).isEqualTo("usuario-1");
        assertThat(usuario.roles()).containsExactly("CLIENTE");
    }

    /** Un token sin el claim roles queda con la lista vacía. */
    @Test
    @DisplayName("Un token sin roles queda con la lista de roles vacía")
    void tokenSinRoles() {
        String token = Jwts.builder()
                .setSubject("usuario-2")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(clave(SECRETO), SignatureAlgorithm.HS256)
                .compact();

        assertThat(validador.validar(token).roles()).isEmpty();
    }

    /** Si la firma no coincide con el secreto, el token se rechaza. */
    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza")
    void rechazaOtraFirma() {
        String token = firmar("OtraClaveDistintaQueNoEsLaDelGatewayParaProbarFirmas2026!!", "usuario-3",
                List.of("ADMIN"), SignatureAlgorithm.HS256);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    /** Solo se aceptan tokens HS256, como los que emite security-service. */
    @Test
    @DisplayName("Un token firmado con otro algoritmo (HS512) se rechaza")
    void rechazaOtroAlgoritmo() {
        String secretoLargo = SECRETO + SECRETO;
        JwtValidador validadorLargo = new JwtValidador(secretoLargo);
        String token = firmar(secretoLargo, "usuario-4", List.of("ADMIN"), SignatureAlgorithm.HS512);

        assertThatThrownBy(() -> validadorLargo.validar(token)).isInstanceOf(JwtException.class);
    }

    /** Texto que no es un JWT. */
    @Test
    @DisplayName("Un texto que no es un JWT se rechaza")
    void rechazaTextoMalFormado() {
        assertThatThrownBy(() -> validador.validar("esto-no-es-un-token"))
                .isInstanceOf(JwtException.class);
    }

    private static String firmar(String secreto, String sub, List<String> roles, SignatureAlgorithm algoritmo) {
        return Jwts.builder()
                .setSubject(sub)
                .claim("roles", roles)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(clave(secreto), algoritmo)
                .compact();
    }

    private static SecretKey clave(String secreto) {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}
