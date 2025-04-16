package com.nastel.expenseplanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainBudgetActivity extends AppCompatActivity {

    private LinearLayout navHome, navSettings, navTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_budget);

        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        // Handle Edge-to-Edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find buttons by their IDs
        LinearLayout addBudgetButton = findViewById(R.id.add_budget_button);
        LinearLayout viewBudgetsButton = findViewById(R.id.view_budgets_button);

        // Add Budget Button click listener
        addBudgetButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainBudgetActivity.this, CreateBudgetActivity.class);
            startActivity(intent);
        });

        // View Budgets Button click listener
        viewBudgetsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainBudgetActivity.this, ViewBudgetsActivity.class);
            startActivity(intent);
        });

        // Navigation to Home
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(MainBudgetActivity.this, MainActivity.class);
            startActivity(homeIntent);
            finish();
        });

        // Navigation to Transaction
        navTransaction.setOnClickListener(v -> {
            Intent transactionIntent = new Intent(MainBudgetActivity.this, TransactionActivity.class);
            startActivity(transactionIntent);
            finish();
        });

        // Navigation to Settings
        navSettings.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(MainBudgetActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            finish();
        });
    }
}
