package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findFirstByAccountIdOrderByIdAsc(Integer accountId);
}
