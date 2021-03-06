package org.springsource.restbucks.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springsource.restbucks.domain.*;
import org.springsource.restbucks.domain.Payment.Receipt;
import org.springsource.restbucks.event.OrderPaidEvent;
import org.springsource.restbucks.repository.CreditCardRepository;
import org.springsource.restbucks.repository.OrderRepository;
import org.springsource.restbucks.repository.PaymentRepository;

import java.util.Optional;

/**
 * Implementation of {@link PaymentService} delegating persistence operations to {@link PaymentRepository} and
 * {@link CreditCardRepository}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final @NonNull
    CreditCardRepository creditCardRepository;
    private final @NonNull
    PaymentRepository paymentRepository;
    private final @NonNull
    OrderRepository orderRepository;
    private final @NonNull
    ApplicationEventPublisher publisher;

    @Override
    public CreditCardPayment pay(Order order, CreditCardNumber creditCardNumber) {

        if (order.isPaid()) {
            throw new PaymentException(order, "Order already paid!");
        }

        // Using Optional.orElseThrow(…) doesn't work due to https://bugs.openjdk.java.net/browse/JDK-8054569
        Optional<CreditCard> creditCardResult = creditCardRepository.findByNumber(creditCardNumber);

        if (!creditCardResult.isPresent()) {
            throw new PaymentException(order,
                    String.format("No credit card found for number: %s", creditCardNumber.getNumber()));
        }

        CreditCard creditCard = creditCardResult.get();

        if (!creditCard.isValid()) {
            throw new PaymentException(order, String.format("Invalid credit card with number %s, expired %s!",
                    creditCardNumber.getNumber(), creditCard.getExpirationDate()));
        }

        order.markPaid();
        CreditCardPayment payment = paymentRepository.save(new CreditCardPayment(creditCard, order));

        publisher.publishEvent(new OrderPaidEvent(order.getId()));

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentFor(Order order) {
        return paymentRepository.findByOrder(order);
    }

    @Override
    public Optional<Receipt> takeReceiptFor(Order order) {

        order.markTaken();
        orderRepository.save(order);

        return getPaymentFor(order).map(Payment::getReceipt);
    }
}
