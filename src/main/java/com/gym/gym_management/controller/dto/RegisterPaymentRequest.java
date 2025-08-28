package com.gym.gym_management.controller.dto;


import java.time.LocalDate;

public class RegisterPaymentRequest {
    private Double amount;
    private LocalDate paymentDate;
    private Duration duration;

    public enum Duration {
        QUINCE_DIAS(15),
        UN_MES(30);

        private final int days;
        Duration(int days) {
            this.days = days;
        }
        public int getDays() {
            return days;
        }
    }
    public RegisterPaymentRequest(){
    }
    public RegisterPaymentRequest(Double amount, LocalDate paymentDate, Duration duration) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.duration = duration;
    }
    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

}
