package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.review.ReviewResponse;
import com.example.Backend_J2EE.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @Test
    void getReviewsByProductMapsResponses() {
        ReviewResponse response = new ReviewResponse();
        response.setId(3);
        response.setProductId(1);
        response.setProductName("Running Shoe");
        response.setAccountId(2);
        response.setAccountUsername("alice");
        when(reviewService.getReviewsByProduct(1)).thenReturn(List.of(response));

        List<ReviewResponse> result = reviewController.getReviewsByProduct(1);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getId());
        assertEquals(1, result.get(0).getProductId());
        assertEquals("Running Shoe", result.get(0).getProductName());
        assertEquals(2, result.get(0).getAccountId());
        assertEquals("alice", result.get(0).getAccountUsername());
        verify(reviewService).getReviewsByProduct(1);
    }

    @Test
    void createReviewDelegatesToService() {
        ReviewResponse expected = new ReviewResponse();
        expected.setId(7);
        expected.setProductId(1);
        expected.setAccountId(2);
        expected.setRating(5);
        expected.setComment("nice");

        when(reviewService.createReview(1, 2, 5, "nice", null, null)).thenReturn(expected);

        ReviewResponse response = reviewController.createReview(1, 2, 5, "nice", null, null);

        assertSame(expected, response);
        verify(reviewService).createReview(1, 2, 5, "nice", null, null);
    }

    @Test
    void createReviewPropagatesServiceValidationError() {
        when(reviewService.createReview(1, 2, 6, "bad", null, null))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "invalid"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewController.createReview(1, 2, 6, "bad", null, null));

        assertEquals(400, ex.getStatusCode().value());
    }
}
