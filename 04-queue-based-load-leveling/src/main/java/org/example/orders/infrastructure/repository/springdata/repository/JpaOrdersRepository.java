package org.example.orders.infrastructure.repository.springdata.repository;

import java.util.UUID;
import org.example.orders.infrastructure.repository.springdata.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrdersRepository extends JpaRepository<OrderEntity, UUID> {}
