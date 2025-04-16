package com.nastel.expenseplanner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddTransactionActivity extends AppCompatActivity {

    private TextView dateText, timeText;
    private Button incomeButton, expenseButton, addTransactionButton;
    private LinearLayout navHome, navTransaction, navSettings;
    private EditText descriptionInput, amountInput; // Added amountInput
    private Spinner categorySpinner;
    private List<String> categories;
    private ArrayAdapter<String> categoryAdapter;
    private String budgetId; // Add this in your Transaction model

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String transactionType = "Income"; // Default type

    private static final String PREFS_NAME = "ExpensePlannerPrefs";
    private static final String CATEGORIES_KEY = "categories";

    private Spinner budgetSpinner;
    private List<String> budgetList = new ArrayList<>();
    private Map<String, String> budgetMap = new HashMap<>(); // Store budget names & IDs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Bottom navigation buttons
        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        // Initialize Firestore and FirebaseAuth
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize views
        LinearLayout datePickerLayout = findViewById(R.id.date_picker);
        LinearLayout timePickerLayout = findViewById(R.id.time_picker);
        dateText = findViewById(R.id.date_text);
        timeText = findViewById(R.id.time_text);
        incomeButton = findViewById(R.id.income_button);
        expenseButton = findViewById(R.id.expense_button);
        addTransactionButton = findViewById(R.id.add_transaction_button);
        descriptionInput = findViewById(R.id.description_input);
        amountInput = findViewById(R.id.amount_input); // Initialize amountInput
        categorySpinner = findViewById(R.id.category_spinner);

        // category dropdown
        setupCategoryDropdown();

        // button listeners for income and expense selection
        incomeButton.setOnClickListener(v -> selectTransactionType("Income"));
        expenseButton.setOnClickListener(v -> selectTransactionType("Expense"));
        datePickerLayout.setOnClickListener(this::showDatePicker);
        timePickerLayout.setOnClickListener(this::showTimePicker);

        // Listener for add transaction button
        addTransactionButton.setOnClickListener(v -> addTransaction());

        // Date and time picker click listeners
        dateText.setOnClickListener(this::showDatePicker);
        timeText.setOnClickListener(this::showTimePicker);

        budgetSpinner = findViewById(R.id.budget_spinner);
        loadBudgets();

        // Navigation buttons
        navHome.setOnClickListener(v -> navigateTo(MainActivity.class));
        navTransaction.setOnClickListener(v -> navigateTo(TransactionActivity.class));
        navSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class));

    }

    private void setupCategoryDropdown() {
        categories = loadCategories();
        if (categories.isEmpty()) {
            categories = new ArrayList<>(Arrays.asList(
                    "Food", "Transport", "Entertainment", "Groceries", "Bills & Fees", "Work", "Gifts", "Petrol"
            ));
        }


        categories.add(0, "Select Category");

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;


                if (position == 0) {
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };

        categorySpinner.setAdapter(categoryAdapter);


        categorySpinner.setSelection(0, false);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {

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

        if (type.equals("Income")) {
            incomeButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            expenseButton.setBackgroundColor(Color.LTGRAY);
        } else {
            expenseButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            incomeButton.setBackgroundColor(Color.LTGRAY);
        }
    }

    private void addTransaction() {
        String description = descriptionInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String date = dateText.getText().toString().trim();
        String time = timeText.getText().toString().trim();
        String selectedBudget = budgetSpinner.getSelectedItem().toString();

        if (description.isEmpty() || amountStr.isEmpty() || category.equals("Select Category") ||
                date.equals("Select Date") || time.equals("Select Time")) {
            Toast.makeText(this, "Please fill in all the fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        budgetId = budgetMap.containsKey(selectedBudget) ? budgetMap.get(selectedBudget) : "No Budget";

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("description", description);
        transactionData.put("amount", amount);
        transactionData.put("category", category);
        transactionData.put("date", date);
        transactionData.put("time", time);
        transactionData.put("type", transactionType);
        transactionData.put("timestamp", System.currentTimeMillis());
        transactionData.put("budgetId", budgetId);

        if (budgetId != null && !budgetId.equals("No Budget")) {
            DocumentReference budgetRef = firestore.collection("users")
                    .document(userId)
                    .collection("budgets")
                    .document(budgetId);

            budgetRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object budgetValue = documentSnapshot.get("remainingAmount");

                    if (budgetValue == null) {
                        Toast.makeText(this, "Budget value missing!", Toast.LENGTH_SHORT).show();
                        Log.e("BudgetUpdate", "remainingAmount field is missing in Firestore");
                        return;
                    }

                    double currentBudget;
                    try {
                        currentBudget = Double.parseDouble(budgetValue.toString());
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid budget value!", Toast.LENGTH_SHORT).show();
                        Log.e("BudgetUpdate", "Invalid remainingAmount type: " + budgetValue.getClass().getSimpleName(), e);
                        return;
                    }

                    double updatedBudget;
                    if (transactionType.equals("Expense")) {
                        if (currentBudget < amount) {
                            Toast.makeText(this, "Insufficient budget!", Toast.LENGTH_SHORT).show();
                            Log.e("BudgetUpdate", "Transaction amount exceeds available budget.");
                            return;
                        }
                        updatedBudget = currentBudget - amount;  // Reduce budget for Expense
                    } else {
                        updatedBudget = currentBudget + amount;  // Increase budget for Income
                    }

                    budgetRef.update("remainingAmount", updatedBudget)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Budget updated successfully!", Toast.LENGTH_SHORT).show();
                                Log.d("BudgetUpdate", "Budget updated: " + updatedBudget);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update budget!", Toast.LENGTH_SHORT).show();
                                Log.e("BudgetUpdate", "Error updating budget: " + e.getMessage(), e);
                            });

                } else {
                    Toast.makeText(this, "Budget not found!", Toast.LENGTH_SHORT).show();
                    Log.e("BudgetUpdate", "Budget document does not exist in Firestore");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to fetch budget!", Toast.LENGTH_SHORT).show();
                Log.e("BudgetUpdate", "Error fetching budget document: " + e.getMessage(), e);
            });
        }

        firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Transaction Added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error adding transaction", Toast.LENGTH_SHORT).show());
    }

    private void updateBudgetBalance(String budgetId, double amount, String transactionType) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e("BudgetUpdate", "User not signed in!");
            return;
        }

        String userId = user.getUid();
        DocumentReference budgetRef = firestore.collection("users")
                .document(userId)
                .collection("budgets")
                .document(budgetId);

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Object budgetValue = documentSnapshot.get("remainingAmount");

                if (budgetValue == null) {
                    Log.e("BudgetUpdate", "remainingAmount field is missing in Firestore");
                    return;
                }

                double currentBudget;
                try {
                    currentBudget = Double.parseDouble(budgetValue.toString());
                } catch (Exception e) {
                    Log.e("BudgetUpdate", "Invalid remainingAmount type: " + budgetValue.getClass().getSimpleName(), e);
                    return;
                }


                double newBudget;
                if (transactionType.equals("Expense")) {
                    newBudget = currentBudget - amount;  // Subtract for expenses
                } else {
                    newBudget = currentBudget + amount;  // Add for income
                }


                budgetRef.update("remainingAmount", newBudget)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("BudgetUpdate", "Budget updated successfully! New Balance: " + newBudget);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("BudgetUpdate", "Failed to update budget: " + e.getMessage(), e);
                        });

            } else {
                Log.e("BudgetUpdate", "Budget document does not exist in Firestore");
            }
        }).addOnFailureListener(e -> {
            Log.e("BudgetUpdate", "Error fetching budget document: " + e.getMessage(), e);
        });
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
                    budgetList.add("No Budget"); // Better hint
                    budgetMap.put("No Budget", null);

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
                                textView.setTextColor(Color.GRAY); // Hint color
                            } else {
                                textView.setTextColor(Color.BLACK); // Normal selection
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

    private void redirectToHistory() {
        Log.d("Transaction", "Redirecting to history..."); // Debug log to ensure it's being called
        Intent intent = new Intent(AddTransactionActivity.this, TransactionHistoryActivity.class);
        startActivity(intent);
        finish();
    }


    private void deductFromBudget(String userId, String budgetId, double amount) {
        DocumentReference budgetRef = firestore.collection("users")
                .document(userId)
                .collection("budgets")
                .document(budgetId);

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                double remainingAmount = documentSnapshot.getDouble("remainingAmount");

                if (remainingAmount >= amount) {
                    budgetRef.update("remainingAmount", remainingAmount - amount)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Transaction added and budget updated!", Toast.LENGTH_SHORT).show();
                                clearFields();
                                redirectToHistory();
                            })
                            .addOnFailureListener(e -> Log.e("DeductBudget", "Error updating budget", e));
                } else {
                    Toast.makeText(this, "Not enough budget remaining!", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> Log.e("DeductBudget", "Error fetching budget", e));
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