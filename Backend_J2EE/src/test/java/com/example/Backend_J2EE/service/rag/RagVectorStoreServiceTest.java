package com.example.Backend_J2EE.service.rag;

import com.example.Backend_J2EE.entity.Product;
import com.example.Backend_J2EE.entity.RagProductVector;
import com.example.Backend_J2EE.repository.RagProductVectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagVectorStoreServiceTest {

    @Mock
    private RagProductVectorRepository vectorRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private RagVectorStoreService ragVectorStoreService;

    @Test
    void upsertDocuments_savesChangedVectorsAndDeletesStaleEntries() {
        Product firstProduct = product(1, "Running shoe");
        Product secondProduct = product(2, "Training shirt");
        Product ignoredProduct = product(null, "Ignored");

        RagProductDocument firstDocument = new RagProductDocument(firstProduct, "red running shoe", List.of("M"), 4.5d);
        RagProductDocument secondDocument = new RagProductDocument(secondProduct, "blue training shirt", List.of("L"), 4.2d);
        RagProductDocument ignoredDocument = new RagProductDocument(ignoredProduct, "no id", List.of(), 0.0d);

        String unchangedHash = sha256(secondDocument.document());
        RagProductVector existingVector = RagProductVector.builder()
                .productId(2)
                .documentHash(unchangedHash)
                .embeddingData("1.0,0.0")
                .build();

        when(vectorRepository.findByProductId(1)).thenReturn(Optional.empty());
        when(vectorRepository.findByProductId(2)).thenReturn(Optional.of(existingVector));
        when(embeddingService.embed(firstDocument.document())).thenReturn(new float[]{0.25f, 0.75f});

        ragVectorStoreService.upsertDocuments(List.of(firstDocument, secondDocument, ignoredDocument));

        ArgumentCaptor<RagProductVector> savedVectorCaptor = ArgumentCaptor.forClass(RagProductVector.class);
        verify(vectorRepository).save(savedVectorCaptor.capture());
        RagProductVector savedVector = savedVectorCaptor.getValue();
        assertEquals(1, savedVector.getProductId());
        assertEquals(sha256(firstDocument.document()), savedVector.getDocumentHash());
        assertEquals("0.25,0.75", savedVector.getEmbeddingData());

        verify(embeddingService).embed(firstDocument.document());
        verify(embeddingService, never()).embed(secondDocument.document());
        verify(vectorRepository).deleteByProductIdNotIn(List.of(1, 2));
    }

    @Test
    void upsertDocuments_deletesAllWhenNoValidProductIdsExist() {
        RagProductDocument ignoredDocument = new RagProductDocument(product(null, "Ignored"), "no id", List.of(), 0.0d);

        ragVectorStoreService.upsertDocuments(List.of(ignoredDocument));

        verify(vectorRepository).deleteAll();
        verifyNoInteractions(embeddingService);
        verifyNoMoreInteractions(vectorRepository);
    }

    @Test
    void hasAnyVectorForDocumentsReturnsFalseWhenNoValidIdsExist() {
        RagProductDocument ignoredDocument = new RagProductDocument(product(null, "Ignored"), "no id", List.of(), 0.0d);

        assertTrue(!ragVectorStoreService.hasAnyVectorForDocuments(List.of(ignoredDocument)));
        verifyNoInteractions(vectorRepository);
    }

    @Test
    void searchReturnsEmptyWhenDocumentsAreEmpty() {
        List<RagVectorStoreService.RagProductHit> hits = ragVectorStoreService.search("any", List.of(), 3);

        assertEquals(List.of(), hits);
        verifyNoInteractions(vectorRepository);
        verifyNoInteractions(embeddingService);
    }

    @Test
    void hasAnyVectorForDocumentsUsesDistinctProductIds() {
        Product firstProduct = product(11, "First");
        Product duplicateFirstProduct = product(11, "First copy");
        Product nullProduct = product(null, "Ignored");

        RagProductDocument firstDocument = new RagProductDocument(firstProduct, "first", List.of(), 0.0d);
        RagProductDocument duplicateDocument = new RagProductDocument(duplicateFirstProduct, "first copy", List.of(), 0.0d);
        RagProductDocument ignoredDocument = new RagProductDocument(nullProduct, "ignored", List.of(), 0.0d);

        when(vectorRepository.existsByProductIdIn(List.of(11))).thenReturn(true);

        assertTrue(ragVectorStoreService.hasAnyVectorForDocuments(List.of(firstDocument, duplicateDocument, ignoredDocument)));

        verify(vectorRepository).existsByProductIdIn(List.of(11));
        verifyNoMoreInteractions(vectorRepository);
    }

    @Test
    void searchRanksResultsByCombinedScore() {
        Product firstProduct = product(1, "Running shoe");
        Product secondProduct = product(2, "Winter hoodie");

        RagProductDocument firstDocument = new RagProductDocument(firstProduct, "red running shoe", List.of("M"), 4.8d);
        RagProductDocument secondDocument = new RagProductDocument(secondProduct, "blue winter hoodie", List.of("L"), 4.1d);

        when(embeddingService.embed("red shoe")).thenReturn(new float[]{1.0f, 0.0f});
        when(vectorRepository.findByProductIdIn(List.of(1, 2))).thenReturn(List.of(
                RagProductVector.builder().productId(1).embeddingData("1.0,0.0").build(),
                RagProductVector.builder().productId(2).embeddingData("0.0,1.0").build()
        ));

        List<RagVectorStoreService.RagProductHit> hits = ragVectorStoreService.search("red shoe", List.of(firstDocument, secondDocument), 1);

        assertEquals(1, hits.size());
        assertSame(firstDocument, hits.get(0).document());
        assertTrue(hits.get(0).score() > 0.8d);
        verify(embeddingService).embed("red shoe");
        verify(vectorRepository).findByProductIdIn(List.of(1, 2));
        verifyNoMoreInteractions(vectorRepository);
    }

    @Test
    void searchSkipsVectorsWithMissingMatchingDocumentAndClampsTopKToOne() {
        Product product = product(1, "Running shoe");
        RagProductDocument document = new RagProductDocument(product, "red running shoe", List.of("M"), 4.8d);

        when(embeddingService.embed("red shoe")).thenReturn(new float[]{1.0f, 0.0f});
        when(vectorRepository.findByProductIdIn(List.of(1))).thenReturn(List.of(
                RagProductVector.builder().productId(1).embeddingData("1.0,0.0").build(),
                RagProductVector.builder().productId(99).embeddingData("0.0,1.0").build()
        ));

        List<RagVectorStoreService.RagProductHit> hits = ragVectorStoreService.search("red shoe", List.of(document), 0);

        assertEquals(1, hits.size());
        assertSame(document, hits.get(0).document());
    }

    private static Product product(Integer id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .build();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}