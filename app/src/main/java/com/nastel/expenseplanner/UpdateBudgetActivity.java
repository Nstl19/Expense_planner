package com.nastel.expenseplanner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UpdateBudgetActivity extends AppCompatActivity {

    private EditText budgetNameInput, budgetAmountInput;
    private RadioGroup periodGroup;
    private TextView startDateText, endDateText;
    private Button startDateButton, endDateButton, updateBudgetButton, deleteBudgetButton;
    private String selectedPeriod = "Monthly"; // Default period
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String budgetId;
    private DocumentReference budgetRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_budget);
        Log.d("UpdateBudgetActivity", "Received Budget ID: " + budgetId);

        initializeUI();
        initializeFirebase();
        retrieveBudgetId();
        setupListeners();
        loadBudgetDetails();
    }

    private void initializeUI() {
        budgetNameInput = findViewById(R.id.name_text);
        budgetAmountInput = findViewById(R.id.amount_text);
        periodGroup = findViewById(R.id.period_group);
        startDateText = findViewById(R.id.start_date_text);
        endDateText = findViewById(R.id.end_date_text);
        startDateButton = findViewById(R.id.start_date_button);
        endDateButton = findViewById(R.id.end_date_button);
        updateBudgetButton = findViewById(R.id.update_budget_button);
        deleteBudgetButton = findViewById(R.id.delete_budget_button);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void retrieveBudgetId() {
        budgetId = getIntent().getStringExtra("BUDGET_ID");
        if (budgetId == null || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Invalid budget data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        budgetRef = db.collection("users").document(userId).collection("budgets").document(budgetId);
    }

    private void setupListeners() {
        startDateButton.setOnClickListener(v -> showDatePickerDialog(startDateText));
        endDateButton.setOnClickListener(v -> showDatePickerDialog(endDateText));
        periodGroup.setOnCheckedChangeListener((group, checkedId) -> updateSelectedPeriod(checkedId));
        updateBudgetButton.setOnClickListener(v -> updateBudgetInDatabase());
        deleteBudgetButton.setOnClickListener(v -> confirmDeleteBudget());
    }

    private void updateSelectedPeriod(int checkedId) {
        if (checkedId == R.id.radio_weekly) {
            selectedPeriod = "Weekly";
        } else if (checkedId == R.id.radio_monthly) {
            selectedPeriod = "Monthly";
        } else {
            selectedPeriod = "Yearly";
        }
    }

    private void loadBudgetDetails() {
        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                budgetNameInput.setText(documentSnapshot.getString("name"));
                budgetAmountInput.setText(String.valueOf(documentSnapshot.getDouble("amount")));
                startDateText.setText(documentSnapshot.getString("startDate"));
                endDateText.setText(documentSnapshot.getString("endDate"));
                selectedPeriod = documentSnapshot.getString("period");

                if (selectedPeriod.equals("Weekly")) {
                    periodGroup.check(R.id.radio_weekly);
                } else if (selectedPeriod.equals("Monthly")) {
                    periodGroup.check(R.id.radio_monthly);
                } else {
                    periodGroup.check(R.id.radio_yearly);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load budget", Toast.LENGTH_SHORT).show();
            Log.e("FirestoreError", "Error fetching budget", e);
        });
    }

    private void showDatePickerDialog(TextView dateTextView) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            dateTextView.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateBudgetInDatabase() {
        String budgetName = budgetNameInput.getText().toString().trim();
        String budgetAmount = budgetAmountInput.getText().toString().trim();
        String startDate = startDateText.getText().toString();
        String endDate = endDateText.getText().toString();

        if (budgetName.isEmpty() || budgetAmount.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return;
        }

        double newAmount;
        try {
            newAmount = Double.parseDouble(budgetAmount);
            if (newAmount <= 0) {
                Toast.makeText(this, "Budget amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double previousAmount = documentSnapshot.getDouble("amount");
                Double previousRemaining = documentSnapshot.getDouble("remainingAmount");

                if (previousAmount == null || previousRemaining == null) {
                    previousAmount = 0.0;
                    previousRemaining = 0.0;
                }


                double newRemainingAmount = previousRemaining + (newAmount - previousAmount);

                Map<String, Object> budgetData = new HashMap<>();
                budgetData.put("name", budgetName);
                budgetData.put("amount", newAmount);
                budgetData.put("remainingAmount", newRemainingAmount);  // Ensure this updates!
                budgetData.put("startDate", startDate);
                budgetData.put("endDate", endDate);
                budgetData.put("period", selectedPeriod);

                budgetRef.update(budgetData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(UpdateBudgetActivity.this, "Budget Updated!", Toast.LENGTH_SHORT).show();
                            finish();  // Close activity after successful update
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UpdateBudgetActivity.this, "Error updating budget", Toast.LENGTH_SHORT).show();
                            Log.e("FirestoreError", "Error updating budget", e);
                        });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load budget", Toast.LENGTH_SHORT).show();
            Log.e("FirestoreError", "Error fetching budget", e);
        });
    }


    private void confirmDeleteBudget() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete this budget?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBudget())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBudget() {
        budgetRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UpdateBudgetActivity.this, "Budget Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UpdateBudgetActivity.this, "Error deleting budget", Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", "Error deleting budget", e);
                });
    }
}
