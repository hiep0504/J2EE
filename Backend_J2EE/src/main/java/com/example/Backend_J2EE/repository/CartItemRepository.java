package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    Optional<CartItem> findByCartIdAndProductIdAndSizeId(Integer cartId, Integer productId, Integer sizeId);
    void deleteByCartIdAndProductIdAndSizeId(Integer cartId, Integer productId, Integer sizeId);
}
