package org.springsource.restbucks.web;

import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;
import org.springframework.stereotype.Component;
import org.springsource.restbucks.domain.Order;

/**
 * Event listener to reject {@code DELETE} requests to Spring Data REST.
 */
@Component
class OrderControllerEventListener extends AbstractRepositoryEventListener<Order> {

    @Override
    protected void onBeforeDelete(Order order) {

        if (order.isPaid()) {
            throw new OrderAlreadyPaidException();
        }
    }
}
