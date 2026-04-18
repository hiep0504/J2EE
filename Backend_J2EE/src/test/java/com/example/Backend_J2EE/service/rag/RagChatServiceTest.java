package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.dto.rag.RagChatResponse;
import com.example.Backend_J2EE.dto.rag.RagChatTurn;
import com.example.Backend_J2EE.dto.rag.RagProductSuggestion;
import com.example.Backend_J2EE.entity.Category;
import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.ProductSize;
import com.example.Backend_J2EE.entity.Review;
import com.example.Backend_J2EE.entity.Size;
import com.example.Backend_J2EE.repository.ProductRepository;
import com.example.Backend_J2EE.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RagVectorStoreService ragVectorStoreService;

    @Mock
    private LlmAnswerService llmAnswerService;

    private RagChatService service;

    @BeforeEach
    void setUp() {
        service = new RagChatService(productRepository, reviewRepository, ragVectorStoreService, llmAnswerService, 5, 0.20, 2, 120);
    }

    @Test
    void askRejectsBlankQuestion() {
        RagChatResponse response = service.ask("   ", List.of(), null);

        assertTrue(response.answer().contains("Ban hay nhap cau hoi cu the"));
        assertTrue(response.products().isEmpty());
        verifyNoInteractionsOnRagPipeline();
    }

    @Test
    void askReturnsNoDataMessageWhenNoProductsExist() {
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        RagChatResponse response = service.ask("Can giay nao tot?", List.of(), null);

        assertTrue(response.answer().contains("He thong chua co du lieu san pham"));
        assertTrue(response.products().isEmpty());
        verify(ragVectorStoreService, never()).hasAnyVectorForDocuments(anyList());
    }

    @Test
    void askReturnsIndexNotReadyWhenVectorsAreMissing() {
        Product product = product(1, "Running shoe", "Shoes", new BigDecimal("199000"), "shoe.jpg", List.of("42", "43"));
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));
        when(reviewRepository.findAll()).thenReturn(List.of());
        when(ragVectorStoreService.hasAnyVectorForDocuments(anyList())).thenReturn(false);

        RagChatResponse response = service.ask("Can nao tot?", List.of(), null);

        assertTrue(response.answer().contains("Chi muc vector chua san sang"));
        assertTrue(response.products().isEmpty());
        verify(ragVectorStoreService, never()).search(anyString(), anyList(), eq(5));
    }

    @Test
    void askReturnsNoMatchWhenAllHitsAreBelowThreshold() {
        Product product = product(1, "Running shoe", "Shoes", new BigDecimal("199000"), "shoe.jpg", List.of("42", "43"));
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));
        when(reviewRepository.findAll()).thenReturn(List.of());
        when(ragVectorStoreService.hasAnyVectorForDocuments(anyList())).thenReturn(true);
        when(ragVectorStoreService.search(anyString(), anyList(), eq(5))).thenReturn(List.of(
                new RagVectorStoreService.RagProductHit(
                        new RagProductDocument(product, "doc", List.of("42"), 4.5d),
                        0.19d
                )
        ));

        RagChatResponse response = service.ask("Can giay nao tot?", List.of(), null);

        assertTrue(response.answer().contains("Minh chua tim thay ket qua du phu hop"));
        assertTrue(response.products().isEmpty());
        verify(llmAnswerService, never()).generateAnswer(anyString(), anyString(), anyList(), anyList(), anyString());
    }

    @Test
    void rebuildIndexBuildsDocumentsWithAverageRatingsAndDistinctSizes() {
        Product product = productWithDetails();
        Review validReview1 = Review.builder().product(product).rating(5).build();
        Review validReview2 = Review.builder().product(product).rating(3).build();
        Review ignoredMissingProduct = Review.builder().rating(4).build();
        Review ignoredMissingRating = Review.builder().product(product).build();

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));
        when(reviewRepository.findAll()).thenReturn(List.of(validReview1, validReview2, ignoredMissingProduct, ignoredMissingRating));

        ArgumentCaptor<List<RagProductDocument>> captor = ArgumentCaptor.forClass(List.class);

        int size = service.rebuildIndex();

        verify(ragVectorStoreService).upsertDocuments(captor.capture());
        assertEquals(1, size);
        List<RagProductDocument> docs = captor.getValue();
        assertEquals(1, docs.size());
        RagProductDocument document = docs.get(0);
        assertTrue(document.document().contains("San pham: Premier Boot"));
        assertTrue(document.document().contains("Size: 40, 42"));
        assertEquals(4.0d, document.averageRating(), 0.0001d);
        assertEquals(List.of("40", "42"), document.sizes());
    }

    @Test
    void rebuildIndexSendsEmptyListWhenNoProductsExist() {
        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        int size = service.rebuildIndex();

        assertEquals(0, size);
        verify(ragVectorStoreService).upsertDocuments(List.of());
    }

    @Test
    void askBuildsQueryAndTrimsHistoryBeforeGeneratingAnswer() {
        Product product = productWithDetails();
        RagVectorStoreService.RagProductHit hit = new RagVectorStoreService.RagProductHit(
                new RagProductDocument(product, "San pham: Premier Boot. Danh muc: Shoes. Gia: 100000 VND. Size: 40, 42. Rating trung binh: 4.0. Mo ta: Boot tot", List.of("40", "42"), 4.0d),
                0.92d
        );

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));
        when(reviewRepository.findAll()).thenReturn(List.of());
        when(ragVectorStoreService.hasAnyVectorForDocuments(anyList())).thenReturn(true);
        when(ragVectorStoreService.search(anyString(), anyList(), eq(5))).thenReturn(List.of(hit));
        when(llmAnswerService.generateAnswer(anyString(), anyString(), anyList(), anyList(), anyString())).thenReturn("ANSWER");

        List<RagChatTurn> history = new ArrayList<>();
        history.add(turn("system", "ignored"));
        history.add(turn("user", "   "));
        history.add(turn("assistant", "Cau tra loi cu"));
        history.add(turn("user", "   size 42   "));

        RagChatResponse response = service.ask("  can giay  ", history, "  Premier Boot  ");

        ArgumentCaptor<String> searchQueryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<RagChatTurn>> historyCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> focusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<RagProductSuggestion>> suggestionCaptor = ArgumentCaptor.forClass(List.class);

        verify(ragVectorStoreService).search(searchQueryCaptor.capture(), anyList(), eq(5));
        verify(llmAnswerService).generateAnswer(
                eq("can giay"),
                contextCaptor.capture(),
                suggestionCaptor.capture(),
                historyCaptor.capture(),
                focusCaptor.capture()
        );

        assertEquals("can giay Premier Boot size 42", searchQueryCaptor.getValue());
        assertEquals("Premier Boot", focusCaptor.getValue());
        assertEquals(2, historyCaptor.getValue().size());
        assertEquals("assistant", historyCaptor.getValue().get(0).getRole());
        assertEquals("Cau tra loi cu", historyCaptor.getValue().get(0).getContent());
        assertEquals("user", historyCaptor.getValue().get(1).getRole());
        assertEquals("size 42", historyCaptor.getValue().get(1).getContent());
        assertTrue(contextCaptor.getValue().contains("Premier Boot"));
        assertEquals(1, suggestionCaptor.getValue().size());
        assertEquals("ANSWER", response.answer());
        assertEquals(1, response.products().size());
    }

    private Product product(Integer id, String name, String categoryName, BigDecimal price, String image, List<String> sizeNames) {
        return Product.builder()
                .id(id)
                .name(name)
                .category(Category.builder().id(10).name(categoryName).build())
                .price(price)
                .image(image)
                .build();
    }

    private Product productWithDetails() {
        Product product = Product.builder()
                .id(1)
                .name("Premier Boot")
                .category(Category.builder().id(10).name("Shoes").build())
                .price(new BigDecimal("100000"))
                .description("Boot tot")
                .image("boot.jpg")
                .build();

        Size size40 = Size.builder().id(40).sizeName("40").build();
        Size size42 = Size.builder().id(42).sizeName("42").build();
        Size sizeDuplicate = Size.builder().id(43).sizeName("42").build();
        Size sizeBlank = Size.builder().id(44).sizeName(" ").build();

        ProductSize ps1 = ProductSize.builder().product(product).size(size42).build();
        ProductSize ps2 = ProductSize.builder().product(product).size(size40).build();
        ProductSize ps3 = ProductSize.builder().product(product).size(sizeDuplicate).build();
        ProductSize ps4 = ProductSize.builder().product(product).size(sizeBlank).build();
        product.setProductSizes(List.of(ps1, ps2, ps3, ps4));
        return product;
    }

    private RagChatTurn turn(String role, String content) {
        RagChatTurn turn = new RagChatTurn();
        turn.setRole(role);
        turn.setContent(content);
        return turn;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verifyNoInteractionsOnRagPipeline() {
        verify(ragVectorStoreService, never()).hasAnyVectorForDocuments(anyList());
        verify(ragVectorStoreService, never()).search(anyString(), anyList(), eq(5));
        verify(llmAnswerService, never()).generateAnswer(anyString(), anyString(), anyList(), anyList(), anyString());
    }
}