package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learner_id", nullable = false)
    private Long learnerId;

    @Column(name = "plan", nullable = false, length = 20)
    private String plan; // BASIC, STANDARD, PREMIUM

    @Column(name = "method", nullable = false, length = 20)
    private String method; // STRIPE, CAMPAY_ORANGE, CAMPAY_MTN, PAYPAL

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 5)
    private String currency = "XAF"; // FCFA

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED, CANCELLED

    @Column(name = "external_ref", length = 200)
    private String externalRef; // Stripe payment intent, CamPay ref, PayPal order id

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // pour Orange/MTN

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLearnerId() { return learnerId; }
    public void setLearnerId(Long v) { this.learnerId = v; }
    public String getPlan() { return plan; }
    public void setPlan(String v) { this.plan = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String v) { this.externalRef = v; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}
