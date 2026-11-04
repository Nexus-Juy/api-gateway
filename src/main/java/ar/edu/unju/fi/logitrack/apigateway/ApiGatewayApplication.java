package ar.edu.unju.fi.logitrack.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Puerta de entrada única de LogiTrack (S3-05).
 * <p>
 * Todas las peticiones de afuera (Postman, Apidog, un frontend) llegan acá y el gateway las reenvía al
 * microservicio que corresponde, buscándolo por su nombre en Eureka ({@code lb://SHIPMENT-SERVICE},
 * {@code lb://COURIER-SERVICE} y {@code lb://SECURITY-SERVICE}). El puerto y las rutas no están en el
 * código: los entrega el config-server desde {@code api-gateway.yml}.
 * </p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Arranca el gateway.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
