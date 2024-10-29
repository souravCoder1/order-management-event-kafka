package org.consumer.repo;

import org.consumer.entity.Order;
import org.consumer.entity.OrderEvent;
import org.springframework.data.repository.CrudRepository;

public interface OrderEventsRepository extends CrudRepository<OrderEvent, Integer> {
}
