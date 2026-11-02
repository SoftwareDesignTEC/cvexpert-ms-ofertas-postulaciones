package com.cvexpert.ofertas.adapter.in.web;

/** El esquema {@code Error} del contrato: un código estable para los clientes y un mensaje. */
public record ApiError(String code, String message) {
}
