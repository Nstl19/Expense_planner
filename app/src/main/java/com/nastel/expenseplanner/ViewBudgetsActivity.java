package com.nastel.expenseplanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nastel.expenseplanner.adapters.BudgetAdapter;
import com.nastel.expenseplanner.models.Budget;

import java.util.ArrayList;
import java.util.List;

public class ViewBudgetsActivity extends AppCompatActivity implements BudgetAdapter.OnBudgetClickListener {

    private RecyclerView recyclerView;
    private BudgetAdapter budgetAdapter;
    private List<Budget> budgetList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private CollectionReference budgetsRef;

    private LinearLayout navHome, navSettings, navTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_budgets);

        recyclerView = findViewById(R.id.recycler_view_budgets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        budgetList = new ArrayList<>();
        budgetAdapter = new BudgetAdapter(budgetList, this);
        recyclerView.setAdapter(budgetAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        budgetsRef = db.collection("budgets");

        loadBudgets();

        // Navigation Buttons
        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        navHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        navTransaction.setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        navSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadBudgets() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        budgetsRef = db.collection("users").document(userId).collection("budgets");

        budgetsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                budgetList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    try {
                        Log.d("FirestoreData", "Budget Data: " + doc.getData()); // Debugging log

                        Budget budget = doc.toObject(Budget.class);
                        budget.setBudgetId(doc.getId()); // Ensure Firestore assigns the ID
                        budgetList.add(budget);
                    } catch (Exception e) {
                        Log.e("FirestoreError", "Invalid Budget Data: " + doc.getData(), e);
                    }
                }
                budgetAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(ViewBudgetsActivity.this, "Error loading budgets", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBudgetClick(Budget budget) {
        if (budget.getBudgetId() == null || budget.getBudgetId().isEmpty()) {
            Toast.makeText(this, "Error: Budget ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEBUG", "Budget Clicked - ID: " + budget.getBudgetId());

        Intent intent = new Intent(ViewBudgetsActivity.this, UpdateBudgetActivity.class);
        intent.putExtra("BUDGET_ID", budget.getBudgetId());
        intent.putExtra("BUDGET_NAME", budget.getName());
        intent.putExtra("BUDGET_AMOUNT", budget.getAmount());
        intent.putExtra("BUDGET_START_DATE", budget.getStartDate());
        intent.putExtra("BUDGET_END_DATE", budget.getEndDate());
        startActivity(intent);
    }

    @Override
    public void onEditBudget(Budget budget) {
        if (budget.getBudgetId() == null || budget.getBudgetId().isEmpty()) {
            Toast.makeText(this, "Cannot edit budget: ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEBUG", "Editing Budget - ID: " + budget.getBudgetId());

        Intent intent = new Intent(this, UpdateBudgetActivity.class);
        intent.putExtra("BUDGET_ID", budget.getBudgetId()); // Corrected key name
        startActivity(intent);
    }

    @Override
    public void onDeleteBudget(Budget budget) {
        if (budget.getBudgetId() == null || budget.getBudgetId().isEmpty()) {
            Toast.makeText(this, "Cannot delete budget: ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEBUG", "Deleting Budget - ID: " + budget.getBudgetId());

        budgetsRef.document(budget.getBudgetId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Budget deleted", Toast.LENGTH_SHORT).show();
                    loadBudgets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting budget", Toast.LENGTH_SHORT).show();
                    Log.e("DEBUG", "Error deleting budget: " + e.getMessage());
                });
    }
}
