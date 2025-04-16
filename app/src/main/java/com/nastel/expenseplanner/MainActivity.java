package com.nastel.expenseplanner;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView welcomeText;
    private ImageView accountIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Button mainBudgetButton;
    private BarChart barChart;
    private PieChart pieChart;
    private ListenerRegistration usernameListener;
    private String selectedBudgetId = null;
    private ArrayList<String> budgetNames = new ArrayList<>();
    private ArrayList<String> budgetIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        welcomeText = findViewById(R.id.welcome_text);
        accountIcon = findViewById(R.id.account_icon);
        mainBudgetButton = findViewById(R.id.main_budget_button);
        barChart = findViewById(R.id.home_bar_chart);
        pieChart = findViewById(R.id.home_pie_chart);
        LinearLayout settingsButton = findViewById(R.id.nav_settings);
        LinearLayout transactionButton = findViewById(R.id.nav_transaction);

        Button selectBudgetButton = findViewById(R.id.budget_select_button);
        selectBudgetButton.setOnClickListener(v -> showBudgetSelectionDialog());

        settingsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        mainBudgetButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MainBudgetActivity.class)));
        transactionButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TransactionActivity.class)));

        loadUserData();
    }


    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
            loadExpensesForSelectedBudget();
            listenForUserNameChanges(user.getUid());
        } else {
            Log.e(TAG, "No user is logged in.");
        }
    }



    private void updateUI(FirebaseUser user) {
        if (user != null) {
            welcomeText.setText("Hi, " + user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(accountIcon);
            } else {
                accountIcon.setImageResource(R.drawable.accountlogo);
            }
        } else {
            Log.e(TAG, "User is null.");
        }
    }


    private void listenForUserNameChanges(String userId) {
        DocumentReference userRef = firestore.collection("users").document(userId);
        usernameListener = userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening for username changes", e);
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String updatedUsername = documentSnapshot.getString("username");
                if (updatedUsername != null) {
                    welcomeText.setText("Hi, " + updatedUsername);
                }
            }
        });
    }


    private void loadExpensesForSelectedBudget() {
        if (selectedBudgetId != null) {
            firestore.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .collection("transactions")
                    .whereEqualTo("budgetId", selectedBudgetId)  // Only fetch transactions for the selected budget
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            HashMap<String, Float> categoryExpenses = new HashMap<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String category = document.getString("category");
                                Float amount = document.getDouble("amount") != null ? document.getDouble("amount").floatValue() : 0f;

                                if (category != null) {
                                    categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0f) + amount);
                                }
                            }

                            // Update only the bar chart for this selected budget
                            updateBarChartForSelectedBudget(categoryExpenses);
                        } else {
                            Log.e(TAG, "Error fetching filtered transactions: ", task.getException());
                        }
                    });
        }
    }


    private void showBudgetSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Budget");

        firestore.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("budgets")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();

                        if (documents.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No budgets available.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] budgetItems = new String[documents.size()];
                        String[] budgetIds = new String[documents.size()];

                        for (int i = 0; i < documents.size(); i++) {
                            DocumentSnapshot doc = documents.get(i);
                            String budgetName = doc.getString("name");
                            if (budgetName == null) budgetName = "Unnamed Budget";

                            // Fetch remainingAmount directly from Firestore
                            Double remainingAmount = doc.getDouble("remainingAmount");
                            if (remainingAmount == null) {
                                Log.d("FirestoreDebug", "remainingAmount is NULL for budget: " + doc.getId());
                                remainingAmount = 0.0; // Default to 0 if missing
                            } else {
                                Log.d("FirestoreDebug", "Loaded remainingAmount: " + remainingAmount + " for budget: " + doc.getId());
                            }

                            budgetItems[i] = budgetName + " (₹" + remainingAmount + ")";
                            budgetIds[i] = doc.getId();
                        }

                        builder.setItems(budgetItems, (dialog, which) -> {
                            selectedBudgetId = budgetIds[which];
                            loadAndUpdateChartDataForSelectedBudget();
                        });

                        builder.show();
                    } else {
                        Log.e("FirestoreDebug", "Error fetching budgets: ", task.getException());
                    }
                });
    }


    private void updatePieChartForTotalExpenses(float totalSpent) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(totalSpent, "Total Spent"));
        entries.add(new PieEntry(1, "Remaining")); // Placeholder for now

        PieDataSet dataSet = new PieDataSet(entries, "Spending Overview");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }


    private void loadAndUpdateChartDataForSelectedBudget() {
        if (selectedBudgetId != null) {
            firestore.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .collection("budgets")
                    .document(selectedBudgetId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                String budgetName = documentSnapshot.getString("name");
                                double allocatedAmount = documentSnapshot.getDouble("allocated") != null ? documentSnapshot.getDouble("allocated") : 0;
                                double usedAmount = documentSnapshot.contains("used") && documentSnapshot.getDouble("used") != null ? documentSnapshot.getDouble("used") : 0;


                                updatePieChartForSelectedBudget((float) allocatedAmount);


                                loadExpensesForSelectedBudget();
                            }
                        } else {
                            Log.e(TAG, "Error fetching selected budget data: ", task.getException());
                        }
                    });
        }
    }




    //Updating Bar and Pie chart for selected Budget
    private void updateBarChartForSelectedBudget(HashMap<String, Float> categoryExpenses) {
        try {
            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<String> categories = new ArrayList<>();

            int index = 0;
            for (Map.Entry<String, Float> entry : categoryExpenses.entrySet()) {
                Log.d("BarChartDebug", "Category: " + entry.getKey() + ", Value: " + entry.getValue());
                barEntries.add(new BarEntry(index, entry.getValue()));
                categories.add(entry.getKey());
                index++;
            }

            if (barEntries.isEmpty()) {
                Log.w("BarChartDebug", "No data available for the bar chart.");
                barChart.clear();
                return;
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "Transactions by Category");
            barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            barDataSet.setValueTextSize(12f);

            BarData barData = new BarData(barDataSet);
            barChart.setData(barData);
            barChart.getDescription().setEnabled(false);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(categories));
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(categories.size());


            if (categories.size() > 4) {
                barChart.setVisibleXRangeMaximum(4);
                barChart.setDragEnabled(true);
                barChart.setScaleXEnabled(true);
            } else {
                barChart.setDragEnabled(false);
                barChart.setScaleXEnabled(false);
            }

            barChart.invalidate();
        } catch (Exception e) {
            Log.e("BarChartDebug", "Error updating bar chart: " + e.getMessage());
        }
    }


    private void updatePieChartForSelectedBudget(float allocated) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ArrayList<PieEntry> entries = new ArrayList<>();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("transactions")
                .whereEqualTo("budgetId", selectedBudgetId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    float totalSpent = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double amount = doc.getDouble("amount");
                        String type = doc.getString("type");

                        if (amount != null && type != null && type.equalsIgnoreCase("expense")) {
                            totalSpent += amount;
                        }
                    }

                    float remaining = allocated - totalSpent;
                    remaining = Math.max(0, remaining); // Prevent negative values

                    Log.d("PieChartDebug", "Allocated: " + allocated + ", Spent: " + totalSpent + ", Remaining: " + remaining);

                    // Add "Spent" and "Remaining" to PieChart
                    if (totalSpent > 0) {
                        entries.add(new PieEntry(totalSpent, "₹" + totalSpent + "\nSpent"));
                    }
                    if (remaining > 0) {
                        entries.add(new PieEntry(remaining, "₹" + remaining + "\nRemaining"));
                    }

                    if (entries.isEmpty()) {
                        pieChart.clear();
                        pieChart.invalidate();
                        return;
                    }

                    // Set Colors
                    ArrayList<Integer> colors = new ArrayList<>();
                    colors.add(Color.RED);
                    colors.add(Color.GREEN);

                    PieDataSet dataSet = new PieDataSet(entries, "");
                    dataSet.setColors(colors);
                    dataSet.setValueTextSize(18f);
                    dataSet.setValueTextColor(Color.WHITE);
                    dataSet.setSliceSpace(3f);
                    dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

                    PieData data = new PieData(dataSet);
                    pieChart.setData(data);
                    pieChart.setUsePercentValues(false);
                    pieChart.setDrawHoleEnabled(true);
                    pieChart.setHoleRadius(40f);
                    pieChart.setTransparentCircleRadius(45f);
                    pieChart.setHoleColor(Color.WHITE);
                    pieChart.getDescription().setEnabled(false);
                    pieChart.setEntryLabelColor(Color.BLACK);
                    pieChart.setEntryLabelTextSize(14f);
                    pieChart.setCenterText(""); //

                    // Legend settings
                    Legend legend = pieChart.getLegend();
                    legend.setEnabled(true);
                    legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                    legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                    legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
                    legend.setTextColor(Color.BLACK);
                    legend.setTextSize(14f);
                    legend.setWordWrapEnabled(true);

                    pieChart.invalidate();
                })
                .addOnFailureListener(e -> Log.e("PieChartDebug", "Error fetching transactions", e));
    }

    @Override
    protected void onDestroy() {
        if (usernameListener != null) {
            usernameListener.remove();
        }
        super.onDestroy();
    }
}
