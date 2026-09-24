package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final ContactUnlockRepository contactUnlockRepository;
    private final ListingRepository listingRepository;
    private final CategoryService categoryService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.contact-unlock-amount}")
    private int contactUnlockAmount;

    /**
     * Returns true if the listing belongs to a free category (or free parent category).
     */
    public boolean isListingFree(Long listingId) {
        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null) return false;
        return categoryService.isCategoryFree(listing.getCategory());
    }

    /**
     * Check if user has access to contact for this listing.
     * Access is granted if: the category is free, OR the user has paid to unlock.
     */
    public boolean isContactUnlocked(Long buyerId, Long listingId) {
        if (isListingFree(listingId)) {
            return true;
        }
        return contactUnlockRepository.existsByBuyerIdAndListingId(buyerId, listingId);
    }

    /**
     * Get seller phone if contact is unlocked (either free category or paid).
     */
    public String getUnlockedContact(Long buyerId, Long listingId) {
        if (!isContactUnlocked(buyerId, listingId)) {
            return null;
        }
        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null) return null;
        return listing.getUser().getPhone();
    }

    /**
     * Create Razorpay order for contact unlock
     */
    public Map<String, Object> createUnlockOrder(Long listingId, User buyer) throws RazorpayException {
        // Free category — no payment needed
        if (isListingFree(listingId)) {
            throw new RuntimeException("This listing's contact is free — no payment required");
        }

        // Check if already unlocked
        if (contactUnlockRepository.existsByBuyerIdAndListingId(buyer.getId(), listingId)) {
            throw new RuntimeException("Contact already unlocked for this listing");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Can't unlock own listing
        if (listing.getUser().getId().equals(buyer.getId())) {
            throw new RuntimeException("Cannot unlock your own listing");
        }

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", contactUnlockAmount); // in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "unlock_" + listingId + "_" + buyer.getId());

        Order razorpayOrder = client.orders.create(orderRequest);

        // Save to DB
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .user(buyer)
                .listing(listing)
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(contactUnlockAmount)
                .build();
        paymentOrderRepository.save(paymentOrder);

        return Map.of(
                "orderId", razorpayOrder.get("id"),
                "amount", contactUnlockAmount,
                "currency", "INR",
                "keyId", razorpayKeyId
        );
    }

    /**
     * Verify payment and unlock contact
     */
    @Transactional
    public Map<String, Object> verifyAndUnlock(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            User buyer) throws RazorpayException {

        // Verify signature
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        boolean valid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        if (!valid) {
            throw new RuntimeException("Payment verification failed");
        }

        // Update payment order
        PaymentOrder paymentOrder = paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        paymentOrder.setRazorpayPaymentId(razorpayPaymentId);
        paymentOrder.setRazorpaySignature(razorpaySignature);
        paymentOrder.setStatus("PAID");
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);

        // Create contact unlock record
        Listing listing = paymentOrder.getListing();
        ContactUnlock unlock = ContactUnlock.builder()
                .buyer(buyer)
                .seller(listing.getUser())
                .listing(listing)
                .paymentOrder(paymentOrder)
                .build();
        contactUnlockRepository.save(unlock);

        // Return unlocked contact
        return Map.of(
                "message", "Contact unlocked successfully",
                "phone", listing.getUser().getPhone(),
                "sellerName", listing.getUser().getName() != null ? listing.getUser().getName() : ""
        );
    }
}
