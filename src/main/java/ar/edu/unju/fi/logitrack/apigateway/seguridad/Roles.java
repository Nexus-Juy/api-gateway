package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import java.util.Set;

/**
 * Roles de LogiTrack, tal como vienen en el claim {@code roles} del JWT (sin el prefijo {@code ROLE_}).
 */
public final class Roles {

    /** Administrador: acceso total. */
    public static final String ADMIN = "ADMIN";

    /** Cliente que registra y sigue sus envíos. */
    public static final String CLIENTE = "CLIENTE";

    /** Repartidor que lleva los envíos asignados. */
    public static final String REPARTIDOR = "REPARTIDOR";

    /** Los tres roles juntos, para las rutas que acepta cualquier usuario del sistema. */
    public static final Set<String> TODOS = Set.of(ADMIN, CLIENTE, REPARTIDOR);

    private Roles() {
    }
}
