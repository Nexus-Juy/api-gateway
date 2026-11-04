package ar.edu.unju.fi.logitrack.apigateway.seguridad;

import java.util.Collection;
import java.util.List;

/**
 * Datos del usuario que el gateway saca del JWT (S3-06).
 * <p>
 * El filtro de JWT lo deja guardado en la petición para que el filtro de roles (S3-07) no tenga que volver
 * a leer el token.
 * </p>
 *
 * @param usuarioId id del usuario, que viene en el claim {@code sub}
 * @param roles     roles del usuario, que vienen en el claim {@code roles} (por ejemplo {@code ["CLIENTE"]})
 */
public record UsuarioAutenticado(String usuarioId, List<String> roles) {

    /**
     * Copia la lista de roles para que nadie la pueda modificar después.
     *
     * @param usuarioId id del usuario
     * @param roles     roles del usuario; si es {@code null} queda una lista vacía
     */
    public UsuarioAutenticado {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /**
     * Indica si el usuario tiene al menos uno de los roles pedidos.
     *
     * @param rolesPermitidos roles que alcanzan para la operación
     * @return {@code true} si alguno de sus roles está en la lista
     */
    public boolean tieneAlgunRol(Collection<String> rolesPermitidos) {
        return roles.stream().anyMatch(rolesPermitidos::contains);
    }
}
