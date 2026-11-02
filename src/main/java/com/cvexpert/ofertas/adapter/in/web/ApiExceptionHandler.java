package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.exception.DomainException;
import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce los errores a códigos HTTP con el esquema {@code Error} del contrato. Es el único lugar
 * donde el dominio se encuentra con HTTP: las excepciones del dominio no saben de códigos.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException e) {
        return ResponseEntity.status(statusOf(e)).body(new ApiError(e.code(), e.getMessage()));
    }

    /** JSON mal formado, un número que no es número o un parámetro obligatorio que falta. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "La solicitud no es válida"));
    }

    /** Cualquier otra cosa es un error nuestro: se registra completo y al cliente no se le dan detalles. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.internalServerError().body(new ApiError("INTERNAL_ERROR", "Ocurrió un error inesperado"));
    }

    private static HttpStatus statusOf(DomainException e) {
        if (e instanceof InvalidRequestException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (e instanceof ForbiddenException) {
            return HttpStatus.FORBIDDEN;
        }
        if (e instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof AlreadyAppliedException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
