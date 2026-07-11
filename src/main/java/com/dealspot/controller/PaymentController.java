package com.dealspot.controller;

import com.dealspot.entity.User;
import com.dealspot.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create order to unlock contact for a listing
     */
    @PostMapping("/unlock/{listingId}")
    public ResponseEntity<Map<String, Object>> createUnlockOrder(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) throws RazorpayException {
        return ResponseEntity.ok(paymentService.createUnlockOrder(listingId, user));
    }

    /**
     * Verify payment after Razorpay checkout and unlock contact
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) throws RazorpayException {

        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        return ResponseEntity.ok(paymentService.verifyAndUnlock(orderId, paymentId, signature, user));
    }

    /**
     * Check if contact is already unlocked
     */
    @GetMapping("/unlock/check/{listingId}")
    public ResponseEntity<Map<String, Object>> checkUnlock(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {

        boolean unlocked = paymentService.isContactUnlocked(user.getId(), listingId);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("unlocked", unlocked);

        if (unlocked) {
            String phone = paymentService.getUnlockedContact(user.getId(), listingId);
            response.put("phone", phone);
        }

        return ResponseEntity.ok(response);
    }
}
