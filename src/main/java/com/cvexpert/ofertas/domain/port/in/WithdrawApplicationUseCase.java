package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;

/** {@code DELETE /applications/{id}}: la persona retira su propia postulación. */
public interface WithdrawApplicationUseCase {

    void withdraw(Caller caller, long id);
}
