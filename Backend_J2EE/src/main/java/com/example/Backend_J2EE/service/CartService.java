package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.cart.CartItemRequest;
import com.example.Backend_J2EE.dto.cart.CartItemResponse;
import com.example.Backend_J2EE.dto.cart.CartCheckoutItemRequest;
import com.example.Backend_J2EE.dto.cart.CartCheckoutRequest;
import com.example.Backend_J2EE.dto.cart.CartResponse;
import com.example.Backend_J2EE.dto.cart.UpdateCartItemRequest;
import com.example.Backend_J2EE.dto.order.OrderDetailResponse;
import com.example.Backend_J2EE.dto.order.OrderItemResponse;
import com.example.Backend_J2EE.entity.*;
import com.example.Backend_J2EE.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CartService {

    public static final String SESSION_CART = "CART";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final ProductSizeRepository productSizeRepository;
    private final OrderRepository orderRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       SizeRepository sizeRepository,
                       ProductSizeRepository productSizeRepository,
                       OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.sizeRepository = sizeRepository;
        this.productSizeRepository = productSizeRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Xóa @OneToOne relationship từ Account entity (optional)
     * hoặc giữ nó nếu cần
     */

    // ============ Add to Cart ============

    /**
     * Thêm sản phẩm vào giỏ hàng
     * - Nếu chưa đăng nhập: lưu vào session
     * - Nếu đã đăng nhập: lưu vào database
     */
    @Transactional
    public void addToCart(CartItemRequest request, Integer accountId, HttpSession session) {
        if (accountId == null) {
            // Chưa đăng nhập - lưu vào session
            addToSessionCart(request, session);
        } else {
            // Đã đăng nhập - lưu vào database
            addToDatabaseCart(request, accountId);
        }
    }

    private void addToSessionCart(CartItemRequest request, HttpSession session) {
        Map<String, CartItemRequest> sessionCart = getSessionCart(session);

        String key = request.getProductId() + "_" + request.getSizeId();

        if (sessionCart.containsKey(key)) {
            // Sản phẩm đã tồn tại -> tăng quantity
            CartItemRequest existing = sessionCart.get(key);
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            // Sản phẩm mới -> thêm vào
            sessionCart.put(key, request);
        }

        session.setAttribute(SESSION_CART, sessionCart);
    }

    @Transactional
    private void addToDatabaseCart(CartItemRequest request, Integer accountId) {
        // Validate - Kiểm tra product và size tồn tại
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Size size = sizeRepository.findById(request.getSizeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size not found"));

        // Lấy hoặc tạo cart
        Cart cart = getOrCreateCart(accountId);

        // Kiểm tra xem item này có tồn tại trong cart không
        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductIdAndSizeId(cart.getId(), request.getProductId(), request.getSizeId());

        if (existingItem.isPresent()) {
            // Sản phẩm đã tồn tại -> tăng quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // Sản phẩm mới -> thêm vào
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .size(size)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }
    }

    // ============ Get Cart ============

    /**
     * Lấy giỏ hàng của user
     * - Nếu chưa đăng nhập: từ session
     * - Nếu đã đăng nhập: từ database
     */
    public CartResponse getCart(Integer accountId, HttpSession session) {
        if (accountId == null) {
            // Chưa đăng nhập - lấy từ session
            return getSessionCartResponse(session);
        } else {
            // Đã đăng nhập - lấy từ database
            return getDatabaseCartResponse(accountId);
        }
    }

    private CartResponse getSessionCartResponse(HttpSession session) {
        Map<String, CartItemRequest> sessionCart = getSessionCart(session);

        if (sessionCart.isEmpty()) {
            return new CartResponse(new ArrayList<>(), BigDecimal.ZERO);
        }

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemRequest itemReq : sessionCart.values()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElse(null);

            if (product == null) continue;

            Size size = sizeRepository.findById(itemReq.getSizeId())
                    .orElse(null);

            if (size == null) continue;

            CartItemResponse itemRes = new CartItemResponse();
            itemRes.setProductId(product.getId());
            itemRes.setSizeId(size.getId());
            itemRes.setSizeName(size.getSizeName());
            itemRes.setName(product.getName());
            itemRes.setImage(product.getImage());
            itemRes.setPrice(product.getPrice());
            itemRes.setQuantity(itemReq.getQuantity());

            BigDecimal lineTotal = product.getPrice().multiply(new BigDecimal(itemReq.getQuantity()));
            itemRes.setLineTotal(lineTotal);

            items.add(itemRes);
            total = total.add(lineTotal);
        }

        return new CartResponse(items, total);
    }

    private CartResponse getDatabaseCartResponse(Integer accountId) {
        Optional<Cart> cartOptional = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId);

        if (cartOptional.isEmpty()) {
            return new CartResponse(new ArrayList<>(), BigDecimal.ZERO);
        }

        Cart cart = cartOptional.get();
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            CartItemResponse itemRes = new CartItemResponse();
            itemRes.setProductId(cartItem.getProduct().getId());
            itemRes.setSizeId(cartItem.getSize().getId());
            itemRes.setSizeName(cartItem.getSize().getSizeName());
            itemRes.setName(cartItem.getProduct().getName());
            itemRes.setImage(cartItem.getProduct().getImage());
            itemRes.setPrice(cartItem.getPrice());
            itemRes.setQuantity(cartItem.getQuantity());

            BigDecimal lineTotal = cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            itemRes.setLineTotal(lineTotal);

            items.add(itemRes);
            total = total.add(lineTotal);
        }

        return new CartResponse(items, total);
    }

    // ============ Update Cart Item ============

    @Transactional
    public void updateCartItem(Integer productId, Integer sizeId, UpdateCartItemRequest request,
                              Integer accountId, HttpSession session) {
        if (accountId == null) {
            // Chưa đăng nhập - cập nhật session
            updateSessionCartItem(productId, sizeId, request, session);
        } else {
            // Đã đăng nhập - cập nhật database
            updateDatabaseCartItem(productId, sizeId, request, accountId);
        }
    }

    private void updateSessionCartItem(Integer productId, Integer sizeId, UpdateCartItemRequest request, HttpSession session) {
        Map<String, CartItemRequest> sessionCart = getSessionCart(session);

        String key = productId + "_" + sizeId;
        if (sessionCart.containsKey(key)) {
            sessionCart.get(key).setQuantity(request.getQuantity());
            session.setAttribute(SESSION_CART, sessionCart);
        }
    }

    @Transactional
    private void updateDatabaseCartItem(Integer productId, Integer sizeId, UpdateCartItemRequest request, Integer accountId) {
        Cart cart = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem item = cartItemRepository.findByCartIdAndProductIdAndSizeId(cart.getId(), productId, sizeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
    }

    // ============ Remove Cart Item ============

    @Transactional
    public void removeCartItem(Integer productId, Integer sizeId, Integer accountId, HttpSession session) {
        if (accountId == null) {
            // Chưa đăng nhập - xóa từ session
            removeSessionCartItem(productId, sizeId, session);
        } else {
            // Đã đăng nhập - xóa từ database
            removeDatabaseCartItem(productId, sizeId, accountId);
        }
    }

    private void removeSessionCartItem(Integer productId, Integer sizeId, HttpSession session) {
        Map<String, CartItemRequest> sessionCart = getSessionCart(session);

        String key = productId + "_" + sizeId;
        sessionCart.remove(key);

        session.setAttribute(SESSION_CART, sessionCart);
    }

    @Transactional
    private void removeDatabaseCartItem(Integer productId, Integer sizeId, Integer accountId) {
        Cart cart = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        cartItemRepository.deleteByCartIdAndProductIdAndSizeId(cart.getId(), productId, sizeId);
    }

    // ============ Merge Session Cart to Database (on Login) ============

    /**
     * Thực hiện merge giỏ hàng từ session vào database
     * - Được gọi khi user đăng nhập
     * - Lấy hoặc tạo cart theo account_id
     * - Duyệt từng sản phẩm trong session:
     *   + Nếu product_size_id đã tồn tại trong DB → tăng quantity
     *   + Nếu chưa có → thêm mới vào cart_items
     * - Xóa giỏ hàng trong session sau khi merge
     */
    @Transactional
    public void mergeSessionCartToDatabase(Integer accountId, HttpSession session) {
        Map<String, CartItemRequest> sessionCart = getSessionCart(session);

        if (sessionCart.isEmpty()) {
            // Session cart rỗng - không cần merge
            session.removeAttribute(SESSION_CART);
            return;
        }

        // Lấy hoặc tạo cart trong database
        Cart cart = getOrCreateCart(accountId);

        // Duyệt từng sản phẩm trong session
        for (CartItemRequest sessionItem : sessionCart.values()) {
            Product product = productRepository.findById(sessionItem.getProductId())
                    .orElse(null);

            if (product == null) continue;

            Size size = sizeRepository.findById(sessionItem.getSizeId())
                    .orElse(null);

            if (size == null) continue;

            // Kiểm tra xem item này có tồn tại trong DB không
            Optional<CartItem> existingItem = cartItemRepository
                    .findByCartIdAndProductIdAndSizeId(cart.getId(), sessionItem.getProductId(), sessionItem.getSizeId());

            if (existingItem.isPresent()) {
                // Sản phẩm đã tồn tại -> tăng quantity
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + sessionItem.getQuantity());
                cartItemRepository.save(item);
            } else {
                // Sản phẩm chưa có -> thêm mới
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .size(size)
                        .quantity(sessionItem.getQuantity())
                        .price(product.getPrice())
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        // Xóa giỏ hàng trong session
        session.removeAttribute(SESSION_CART);
    }

    // ============ Helper Methods ============

    /**
     * Lấy hoặc tạo cart cho user
     */
    @Transactional
    public Cart getOrCreateCart(Integer accountId) {
        Optional<Cart> existing = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Tạo cart mới
        Account account = new Account();
        account.setId(accountId);

        Cart newCart = Cart.builder()
                .account(account)
                .build();

        return cartRepository.save(newCart);
    }

    /**
     * Lấy giỏ hàng từ session
     * Nếu chưa có thì trả về map rỗng
     */
    @SuppressWarnings("unchecked")
    private Map<String, CartItemRequest> getSessionCart(HttpSession session) {
        Object cartObj = session.getAttribute(SESSION_CART);

        if (cartObj == null) {
            return new HashMap<>();
        }

        return (Map<String, CartItemRequest>) cartObj;
    }

    /**
     * Xóa giỏ hàng khi user đã thanh toán hoặc checkout
     */
    @Transactional
    public void clearCart(Integer accountId) {
        Optional<Cart> cartOptional = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId);

        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    /**
     * Xóa giỏ hàng trong session
     */
    public void clearSessionCart(HttpSession session) {
        session.removeAttribute(SESSION_CART);
    }

    @Transactional
    public OrderDetailResponse checkout(Integer accountId, CartCheckoutRequest request) {
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap de thanh toan");
        }
        validateCheckoutRequest(request);

        Cart cart = cartRepository.findFirstByAccountIdOrderByIdAsc(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gio hang dang trong"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gio hang dang trong");
        }

        Set<String> selectedKeys = new HashSet<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CartCheckoutItemRequest item : request.getItems()) {
                if (item == null || item.getProductId() == null || item.getSizeId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sach san pham thanh toan khong hop le");
                }
                selectedKeys.add(item.getProductId() + "_" + item.getSizeId());
            }
        }

        List<CartItem> itemsToCheckout = cartItems.stream()
                .filter(item -> selectedKeys.isEmpty() || selectedKeys.contains(item.getProduct().getId() + "_" + item.getSize().getId()))
                .toList();

        if (itemsToCheckout.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong co san pham nao duoc chon de thanh toan");
        }

        Account account = new Account();
        account.setId(accountId);

        Order order = Order.builder()
                .account(account)
                .status(Order.OrderStatus.pending)
                .address(request.getAddress().trim())
                .phone(request.getPhone().trim())
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();

        for (CartItem cartItem : itemsToCheckout) {
                ProductSize productSize = productSizeRepository
                    .findFirstByProduct_IdAndSize_IdOrderByIdAsc(cartItem.getProduct().getId(), cartItem.getSize().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay bien the san pham"));

            int stock = productSize.getQuantity() == null ? 0 : productSize.getQuantity();
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            if (quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So luong san pham trong gio khong hop le");
            }
            if (quantity > stock) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "So luong vuot ton kho cho san pham " + cartItem.getProduct().getName() + " - size " + cartItem.getSize().getSizeName()
                );
            }

            BigDecimal unitPrice = cartItem.getPrice() != null ? cartItem.getPrice() :
                    (cartItem.getProduct().getPrice() != null ? cartItem.getProduct().getPrice() : BigDecimal.ZERO);

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .productSize(productSize)
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();
            details.add(detail);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            productSize.setQuantity(stock - quantity);
        }

        order.setOrderDetails(details);
        order.setTotalPrice(total);
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(itemsToCheckout);

        return toOrderDetailResponse(savedOrder);
    }

    private void validateCheckoutRequest(CartCheckoutRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du lieu thanh toan la bat buoc");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia chi giao hang la bat buoc");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So dien thoai la bat buoc");
        }
    }

    private OrderDetailResponse toOrderDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setAccountId(order.getAccount() != null ? order.getAccount().getId() : null);
        response.setOrderDate(order.getOrderDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setAddress(order.getAddress());
        response.setPhone(order.getPhone());

        List<OrderItemResponse> items = order.getOrderDetails() == null
                ? List.of()
                : order.getOrderDetails().stream().map(detail -> {
                    ProductSize ps = detail.getProductSize();
                    BigDecimal lineTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    return new OrderItemResponse(
                            detail.getId(),
                            ps != null ? ps.getId() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getName() : null,
                            ps != null && ps.getProduct() != null ? ps.getProduct().getImage() : "",
                            ps != null && ps.getSize() != null ? ps.getSize().getSizeName() : null,
                            detail.getQuantity(),
                            detail.getPrice(),
                            lineTotal
                    );
                }).toList();

        response.setItems(items);
        return response;
    }
}
