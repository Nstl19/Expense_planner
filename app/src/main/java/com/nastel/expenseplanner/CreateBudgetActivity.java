package com.nastel.expenseplanner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateBudgetActivity extends AppCompatActivity {

    private EditText budgetNameInput, budgetAmountInput;
    private RadioGroup periodGroup;
    private TextView startDateText, endDateText;
    private Button startDateButton, endDateButton, saveBudgetButton;
    private String selectedPeriod = "Monthly"; // Default period
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_budget_page);

        // Initialize UI elements
        budgetNameInput = findViewById(R.id.name_text);
        budgetAmountInput = findViewById(R.id.amount_text);
        periodGroup = findViewById(R.id.period_group);
        startDateText = findViewById(R.id.start_date_text);
        endDateText = findViewById(R.id.end_date_text);
        startDateButton = findViewById(R.id.start_date_button);
        endDateButton = findViewById(R.id.end_date_button);
        saveBudgetButton = findViewById(R.id.save_budget_button);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Handle period selection
        periodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Calendar calendar = Calendar.getInstance();

            if (checkedId == R.id.radio_weekly) {
                selectedPeriod = "Weekly";
                calendar.add(Calendar.DAY_OF_YEAR, 7);
            } else if (checkedId == R.id.radio_monthly) {
                selectedPeriod = "Monthly";
                calendar.add(Calendar.MONTH, 1);
            } else if (checkedId == R.id.radio_yearly) {
                selectedPeriod = "Yearly";
                calendar.add(Calendar.YEAR, 1);
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            endDateText.setText(day + "/" + month + "/" + year);
        });


        startDateButton.setOnClickListener(v -> showDatePickerDialog(startDateText));
        endDateButton.setOnClickListener(v -> showDatePickerDialog(endDateText));


        saveBudgetButton.setOnClickListener(v -> showSaveConfirmation());


        findViewById(R.id.nav_home).setOnClickListener(v -> navigateTo(MainActivity.class));
        findViewById(R.id.nav_settings).setOnClickListener(v -> navigateTo(SettingsActivity.class));
        findViewById(R.id.nav_transaction).setOnClickListener(v -> navigateTo(TransactionActivity.class));
    }


    private void showDatePickerDialog(TextView dateTextView) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            dateTextView.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }


    private void showSaveConfirmation() {
        String budgetName = budgetNameInput.getText().toString().trim();
        String budgetAmount = budgetAmountInput.getText().toString().trim();
        String startDate = startDateText.getText().toString();
        String endDate = endDateText.getText().toString();

        if (budgetName.isEmpty() || budgetAmount.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return;
        }


        double totalAmount;
        try {
            totalAmount = Double.parseDouble(budgetAmount);
            if (totalAmount <= 0) {
                Toast.makeText(this, "Budget amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }


        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User authentication failed!", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = auth.getCurrentUser().getUid();


        Log.d("FirestoreDebug", "User ID: " + userId);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String budgetId = db.collection("users").document(userId).collection("budgets").document().getId();
        DocumentReference budgetRef = db.collection("users").document(userId).collection("budgets").document(budgetId);


        Map<String, Object> budget = new HashMap<>();
        budget.put("userId", userId);
        budget.put("name", budgetName);
        budget.put("amount", totalAmount);
        budget.put("period", selectedPeriod);
        budget.put("startDate", startDate);
        budget.put("endDate", endDate);
        budget.put("remainingAmount", totalAmount);


        budgetRef.set(budget)
                .addOnSuccessListener(aVoid -> Log.d("FirestoreDebug", "Budget added successfully"))
                .addOnFailureListener(e -> Log.e("FirestoreDebug", "Failed to add budget", e)); // ✅ Fix Log.e

        Toast.makeText(CreateBudgetActivity.this, "Budget Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Navigation method
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}
