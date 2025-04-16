package com.nastel.expenseplanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TransactionActivity extends AppCompatActivity {

    private LinearLayout navHome, navSettings, navTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        LinearLayout addTransactionButton = findViewById(R.id.add_transaction_button);
        LinearLayout viewTransactionHistoryButton = findViewById(R.id.view_history_button);
        LinearLayout viewStatisticsButton = findViewById(R.id.view_statistics_button);

        // Add Transaction Button click listener
        addTransactionButton.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });

        // View Transaction History Button click listener
        viewTransactionHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });

        // View Statistics Button click listener
        viewStatisticsButton.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, ViewStatisticsActivity.class);
            startActivity(intent);
        });

        // Navigation to Home
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(TransactionActivity.this, MainActivity.class);
            startActivity(homeIntent);
            finish();
        });

        // Navigation to Transaction (refreshes the current page)
        navTransaction.setOnClickListener(v -> {
            Intent TransactionIntent = new Intent(TransactionActivity.this, TransactionActivity.class);
            startActivity(TransactionIntent);
            finish();
        });

        // Navigation to Settings
        navSettings.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(TransactionActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            finish();
        });

    }
}
