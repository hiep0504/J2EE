package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.cart.CartCheckoutItemRequest;
import com.example.Backend_J2EE.dto.cart.CartCheckoutRequest;
import com.example.Backend_J2EE.dto.cart.CartItemRequest;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.dto.cart.UpdateCartItemRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Cart;
import com.example.Backend_J2EE.entity.CartItem;
import com.example.Backend_J2EE.entity.Order;
import com.example.Backend_J2EE.entity.OrderDetail;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Size;
import com.example.Backend_J2EE.repository.CartItemRepository;
import com.example.Backend_J2EE.repository.CartRepository;
import com.example.Backend_J2EE.repository.OrderRepository;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ProductSizeRepository;
import com.example.Backend_J2EE.repository.SizeRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SizeRepository sizeRepository;
    @Mock private ProductSizeRepository productSizeRepository;
    @Mock private OrderRepository orderRepository;

    private CartService cartService;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, productRepository, sizeRepository, productSizeRepository, orderRepository);
        session = new MockHttpSession();
    }

    @Test
    void addToCartStoresItemInSessionAndMergesQuantity() {
        CartItemRequest request = new CartItemRequest(1, 2, 3);
        session.setAttribute(CartService.SESSION_CART, Map.of("1_2", new CartItemRequest(1, 2, 1)));

        cartService.addToCart(request, null, session);

        @SuppressWarnings("unchecked")
        Map<String, CartItemRequest> cart = (Map<String, CartItemRequest>) session.getAttribute(CartService.SESSION_CART);
        assertEquals(4, cart.get("1_2").getQuantity());
    }

    @Test
    void getCartMapsSessionCart() {
        session.setAttribute(CartService.SESSION_CART, Map.of("1_2", new CartItemRequest(1, 2, 2)));

        Product product = Product.builder().id(1).name("Shoe").image("img").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(size));

        CartResponse response = cartService.getCart(null, session);

        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("200"), response.getTotal());
        assertEquals("Shoe", response.getItems().get(0).getName());
    }

    @Test
    void addToDatabaseCartCreatesNewItem() {
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        Cart cart = Cart.builder().id(10).account(Account.builder().id(5).build()).build();

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(size));
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(5)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndSizeId(10, 1, 2)).thenReturn(Optional.empty());

        cartService.addToCart(new CartItemRequest(1, 2, 3), 5, session);

        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToDatabaseCartMergesExistingItemQuantity() {
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        Cart cart = Cart.builder().id(10).account(Account.builder().id(5).build()).build();
        CartItem existing = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(size));
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(5)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndSizeId(10, 1, 2)).thenReturn(Optional.of(existing));

        cartService.addToCart(new CartItemRequest(1, 2, 3), 5, session);

        assertEquals(5, existing.getQuantity());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addToDatabaseCartRejectsMissingProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addToCart(new CartItemRequest(1, 2, 3), 5, session));

        assertEquals(404, ex.getStatusCode().value());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateSessionCartItemChangesQuantity() {
        session.setAttribute(CartService.SESSION_CART, Map.of("1_2", new CartItemRequest(1, 2, 1)));

        cartService.updateCartItem(1, 2, new UpdateCartItemRequest(2, 5), null, session);

        @SuppressWarnings("unchecked")
        Map<String, CartItemRequest> cart = (Map<String, CartItemRequest>) session.getAttribute(CartService.SESSION_CART);
        assertEquals(5, cart.get("1_2").getQuantity());
    }

    @Test
    void updateDatabaseCartItemRejectsWhenCartMissing() {
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.updateCartItem(1, 2, new UpdateCartItemRequest(2, 4), 7, session));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateDatabaseCartItemRejectsWhenItemMissing() {
        Cart cart = Cart.builder().id(11).build();
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndSizeId(11, 1, 2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.updateCartItem(1, 2, new UpdateCartItemRequest(2, 4), 7, session));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void removeSessionCartItemDeletesKey() {
        Map<String, CartItemRequest> mutableCart = new HashMap<>();
        mutableCart.put("1_2", new CartItemRequest(1, 2, 2));
        session.setAttribute(CartService.SESSION_CART, mutableCart);

        cartService.removeCartItem(1, 2, null, session);

        @SuppressWarnings("unchecked")
        Map<String, CartItemRequest> cart = (Map<String, CartItemRequest>) session.getAttribute(CartService.SESSION_CART);
        assertFalse(cart.containsKey("1_2"));
    }

    @Test
    void removeDatabaseCartItemRejectsWhenCartMissing() {
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.removeCartItem(1, 2, 7, session));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void removeDatabaseCartItemCallsDeleteByCompositeKey() {
        Cart cart = Cart.builder().id(11).build();
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));

        cartService.removeCartItem(1, 2, 7, session);

        verify(cartItemRepository).deleteByCartIdAndProductIdAndSizeId(11, 1, 2);
    }

    @Test
    void mergeSessionCartToDatabaseClearsEmptySessionCart() {
        session.setAttribute(CartService.SESSION_CART, new HashMap<String, CartItemRequest>());

        cartService.mergeSessionCartToDatabase(7, session);

        assertEquals(null, session.getAttribute(CartService.SESSION_CART));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void mergeSessionCartToDatabaseMergesAndClearsSession() {
        Map<String, CartItemRequest> sessionCart = new HashMap<>();
        sessionCart.put("1_2", new CartItemRequest(1, 2, 3));
        session.setAttribute(CartService.SESSION_CART, sessionCart);

        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        CartItem existing = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(size));
        when(cartItemRepository.findByCartIdAndProductIdAndSizeId(11, 1, 2)).thenReturn(Optional.of(existing));

        cartService.mergeSessionCartToDatabase(7, session);

        assertEquals(5, existing.getQuantity());
        verify(cartItemRepository).save(existing);
        assertEquals(null, session.getAttribute(CartService.SESSION_CART));
    }

    @Test
    void getOrCreateCartCreatesWhenMissing() {
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId(100);
            return cart;
        });

        Cart cart = cartService.getOrCreateCart(7);

        assertEquals(100, cart.getId());
        assertNotNull(cart.getAccount());
        assertEquals(7, cart.getAccount().getId());
    }

    @Test
    void clearCartDoesNothingWhenNoCart() {
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.empty());

        cartService.clearCart(7);

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void clearSessionCartRemovesSessionAttribute() {
        session.setAttribute(CartService.SESSION_CART, new HashMap<String, CartItemRequest>());

        cartService.clearSessionCart(session);

        assertEquals(null, session.getAttribute(CartService.SESSION_CART));
    }

    @Test
    void checkoutCreatesOrderAndClearsCartItems() {
        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        ProductSize productSize = ProductSize.builder().id(99).product(product).size(size).quantity(10).build();
        CartItem cartItem = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(11)).thenReturn(List.of(cartItem));
        when(productSizeRepository.findFirstByProduct_IdAndSize_IdOrderByIdAsc(1, 2)).thenReturn(Optional.of(productSize));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(50);
            return order;
        });

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", List.of(new CartCheckoutItemRequest(1, 2)));
        OrderDetailResponse response = cartService.checkout(7, request);

        assertEquals(50, response.getId());
        assertEquals(new BigDecimal("200"), response.getTotalPrice());
        assertEquals(1, response.getItems().size());
        assertEquals("Shoe", response.getItems().get(0).getProductName());
        assertEquals(8, productSize.getQuantity());
        verify(cartItemRepository).deleteAll(List.of(cartItem));
    }

    @Test
    void checkoutRejectsEmptyCart() {
        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.empty());

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(7, request));

        assertTrue(ex.getMessage().contains("Gio hang dang trong"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsWhenNotLoggedIn() {
        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(null, request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void checkoutRejectsInvalidSelectedItemsPayload() {
        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        CartItem cartItem = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(11)).thenReturn(List.of(cartItem));

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", List.of(new CartCheckoutItemRequest(null, 2)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(7, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void checkoutRejectsWhenNoSelectedItemMatchesCart() {
        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        CartItem cartItem = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(11)).thenReturn(List.of(cartItem));

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", List.of(new CartCheckoutItemRequest(9, 9)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(7, request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void checkoutRejectsWhenProductVariantNotFound() {
        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        CartItem cartItem = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(11)).thenReturn(List.of(cartItem));
        when(productSizeRepository.findFirstByProduct_IdAndSize_IdOrderByIdAsc(1, 2)).thenReturn(Optional.empty());

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(7, request));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void checkoutRejectsWhenQuantityExceedsStock() {
        Cart cart = Cart.builder().id(11).account(Account.builder().id(7).build()).build();
        Product product = Product.builder().id(1).name("Shoe").price(new BigDecimal("100")).build();
        Size size = Size.builder().id(2).sizeName("42").build();
        ProductSize productSize = ProductSize.builder().id(99).product(product).size(size).quantity(1).build();
        CartItem cartItem = CartItem.builder().cart(cart).product(product).size(size).quantity(2).price(new BigDecimal("100")).build();

        when(cartRepository.findFirstByAccountIdOrderByIdAsc(7)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(11)).thenReturn(List.of(cartItem));
        when(productSizeRepository.findFirstByProduct_IdAndSize_IdOrderByIdAsc(1, 2)).thenReturn(Optional.of(productSize));

        CartCheckoutRequest request = new CartCheckoutRequest("123 street", "0909", "cod", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.checkout(7, request));

        assertEquals(400, ex.getStatusCode().value());
        verify(orderRepository, never()).save(any(Order.class));
    }
}