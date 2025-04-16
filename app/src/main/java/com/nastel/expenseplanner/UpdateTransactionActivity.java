package com.nastel.expenseplanner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class UpdateTransactionActivity extends AppCompatActivity {

    // UI Components
    private TextView dateText, timeText;
    private EditText descriptionInput, amountInput;
    private Spinner categorySpinner, budgetSpinner;
    private Button incomeButton, expenseButton, updateTransactionButton, deleteTransactionButton;
    private LinearLayout navHome, navTransaction, navSettings;

    // Data & Adapters
    private List<String> categories;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> budgetList = new ArrayList<>();
    private Map<String, String> budgetMap = new HashMap<>(); // Stores budget names & IDs

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    // Transaction Details
    private String transactionId = null;  // ID of transaction (for update/delete)
    private String transactionType = "Income"; // Default type
    private String budgetId;  // Stores the budget linked to this transaction
    private String selectedBudgetId; // Tracks which budget is currently selected
    private boolean isIncome; // True if income, False if expense

    // Shared Preferences (For Saving Data)
    private static final String PREFS_NAME = "ExpensePlannerPrefs";
    private static final String CATEGORIES_KEY = "categories";

    // Utility Variables
    private static final String TAG = "UpdateTransactionActivity"; // For Log.d() debugging}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_transaction);

        // Bottom Navigation Buttons
        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        // Firebase
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Views
        LinearLayout datePickerLayout = findViewById(R.id.date_picker);
        LinearLayout timePickerLayout = findViewById(R.id.time_picker);
        dateText = findViewById(R.id.date_text);
        timeText = findViewById(R.id.time_text);
        incomeButton = findViewById(R.id.income_button);
        expenseButton = findViewById(R.id.expense_button);
        updateTransactionButton = findViewById(R.id.update_transaction_button);
        deleteTransactionButton = findViewById(R.id.delete_transaction_button);
        descriptionInput = findViewById(R.id.description_input);
        amountInput = findViewById(R.id.amount_input);
        categorySpinner = findViewById(R.id.category_spinner);
        budgetSpinner = findViewById(R.id.budget_spinner);

        // Initialize Budget Mapping Before Loading Budgets
        budgetMap = new HashMap<>();
        loadBudgets(); // Moved below budgetMap initialization

        // Setup Category Dropdown
        setupCategoryDropdown();

        // Set Button Listeners
        incomeButton.setOnClickListener(v -> selectTransactionType("Income"));
        expenseButton.setOnClickListener(v -> selectTransactionType("Expense"));
        datePickerLayout.setOnClickListener(this::showDatePicker);
        timePickerLayout.setOnClickListener(this::showTimePicker);
        updateTransactionButton.setOnClickListener(v -> updateTransaction(true));
        deleteTransactionButton.setOnClickListener(v -> deleteTransaction());

        // Navigation Buttons
        navHome.setOnClickListener(v -> navigateTo(MainActivity.class));
        navTransaction.setOnClickListener(v -> navigateTo(TransactionActivity.class));
        navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

        // Handle Intent for Editing Existing Transaction
        handleEditTransaction();

        // Retrieve budgetName from Intent
        String budgetName = getIntent().getStringExtra("budgetName");
        if (budgetName != null && budgetList.contains(budgetName)) {
            budgetSpinner.setSelection(budgetList.indexOf(budgetName));
        }
    }


    private void handleEditTransaction() {
        Intent intent = getIntent();

        if (intent == null || !intent.hasExtra("transactionId")) {
            Toast.makeText(this, "Error: Transaction data not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        transactionId = intent.getStringExtra("transactionId");

        if (transactionId != null) {
            loadTransactionDetails(transactionId);  // Call loadTransactionDetails() here
        } else {
            Log.e("UpdateTransaction", "No transaction ID received");
        }

        // Safe null-checking before setting text fields
        descriptionInput.setText(intent.getStringExtra("description") != null ? intent.getStringExtra("description") : "");
        amountInput.setText(intent.getStringExtra("amount") != null ? intent.getStringExtra("amount") : "");
        dateText.setText(intent.getStringExtra("date") != null ? intent.getStringExtra("date") : "");
        timeText.setText(intent.getStringExtra("time") != null ? intent.getStringExtra("time") : "");

        // Handle transaction type
        transactionType = intent.getStringExtra("type");
        if (transactionType == null) {
            transactionType = "Income"; // Default to Income if null
        }
        selectTransactionType(transactionType);

        // Handle category selection safely
        String category = intent.getStringExtra("category");
        if (category != null && categories.contains(category)) {
            categorySpinner.setSelection(categories.indexOf(category));
        } else {
            categorySpinner.setSelection(0);
        }
    }

    private void setupCategoryDropdown() {
        categories = loadCategories();
        if (categories.isEmpty()) {
            categories = new ArrayList<>(Arrays.asList(
                    "Food", "Transport", "Entertainment", "Groceries", "Bills & Fees", "Work", "Gifts", "Petrol"
            ));
        }

        // Add "Select Category" as the first item (hint)
        categories.add(0, "Select Category");

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                // Display hint in gray
                if (position == 0) {
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };

        categorySpinner.setAdapter(categoryAdapter);

        // Ensure "Select Category" is shown initially
        categorySpinner.setSelection(0, false);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Stay on hint if no valid category is selected
                    categorySpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private void saveCategories() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> categorySet = new HashSet<>(categories);
        editor.putStringSet(CATEGORIES_KEY, categorySet);
        editor.apply();
    }

    private List<String> loadCategories() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> categorySet = sharedPreferences.getStringSet(CATEGORIES_KEY, new HashSet<>());
        return new ArrayList<>(categorySet);
    }

    private void selectTransactionType(String type) {
        transactionType = type;

        if ("Income".equals(type)) {
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_green_light)));
            expenseButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
        } else {
            expenseButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_red_light)));
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
        }
    }


    private void updateTransaction(boolean isUpdate) {
        try {
            String description = descriptionInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String category = (categorySpinner.getSelectedItem() != null) ?
                    categorySpinner.getSelectedItem().toString() : "Select Category";
            String date = dateText.getText().toString().trim();
            String time = timeText.getText().toString().trim();
            String selectedBudgetDisplayName = (budgetSpinner.getSelectedItem() != null) ?
                    budgetSpinner.getSelectedItem().toString() : "Select Budget";

            if (description.isEmpty() || amountStr.isEmpty() || category.equals("Select Category") ||
                    date.equals("Select Date") || time.equals("Select Time") || selectedBudgetDisplayName.equals("Select Budget")) {
                Toast.makeText(this, "Please fill in all the fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount entered!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                Log.e("UpdateTransaction", "User not logged in.");
                return;
            }

            String userId = user.getUid();

            Log.d("BudgetSpinner", "Budget selected: " + selectedBudgetDisplayName);
            Log.d("BudgetSpinner", "Budget Map: " + budgetMap.toString());

            String budgetId = budgetMap.get(selectedBudgetDisplayName);
            if (budgetId == null) {
                Log.e("UpdateTransaction", "Budget ID is null for budget: " + selectedBudgetDisplayName);
                Toast.makeText(this, "Invalid budget selection!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("description", description);
            transactionData.put("amount", amount);
            transactionData.put("category", category);
            transactionData.put("date", date);
            transactionData.put("time", time);
            transactionData.put("type", transactionType);
            transactionData.put("budgetId", budgetId);
            transactionData.put("timestamp", System.currentTimeMillis());

            if (isUpdate) {
                if (transactionId == null || transactionId.isEmpty()) {
                    Log.e("UpdateTransaction", "Transaction ID is null or empty during update.");
                    return;
                }

                DocumentReference transactionRef = firestore.collection("users").document(userId)
                        .collection("transactions").document(transactionId);

                transactionRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e("UpdateTransaction", "Transaction not found in Firestore.");
                        return;
                    }

                    Double oldAmount = documentSnapshot.contains("amount") ? documentSnapshot.getDouble("amount") : 0.0;
                    String oldBudgetId = documentSnapshot.getString("budgetId");
                    String oldTransactionType = documentSnapshot.getString("type");

                    if (oldBudgetId == null || oldTransactionType == null) {
                        Log.e("UpdateTransaction", "Old budget ID or transaction type is null.");
                        return;
                    }

                    Log.d("UpdateTransaction", "Old Transaction - Amount: " + oldAmount +
                            ", Budget ID: " + oldBudgetId + ", Type: " + oldTransactionType);

                    boolean wasExpense = oldTransactionType.equals("Expense");
                    boolean isExpense = transactionType.equals("Expense");

                    // **If budget is different, adjust old and new budgets separately**
                    if (!oldBudgetId.equals(budgetId)) {
                        adjustBudgetOnTransactionUpdate(userId, oldBudgetId, oldTransactionType, oldAmount,
                                budgetId, transactionType, amount);
                    }

                    // **Update transaction data in Firestore**
                    transactionRef.update(transactionData)
                            .addOnSuccessListener(aVoid -> {
                                // **Only update budget amount if the same budget is used**
                                if (oldBudgetId.equals(budgetId)) {
                                    updateBudgetAmount(userId, budgetId, oldAmount, amount, false, wasExpense, isExpense);
                                }

                                Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Log.e("UpdateTransaction", "Error updating transaction", e));
                }).addOnFailureListener(e -> Log.e("UpdateTransaction", "Error retrieving old transaction data", e));
            } else {
                firestore.collection("users").document(userId)
                        .collection("transactions")
                        .add(transactionData)
                        .addOnSuccessListener(documentReference -> {
                            boolean isExpense = transactionType.equals("Expense");
                            updateBudgetAmount(userId, budgetId, 0.0, amount, true, false, isExpense);

                            Toast.makeText(this, "Transaction added successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Log.e("UpdateTransaction", "Error adding transaction", e));
            }

        } catch (Exception e) {
            Log.e("UpdateTransaction", "Unexpected error in updateTransaction()", e);
        }
    }


    private void deleteTransaction() {
        Log.d("DeleteTransaction", "Function called");

        if (transactionId == null || transactionId.isEmpty()) {
            Log.e("DeleteTransaction", "Transaction ID is null or empty");
            return;
        }

        Log.d("DeleteTransaction", "Transaction ID: " + transactionId);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference transactionRef = firestore.collection("users").document(userId)
                .collection("transactions").document(transactionId);

        transactionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.e("DeleteTransaction", "Transaction not found in Firestore");
                return;
            }

            // Retrieve transaction details
            double amount = documentSnapshot.contains("amount") ? documentSnapshot.getDouble("amount") : 0.0;
            String budgetId = documentSnapshot.getString("budgetId");
            String transactionType = documentSnapshot.getString("type"); // Ensure correct field name

            Log.d("DeleteTransaction", "Transaction found: " + documentSnapshot.getData());

            if (budgetId != null && !budgetId.isEmpty()) {
                Log.d("DeleteTransaction", "Budget ID found: " + budgetId);

                DocumentReference budgetRef = firestore.collection("users").document(userId)
                        .collection("budgets").document(budgetId);

                budgetRef.get().addOnSuccessListener(budgetSnapshot -> {
                    if (!budgetSnapshot.exists()) {
                        Log.e("DeleteTransaction", "Budget not found in Firestore");
                        return;
                    }

                    double remainingAmount = budgetSnapshot.contains("remainingAmount") ?
                            budgetSnapshot.getDouble("remainingAmount") : 0.0;

                    Log.d("DeleteTransaction", "Current Remaining Amount: " + remainingAmount);

                    // **Fixing Income & Expense logic**
                    if ("Expense".equals(transactionType)) {
                        remainingAmount += amount; // Restore spent amount
                    } else if ("Income".equals(transactionType)) {
                        remainingAmount -= amount; // Remove added income
                    }

                    Log.d("DeleteTransaction", "Updated Remaining Amount: " + remainingAmount);

                    // **Update budget first, then delete transaction**
                    budgetRef.update("remainingAmount", remainingAmount)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("DeleteTransaction", "Budget updated successfully");

                                // Now delete the transaction
                                transactionRef.delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            Log.d("DeleteTransaction", "Transaction deleted successfully");
                                            Toast.makeText(UpdateTransactionActivity.this,
                                                    "Transaction deleted!", Toast.LENGTH_SHORT).show();
                                            finish(); // Close activity
                                        })
                                        .addOnFailureListener(e -> Log.e("DeleteTransaction", "Error deleting transaction", e));
                            })
                            .addOnFailureListener(e -> Log.e("DeleteTransaction", "Error updating budget", e));

                }).addOnFailureListener(e -> Log.e("DeleteTransaction", "Error retrieving budget", e));
            } else {
                // If no budget is linked, just delete the transaction
                Log.d("DeleteTransaction", "No associated budget. Deleting transaction directly.");

                transactionRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d("DeleteTransaction", "Transaction deleted successfully (no budget update needed)");
                            Toast.makeText(UpdateTransactionActivity.this,
                                    "Transaction deleted!", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity
                        })
                        .addOnFailureListener(e -> Log.e("DeleteTransaction", "Error deleting transaction", e));
            }
        }).addOnFailureListener(e -> Log.e("DeleteTransaction", "Error retrieving transaction", e));
    }



    private void loadBudgets() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        firestore.collection("users")
                .document(user.getUid())
                .collection("budgets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    budgetList.clear();
                    budgetMap.clear();

                    // Add hint as first item
                    budgetList.add("Select Budget"); // Better hint
                    budgetMap.put("Select Budget", null);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String budgetId = document.getId();
                        String budgetName = document.getString("name");
                        if (budgetName == null) budgetName = "Unnamed Budget";

                        Double remainingAmount = document.getDouble("remainingAmount");
                        if (remainingAmount == null) {
                            Log.d("FirestoreDebug", "remainingAmount is NULL for budget: " + document.getId());
                            remainingAmount = 0.0;
                        } else {
                            Log.d("FirestoreDebug", "Loaded remainingAmount: " + remainingAmount + " for budget: " + document.getId());
                        }

                        String displayName = budgetName + " (₹" + remainingAmount + " left)";
                        budgetList.add(displayName);
                        budgetMap.put(displayName, budgetId);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, budgetList) {
                        @Override
                        public boolean isEnabled(int position) {
                            // Disable the first item (hint)
                            return position != 0;
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = (TextView) view;

                            if (position == 0) {
                                textView.setTextColor(Color.GRAY);
                            } else {
                                textView.setTextColor(Color.BLACK);
                            }
                            return view;
                        }
                    };

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    budgetSpinner.setAdapter(adapter);
                    budgetSpinner.setSelection(0, false); // Set default selection to hint

                    budgetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                ((TextView) parent.getChildAt(0)).setTextColor(Color.GRAY);
                            } else {
                                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                })
                .addOnFailureListener(e -> Log.e("LoadBudgets", "Error loading budgets", e));
    }


    private String getDisplayNameFromBudgetId(String budgetId) {
        // Loop through the budgetMap to find the matching budgetId
        for (Map.Entry<String, String> entry : budgetMap.entrySet()) {
            if (entry.getValue().equals(budgetId)) {
                return entry.getKey();
            }
        }
        return null;
    }


    private String getBudgetIdFromDisplayName(String budgetName) {
        if (budgetMap == null) {
            Log.e("BudgetError", "getBudgetIdFromDisplayName: budgetMap is NULL!");
            return "";
        }

        if (budgetName == null || budgetName.isEmpty()) {
            Log.e("BudgetError", "getBudgetIdFromDisplayName: Invalid budget name!");
            return "";
        }

        if (!budgetMap.containsKey(budgetName)) {
            Log.e("BudgetError", "getBudgetIdFromDisplayName: BudgetMap does NOT contain key: " + budgetName);
            return "";
        }

        return budgetMap.get(budgetName);
    }


    private void updateBudgetAmount(String userId, String budgetId, double oldAmount, double newAmount,
                                    boolean isNewTransaction, boolean wasExpense, boolean isExpense) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference budgetRef = firestore.collection("users").document(userId)
                .collection("budgets").document(budgetId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot budgetSnapshot = transaction.get(budgetRef);
                    if (!budgetSnapshot.exists()) return null;

                    double remainingAmount = budgetSnapshot.getDouble("remainingAmount") != null ?
                            budgetSnapshot.getDouble("remainingAmount") : 0.0;

                    if (!isNewTransaction) {

                        if (wasExpense) {
                            remainingAmount += oldAmount;
                        } else {
                            remainingAmount -= oldAmount;
                        }
                    }


                    if (isExpense) {
                        remainingAmount -= newAmount;
                    } else {
                        remainingAmount += newAmount;
                    }

                    transaction.update(budgetRef, "remainingAmount", remainingAmount);
                    return null;
                }).addOnSuccessListener(aVoid -> Log.d("BudgetUpdate", "Budget updated successfully"))
                .addOnFailureListener(e -> Log.e("BudgetUpdate", "Error updating budget", e));
    }


    // Load existing transaction details when editing
    private void loadTransactionDetails(String transactionId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("users").document(userId).collection("transactions")
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String description = documentSnapshot.getString("description");
                        double amount = documentSnapshot.getDouble("amount");
                        String date = documentSnapshot.getString("date");
                        String time = documentSnapshot.getString("time");
                        String category = documentSnapshot.getString("category");
                        String transactionType = documentSnapshot.getString("transactionType");
                        String budgetId = documentSnapshot.getString("budgetId");

                        // Set values in UI fields
                        descriptionInput.setText(description);
                        amountInput.setText(String.valueOf(amount));
                        dateText.setText(date);
                        timeText.setText(time);


                        if (transactionType != null) {
                            transactionType = transactionType.trim();
                            Log.d("LoadTransaction", "Fetched transactionType: " + transactionType);

                            if ("Expense".equalsIgnoreCase(transactionType)) {
                                expenseButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8077"))); // Red for Expense
                                incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3"))); // Gray for unselected
                                isIncome = false;
                            } else if ("Income".equalsIgnoreCase(transactionType)) {
                                incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7EF480"))); // Green for Income
                                expenseButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3"))); // Gray for unselected
                                isIncome = true;
                            } else {
                                Log.e("LoadTransaction", "Unexpected transactionType: " + transactionType);
                            }
                        } else {
                            Log.e("LoadTransaction", "Transaction type is NULL");
                        }


                        ArrayAdapter<String> categoryAdapter = (ArrayAdapter<String>) categorySpinner.getAdapter();
                        if (categoryAdapter != null) {
                            int categoryIndex = categoryAdapter.getPosition(category);
                            categorySpinner.setSelection(categoryIndex);
                        }


                        selectedBudgetId = budgetId;


                        if (budgetId != null && budgetMap.containsValue(budgetId)) {
                            for (Map.Entry<String, String> entry : budgetMap.entrySet()) {
                                if (budgetId.equals(entry.getValue())) {
                                    int budgetIndex = budgetList.indexOf(entry.getKey());
                                    if (budgetIndex != -1) {
                                        budgetSpinner.setSelection(budgetIndex);
                                    }
                                    break;
                                }
                            }
                        } else {
                            budgetSpinner.setSelection(0);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("LoadTransaction", "Error fetching transaction details", e));
    }


    private void adjustBudgetOnTransactionUpdate(String userId, String oldBudgetId, String oldTransactionType,
                                                 double oldAmount, String newBudgetId, String newTransactionType,
                                                 double newAmount) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();


        if (!oldBudgetId.equals(newBudgetId)) {
            DocumentReference oldBudgetRef = firestore.collection("users").document(userId)
                    .collection("budgets").document(oldBudgetId);
            DocumentReference newBudgetRef = firestore.collection("users").document(userId)
                    .collection("budgets").document(newBudgetId);

            firestore.runTransaction(transaction -> {
                        DocumentSnapshot oldBudgetSnapshot = transaction.get(oldBudgetRef);
                        DocumentSnapshot newBudgetSnapshot = transaction.get(newBudgetRef);

                        double oldBudgetAmount = oldBudgetSnapshot.exists() ?
                                oldBudgetSnapshot.getDouble("remainingAmount") : 0.0;
                        double newBudgetAmount = newBudgetSnapshot.exists() ?
                                newBudgetSnapshot.getDouble("remainingAmount") : 0.0;


                        if (oldTransactionType.equals("Expense")) {
                            oldBudgetAmount += oldAmount;
                        } else {
                            oldBudgetAmount -= oldAmount;
                        }


                        if (newTransactionType.equals("Expense")) {
                            newBudgetAmount -= newAmount;
                        } else {
                            newBudgetAmount += newAmount;
                        }

                        transaction.update(oldBudgetRef, "remainingAmount", oldBudgetAmount);
                        transaction.update(newBudgetRef, "remainingAmount", newBudgetAmount);
                        return null;
                    }).addOnSuccessListener(aVoid -> Log.d("BudgetAdjust", "Budget adjusted"))
                    .addOnFailureListener(e -> Log.e("BudgetAdjust", "Error adjusting budget", e));
        }
    }


    private void clearFields() {
        descriptionInput.setText("");
        amountInput.setText("");
        dateText.setText("Select Date");
        timeText.setText("Select Time");
        categorySpinner.setSelection(0);
        selectTransactionType("Income");
    }


    private void showDatePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year1, month1, day1) -> {
            String selectedDate = day1 + "/" + (month1 + 1) + "/" + year1;
            dateText.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }


    private void showTimePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour1, minute1) -> {
            String selectedTime = String.format("%02d:%02d", hour1, minute1);
            timeText.setText(selectedTime);
        }, hour, minute, true);
        timePickerDialog.show();
    }


    // Navigation Method
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }
}
