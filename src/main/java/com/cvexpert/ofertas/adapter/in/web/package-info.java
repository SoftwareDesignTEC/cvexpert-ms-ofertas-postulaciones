/**
 * Adaptador de entrada HTTP: controladores REST que APIM llama.
 *
 * <p>La identidad llega en los encabezados {@code X-User-Id} y {@code X-User-Email} (y
 * {@code X-User-Role} en las rutas de administración); el servicio no valida tokens de Google.
 */
package com.cvexpert.ofertas.adapter.in.web;
