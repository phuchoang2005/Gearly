package com.dominator.gearly.repository;

import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Transaction;
import com.dominator.gearly.model.TransactionStatus;
import com.dominator.gearly.repository.custom.OrderRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
}