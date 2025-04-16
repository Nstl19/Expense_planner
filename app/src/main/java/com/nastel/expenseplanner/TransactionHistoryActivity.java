package com.nastel.expenseplanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class TransactionHistoryActivity extends AppCompatActivity {

    private LinearLayout navHome, navSettings, navTransaction;
    private RecyclerView transactionRecyclerView;
    private TransactionAdapter transactionAdapter;
    private ArrayList<Transaction> transactionList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize navigation buttons
        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        // Initialize RecyclerView
        transactionRecyclerView = findViewById(R.id.recycler_view_transactions);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(transactionList, this::onTransactionClick);
        transactionRecyclerView.setAdapter(transactionAdapter);

        // Load transactions from Firestore
        loadTransactions();

        // Navigation to Home
        navHome.setOnClickListener(v -> navigateTo(MainActivity.class));

        // Navigation to Transaction
        navTransaction.setOnClickListener(v -> navigateTo(TransactionActivity.class));

        // Navigation to Settings
        navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));
    }

    /**
     * Load transactions from Firestore and populate the RecyclerView.
     */
    private void loadTransactions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();  // Get the UID

            db.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        transactionList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Transaction transaction = document.toObject(Transaction.class);
                            if (transaction != null) {
                                transaction.setId(document.getId());  // Set Firestore document ID
                                transactionList.add(transaction);
                            }
                        }
                        transactionAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load transactions.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Handle click events for individual transactions.
     *
     * @param transaction The transaction clicked by the user.
     */
    private void onTransactionClick(Transaction transaction) {
        if (transaction != null) {
            Intent updateIntent = new Intent(TransactionHistoryActivity.this, UpdateTransactionActivity.class);
            updateIntent.putExtra("transactionId", transaction.getId());
            updateIntent.putExtra("transactionAmount", transaction.getAmount());
            updateIntent.putExtra("transactionCategory", transaction.getCategory());
            updateIntent.putExtra("transactionDescription", transaction.getDescription());
            updateIntent.putExtra("transactionDate", transaction.getDate());
            updateIntent.putExtra("transactionType", transaction.getType()); // Income or Expense
            startActivity(updateIntent);
        }
    }

    /**
     * Simplifies navigation.
     */
    private void navigateTo(Class<?> activity) {
        Intent intent = new Intent(TransactionHistoryActivity.this, activity);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadTransactions();
    }
}