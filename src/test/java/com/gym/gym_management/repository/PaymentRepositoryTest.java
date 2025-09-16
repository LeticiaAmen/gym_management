package com.gym.gym_management.repository;

import com.gym.gym_management.model.Client;
import com.gym.gym_management.model.Payment;
import com.gym.gym_management.model.PaymentMethod;
import com.gym.gym_management.model.PaymentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de capa de repositorio para validar consultas personalizadas y mapeos sensibles
 * de la entidad Payment:
 * - Filtrado por voided = false
 * - Ordenamientos (fecha de pago DESC / fecha de expiración ASC)
 * - Ventana de expiración (próximos 3 días)
 * - Persistencia de @Enumerated(EnumType.STRING) y columnas period_month / period_year
 */
@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private IPaymentRepository paymentRepository;

    @Autowired
    private IClientRepository clientRepository;

    private Client nuevoCliente(String nombre, String apellido, String email) {
        Client c = new Client();
        c.setFirstName(nombre);
        c.setLastName(apellido);
        c.setEmail(email);
        c.setPhone("123");
        return clientRepository.save(c);
    }

    private Payment crearPago(Client client, LocalDate paymentDate, LocalDate expirationDate, boolean voided, PaymentState state, int month, int year) {
        Payment p = new Payment();
        p.setClient(client);
        p.setAmount(5000.0);
        p.setMethod(PaymentMethod.CASH);
        p.setMonth(month);
        p.setYear(year);
        p.setPaymentDate(paymentDate);
        p.setExpirationDate(expirationDate);
        p.setVoided(voided);
        p.setState(state);
        return paymentRepository.save(p);
    }

    @Test
    @DisplayName("encuentraPagosRecientes_ignoraVoided_yOrdenaDesc")
    void encuentraPagosRecientes_ignoraVoided_yOrdenaDesc() {
        // GIVEN
        Client c1 = nuevoCliente("Juan", "Perez", "juan+p1@test.com");
        LocalDate hoy = LocalDate.now();
        // Pagos (uno anulado) - todos posteriores al umbral (hoy - 7)
        Payment p1 = crearPago(c1, hoy.minusDays(1), hoy.plusMonths(1), false, PaymentState.UP_TO_DATE, hoy.getMonthValue(), hoy.getYear());
        Payment p2 = crearPago(c1, hoy.minusDays(2), hoy.plusMonths(1), false, PaymentState.UP_TO_DATE, hoy.getMonthValue(), hoy.getYear());
        Payment p3Voided = crearPago(c1, hoy.minusDays(3), hoy.plusMonths(1), true, PaymentState.VOIDED, hoy.getMonthValue(), hoy.getYear());
        Payment p4 = crearPago(c1, hoy.minusDays(5), hoy.plusMonths(1), false, PaymentState.UP_TO_DATE, hoy.getMonthValue(), hoy.getYear());

        LocalDate umbral = hoy.minusDays(7);

        // WHEN
        List<Payment> result = paymentRepository.findByPaymentDateAfterAndVoidedFalseOrderByPaymentDateDesc(umbral);

        // THEN
        assertThat(result)
                .as("Debe retornar sólo pagos no anulados posteriores al umbral")
                .extracting(Payment::getId)
                .containsExactly(p1.getId(), p2.getId(), p4.getId()); // orden DESC por paymentDate

        // Verifica que el voided fue excluido
        assertThat(result).noneMatch(p -> p.getId().equals(p3Voided.getId()));

        // Verifica mapping enumerados y columnas de período
        Payment primero = result.get(0);
        assertThat(primero.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(primero.getState()).isEqualTo(PaymentState.UP_TO_DATE);
        assertThat(primero.getMonth()).isEqualTo(hoy.getMonthValue());
        assertThat(primero.getYear()).isEqualTo(hoy.getYear());
    }

    @Test
    @DisplayName("encuentraPagosPorExpirar_enVentanaDeTresDias")
    void encuentraPagosPorExpirar_enVentanaDeTresDias() {
        // GIVEN
        Client c1 = nuevoCliente("Ana", "Gomez", "ana+p2@test.com");
        LocalDate hoy = LocalDate.now();
        LocalDate start = hoy.plusDays(1);
        LocalDate end = hoy.plusDays(3);

        // Dentro de la ventana
        Payment e1 = crearPago(c1, hoy.minusDays(2), start, false, PaymentState.UP_TO_DATE, start.getMonthValue(), start.getYear());
        Payment e2 = crearPago(c1, hoy.minusDays(2), hoy.plusDays(2), false, PaymentState.UP_TO_DATE, start.getMonthValue(), start.getYear());
        Payment e3 = crearPago(c1, hoy.minusDays(2), end, false, PaymentState.UP_TO_DATE, start.getMonthValue(), start.getYear());
        // Fuera (después)
        Payment fuera = crearPago(c1, hoy.minusDays(2), hoy.plusDays(4), false, PaymentState.UP_TO_DATE, start.getMonthValue(), start.getYear());
        // Ignorado por voided
        Payment voided = crearPago(c1, hoy.minusDays(2), hoy.plusDays(2), true, PaymentState.VOIDED, start.getMonthValue(), start.getYear());

        // WHEN
        List<Payment> result = paymentRepository.findByExpirationDateBetweenAndVoidedFalseOrderByExpirationDateAsc(start, end);

        // THEN
        assertThat(result)
                .as("Debe traer sólo los pagos no anulados cuya expirationDate esté dentro de [start, end]")
                .extracting(Payment::getId)
                .containsExactly(e1.getId(), e2.getId(), e3.getId()); // orden ASC expiración

        assertThat(result).noneMatch(p -> p.getId().equals(voided.getId()));
        assertThat(result).noneMatch(p -> p.getId().equals(fuera.getId()));

        // Validar consistencia de mapeo de período y enumerados en uno cualquiera
        Payment cualquiera = result.get(0);
        assertThat(cualquiera.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(cualquiera.getState()).isEqualTo(PaymentState.UP_TO_DATE);
        assertThat(cualquiera.getMonth()).isNotNull();
        assertThat(cualquiera.getYear()).isNotNull();
    }
}

