package ar.edu.unju.fi.logitrack.apigateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Genera JWT para los tests, firmados igual que los firma security-service (HS256, claims {@code sub} y
 * {@code roles}).
 */
final class TokensDePrueba {

    /** El mismo secreto que {@code jwt.secret} en el {@code application.yml} de tests. */
    static final String SECRETO = "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";

    private static final String OTRO_SECRETO = "OtraClaveDistintaQueNoEsLaDelGatewayParaProbarFirmas2026!!";

    private static final long UNA_HORA_MS = 3_600_000L;

    private TokensDePrueba() {
    }

    /**
     * Token válido con un rol.
     *
     * @param rol por ejemplo {@code ADMIN}, {@code CLIENTE} o {@code REPARTIDOR}
     * @return el JWT firmado, válido por una hora
     */
    static String conRol(String rol) {
        return firmar(SECRETO, List.of(rol), new Date(System.currentTimeMillis() + UNA_HORA_MS));
    }

    /**
     * Header {@code Authorization} listo para usar con un token válido.
     *
     * @param rol rol del usuario
     * @return {@code Bearer <token>}
     */
    static String bearer(String rol) {
        return "Bearer " + conRol(rol);
    }

    /**
     * Token válido pero sin ningún rol.
     *
     * @return el JWT firmado, sin roles
     */
    static String sinRoles() {
        return firmar(SECRETO, List.of(), new Date(System.currentTimeMillis() + UNA_HORA_MS));
    }

    /**
     * Token con la firma bien hecha pero vencido hace una hora.
     *
     * @return el JWT vencido
     */
    static String vencido() {
        return firmar(SECRETO, List.of("ADMIN"), new Date(System.currentTimeMillis() - UNA_HORA_MS));
    }

    /**
     * Token firmado con otro secreto: para el gateway es una firma alterada.
     *
     * @return el JWT con una firma que el gateway no acepta
     */
    static String conOtraFirma() {
        return firmar(OTRO_SECRETO, List.of("ADMIN"), new Date(System.currentTimeMillis() + UNA_HORA_MS));
    }

    private static String firmar(String secreto, List<String> roles, Date vencimiento) {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("roles", roles)
                .setIssuedAt(new Date(vencimiento.getTime() - UNA_HORA_MS))
                .setExpiration(vencimiento)
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
