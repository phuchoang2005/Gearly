package com.dominator.bookify.repository;

import com.dominator.bookify.model.Order;
import com.dominator.bookify.model.OrderStatus;
import com.dominator.bookify.model.Transaction;
import com.dominator.bookify.model.TransactionStatus;
import com.dominator.bookify.repository.custom.OrderRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
}