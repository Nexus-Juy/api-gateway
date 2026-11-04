package ar.edu.unju.fi.logitrack.apigateway;

/**
 * Los tres servicios falsos que reemplazan a security, courier y shipment en los tests.
 * <p>
 * Se levantan una sola vez para toda la corrida de tests: así todas las clases de test comparten el mismo
 * contexto de Spring (y los mismos puertos), y la suite tarda menos.
 * </p>
 */
final class ServiciosFalsos {

    /** Reemplaza a security-service (rutas {@code /api/v1/auth/**}). */
    static final ServicioFalso SECURITY = ServicioFalso.iniciar("security-service");

    /** Reemplaza a courier-service (rutas {@code /api/v1/repartidores/**}). */
    static final ServicioFalso COURIER = ServicioFalso.iniciar("courier-service");

    /** Reemplaza a shipment-service (rutas {@code /api/v1/envios/**}). */
    static final ServicioFalso SHIPMENT = ServicioFalso.iniciar("shipment-service");

    private ServiciosFalsos() {
    }

    /** Olvida lo que recibieron los tres, antes de cada test. */
    static void limpiar() {
        SECURITY.limpiar();
        COURIER.limpiar();
        SHIPMENT.limpiar();
    }
}
