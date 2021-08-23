package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.beer.inventory.service.services.AllocationService;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationRequestListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(@Payload AllocateOrderRequest request) {
        AllocationResult.AllocationResultBuilder builder = AllocationResult.builder();
        builder.beerOrderDto(request.getBeerOrderDto());

        try {
            Boolean allocationResult = allocationService.allocateOrder(request.getBeerOrderDto());

            if (allocationResult) {
                builder.pendingInventory(false);
            } else {
                builder.pendingInventory(true);
            }

            builder.allocationError(false);
        } catch (Exception e) {
            log.error("Allocation failed for Order Id: " + request.getBeerOrderDto().getId());
            builder.allocationError(true);
        }

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE, builder.build());
    }
}
