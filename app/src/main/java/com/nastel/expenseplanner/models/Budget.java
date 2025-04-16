package com.nastel.expenseplanner.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

public class Budget {
    private String budgetId;  // Firestore document ID (excluded from serialization)
    private String userId;
    private String name;
    private double amount;   // Matches Firestore field 'amount'
    private double amountSpent;
    private String category;
    private String startDate; // Matches Firestore field 'startDate'
    private String endDate;   // Matches Firestore field 'endDate'
    private String period;    // Matches Firestore field 'period'
    private List<String> transactionIds;

    // **Empty Constructor for Firestore**
    public Budget() {
        this.transactionIds = new ArrayList<>();
        this.amountSpent = 0;
    }

    public Budget(String budgetId, String userId, String name, double amount, String category, String startDate, String endDate, String period) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.name = name;
        this.amount = amount;
        this.amountSpent = 0;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.transactionIds = new ArrayList<>();
    }

    // **Firestore Document ID (Excluded from Firestore)**
    @Exclude
    public String getBudgetId() { return budgetId; }
    public void setBudgetId(String budgetId) { this.budgetId = budgetId; }

    // **User ID**
    @PropertyName("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // **Budget Name**
    @PropertyName("name")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // **Total Budget Amount**
    @PropertyName("amount")
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    // **Amount Spent**
    @PropertyName("amountSpent")
    public double getAmountSpent() { return amountSpent; }
    public void setAmountSpent(double amountSpent) { this.amountSpent = amountSpent; }

    // **Start Date**
    @PropertyName("startDate")
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    // **End Date**
    @PropertyName("endDate")
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    // **Budget Period (Yearly, Monthly, etc.)**
    @PropertyName("period")
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    // **Transaction IDs**
    @PropertyName("transactionIds")
    public List<String> getTransactionIds() { return transactionIds; }
    public void setTransactionIds(List<String> transactionIds) { this.transactionIds = transactionIds; }

    // **Add a Transaction to Budget**
    public void addTransaction(String transactionId, double transactionAmount) {
        this.transactionIds.add(transactionId);
        this.amountSpent += transactionAmount;
    }

    // **Remove a Transaction (for edits/deletions)**
    public void removeTransaction(String transactionId, double transactionAmount) {
        this.transactionIds.remove(transactionId);
        this.amountSpent -= transactionAmount;
    }
}
