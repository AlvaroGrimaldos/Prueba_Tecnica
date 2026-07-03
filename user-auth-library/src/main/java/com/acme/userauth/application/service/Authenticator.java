package com.acme.userauth.application.service;

import java.util.Objects;

import com.acme.userauth.application.dto.AuthenticationRequest;
import com.acme.userauth.application.dto.AuthenticationResult;
import com.acme.userauth.domain.exception.AuthenticationException;

/**
 * Clase base abstracta para todos los mecanismos de autenticación de la
 * librería, implementada según el patrón <em>Template Method</em>.
 * <p>
 * Define el esqueleto invariable del proceso de autenticación
 * (validación → autenticación → notificación del desenlace) y delega en las
 * subclases únicamente los pasos que varían entre mecanismos. Ejemplos de
 * implementaciones concretas: autenticación por usuario/contraseña contra
 * base de datos, LDAP, token JWT, certificado X.509, etc.
 *
 * <h2>Diseño y principios SOLID</h2>
 * <ul>
 *   <li><b>SRP</b> — esta clase solo orquesta el flujo de autenticación; no
 *       conoce persistencia, hashing ni transporte. Esos detalles viven en
 *       las subclases o en los puertos que estas consuman.</li>
 *   <li><b>OCP</b> — se añaden nuevos mecanismos creando subclases, sin
 *       modificar esta clase ni a sus consumidores.</li>
 *   <li><b>LSP</b> — el método plantilla {@link #authenticate(AuthenticationRequest)}
 *       es {@code final} y garantiza el mismo contrato observable para
 *       cualquier implementación: nunca devuelve {@code null} y nunca propaga
 *       {@link AuthenticationException} al llamante.</li>
 *   <li><b>ISP</b> — el parámetro genérico {@code R} permite a cada
 *       mecanismo exigir exactamente las credenciales que necesita, en lugar
 *       de un objeto de petición "para todo".</li>
 *   <li><b>DIP</b> — no depende de ningún framework (Spring, Jakarta EE…),
 *       por lo que puede reutilizarse desde cualquier proyecto Java 21+. Las
 *       subclases reciben sus colaboradores (repositorios, codificadores de
 *       contraseña, relojes) como abstracciones por constructor.</li>
 * </ul>
 *
 * <h2>Ejemplo de implementación</h2>
 * <pre>{@code
 * public final class PasswordAuthenticator
 *         extends Authenticator<PasswordAuthenticationRequest> {
 *
 *     private final UserRepositoryPort users;
 *     private final PasswordEncoderPort encoder;
 *
 *     public PasswordAuthenticator(UserRepositoryPort users, PasswordEncoderPort encoder) {
 *         this.users = users;
 *         this.encoder = encoder;
 *     }
 *
 *     @Override
 *     public boolean supports(Class<? extends AuthenticationRequest> type) {
 *         return PasswordAuthenticationRequest.class.isAssignableFrom(type);
 *     }
 *
 *     @Override
 *     protected AuthenticationResult doAuthenticate(PasswordAuthenticationRequest request) {
 *         var user = users.findByUsername(request.principal())
 *                 .orElseThrow(() -> new AuthenticationException("unknown user"));
 *         if (!encoder.matches(request.rawPassword(), user.passwordHash())) {
 *             throw new AuthenticationException("invalid credentials");
 *         }
 *         return AuthenticationResult.success(user.username(), user.roleNames());
 *     }
 * }
 * }</pre>
 *
 * <h2>Hilo de ejecución</h2>
 * Las implementaciones deben ser <em>stateless</em> (sin estado mutable de
 * instancia) para que una única instancia pueda compartirse de forma segura
 * entre hilos, como es habitual en contenedores de inversión de control.
 *
 * @param <R> tipo concreto de solicitud que este mecanismo sabe procesar;
 *            acota las credenciales aceptadas en tiempo de compilación
 * @see AuthenticationRequest
 * @see AuthenticationResult
 */
public abstract class Authenticator<R extends AuthenticationRequest> {

    private static final System.Logger LOG = System.getLogger(Authenticator.class.getName());

    /** Motivo usado cuando la excepción de fallo no aporta mensaje. */
    private static final String DEFAULT_FAILURE_REASON = "authentication failed";

    /**
     * Método plantilla que ejecuta el flujo completo de autenticación.
     * <p>
     * Orden de ejecución:
     * <ol>
     *   <li>{@link #validate(AuthenticationRequest)} — validación sintáctica
     *       de la solicitud;</li>
     *   <li>{@link #doAuthenticate(AuthenticationRequest)} — verificación de
     *       credenciales propia del mecanismo;</li>
     *   <li>{@link #onSuccess(AuthenticationRequest, AuthenticationResult)} o
     *       {@link #onFailure(AuthenticationRequest, AuthenticationException)}
     *       — notificación del desenlace (auditoría, contadores de intentos,
     *       métricas…).</li>
     * </ol>
     * Cualquier {@link AuthenticationException} lanzada por los pasos
     * anteriores se captura y se traduce a un resultado fallido, de modo que
     * el llamante trabaja siempre con un {@link AuthenticationResult} y no
     * necesita manejar excepciones de negocio.
     * <p>
     * Es {@code final} a propósito: las subclases personalizan los pasos,
     * nunca el flujo, preservando el contrato ante los consumidores
     * (principio de sustitución de Liskov).
     *
     * @param request solicitud de autenticación a procesar
     * @return resultado del intento, satisfactorio o fallido; nunca {@code null}
     * @throws NullPointerException si {@code request} es {@code null}
     */
    public final AuthenticationResult authenticate(R request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            validate(request);
            AuthenticationResult result = Objects.requireNonNull(
                    doAuthenticate(request),
                    "doAuthenticate must not return null");
            if (result.authenticated()) {
                notifySuccess(request, result);
            } else {
                notifyFailure(request, null);
            }
            return result;
        } catch (AuthenticationException ex) {
            notifyFailure(request, ex);
            return AuthenticationResult.failure(request.principal(), failureReason(ex));
        }
    }

    /**
     * Invoca {@link #onSuccess} conteniendo cualquier excepción del gancho:
     * un fallo en auditoría/métricas no debe alterar el desenlace de una
     * autenticación ya resuelta.
     */
    private void notifySuccess(R request, AuthenticationResult result) {
        try {
            onSuccess(request, result);
        } catch (RuntimeException ex) {
            LOG.log(System.Logger.Level.WARNING, "onSuccess hook threw an exception; ignored", ex);
        }
    }

    /** Análogo a {@link #notifySuccess} para el gancho de fallo. */
    private void notifyFailure(R request, AuthenticationException failure) {
        try {
            onFailure(request, failure);
        } catch (RuntimeException ex) {
            LOG.log(System.Logger.Level.WARNING, "onFailure hook threw an exception; ignored", ex);
        }
    }

    /** Garantiza un motivo de fallo no vacío para la invariante del resultado. */
    private static String failureReason(AuthenticationException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? DEFAULT_FAILURE_REASON
                : ex.getMessage();
    }

    /**
     * Indica si este mecanismo sabe procesar solicitudes del tipo dado.
     * <p>
     * Permite componer varios autenticadores tras una única fachada (patrón
     * <em>Chain of Responsibility</em> o un registro/composite) que despache
     * cada solicitud al mecanismo adecuado, sin que los consumidores conozcan
     * las implementaciones concretas.
     *
     * @param requestType tipo de la solicitud a evaluar; nunca {@code null}
     * @return {@code true} si {@link #authenticate(AuthenticationRequest)}
     *         puede procesar solicitudes de ese tipo
     */
    public abstract boolean supports(Class<? extends AuthenticationRequest> requestType);

    /**
     * Paso de validación sintáctica de la solicitud, previo a cualquier
     * acceso a sistemas externos (base de datos, LDAP, IdP…).
     * <p>
     * La implementación por defecto no realiza ninguna comprobación. Las
     * subclases pueden sobrescribirla para verificar formato de credenciales,
     * longitudes, campos obligatorios, etc. Aquí no deben aplicarse reglas
     * de negocio (bloqueos, expiraciones): eso pertenece a
     * {@link #doAuthenticate(AuthenticationRequest)}.
     *
     * @param request solicitud a validar; nunca {@code null}
     * @throws AuthenticationException si la solicitud es sintácticamente
     *                                 inválida
     */
    protected void validate(R request) {
        // sin validación por defecto
    }

    /**
     * Verificación de credenciales específica del mecanismo. Único paso
     * obligatorio para las subclases.
     *
     * @param request solicitud ya validada; nunca {@code null}
     * @return resultado del intento; no debe ser {@code null}
     * @throws AuthenticationException si las credenciales son inválidas o el
     *                                 principal no puede autenticarse
     *                                 (bloqueado, expirado, etc.)
     */
    protected abstract AuthenticationResult doAuthenticate(R request);

    /**
     * Gancho invocado tras una autenticación satisfactoria.
     * <p>
     * La implementación por defecto no hace nada. Punto de extensión típico
     * para auditoría, reinicio de contadores de intentos fallidos o emisión
     * de eventos. No debe lanzar excepciones que alteren el resultado.
     *
     * @param request solicitud procesada
     * @param result  resultado satisfactorio devuelto al llamante
     */
    protected void onSuccess(R request, AuthenticationResult result) {
        // sin acción por defecto
    }

    /**
     * Gancho invocado cuando la autenticación falla.
     * <p>
     * La implementación por defecto no hace nada. Punto de extensión típico
     * para auditoría de intentos fallidos, incremento de contadores de
     * bloqueo o alertas de seguridad.
     *
     * @param request solicitud procesada
     * @param failure excepción de negocio que provocó el fallo, o
     *                {@code null} si {@link #doAuthenticate(AuthenticationRequest)}
     *                devolvió un resultado fallido sin lanzar excepción
     */
    protected void onFailure(R request, AuthenticationException failure) {
        // sin acción por defecto
    }
}
