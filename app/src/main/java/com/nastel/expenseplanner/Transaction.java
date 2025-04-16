package com.nastel.expenseplanner;

import android.os.Parcel;
import android.os.Parcelable;

public class Transaction implements Parcelable {
    private String id;
    private String userEmail;
    private String userName;
    private String category;
    private double amount;
    private String date;
    private String description;
    private String type;
    private String budgetId; // Nullable: Only present if linked to a budget

    public Transaction() {
        // Default constructor
    }

    public String getType() {
        return type != null ? type.toLowerCase() : "expense"; // Default to expense if null
    }

    public void setType(String type) {
        this.type = type;
    }

    protected Transaction(Parcel in) {
        id = in.readString();
        userEmail = in.readString();
        userName = in.readString();
        category = in.readString();
        amount = in.readDouble();
        date = in.readString();
        description = in.readString();
        type = in.readString();
        budgetId = in.readString(); // Read budgetId from parcel
    }

    public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
        @Override
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        @Override
        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(String budgetId) {
        this.budgetId = budgetId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userEmail);
        dest.writeString(userName);
        dest.writeString(category);
        dest.writeDouble(amount);
        dest.writeString(date);
        dest.writeString(description);
        dest.writeString(type);
        dest.writeString(budgetId);
    }
}
