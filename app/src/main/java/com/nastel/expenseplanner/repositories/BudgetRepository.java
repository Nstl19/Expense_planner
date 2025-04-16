package com.nastel.expenseplanner.repositories;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nastel.expenseplanner.models.Budget;

public class BudgetRepository {
    private final FirebaseFirestore db;
    private final CollectionReference budgetRef;

    public BudgetRepository() {
        db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        budgetRef = db.collection("users").document(userId).collection("budgets");
    }

    // Add a new budget to Firestore
    public void addBudget(Budget budget, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        String budgetId = budgetRef.document().getId(); // Auto-generate ID
        budget.setBudgetId(budgetId);
        budgetRef.document(budgetId).set(budget)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // Update an existing budget
    public void updateBudget(Budget budget, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        budgetRef.document(budget.getBudgetId()).set(budget)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // Delete a budget
    public void deleteBudget(String budgetId, OnSuccessListener<Void> successListener, OnFailureListener failureListener) {
        budgetRef.document(budgetId).delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // Get budgets for the current user
    public CollectionReference getBudgets() {
        return budgetRef;
    }
}
