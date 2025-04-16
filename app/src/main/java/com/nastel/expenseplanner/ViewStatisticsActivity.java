package com.nastel.expenseplanner;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ViewStatisticsActivity extends AppCompatActivity {
    private LinearLayout navHome, navSettings, navTransaction;
    private static final String TAG = "ViewStatisticsActivity";

    private FirebaseFirestore firestore;
    private BarChart barChart;
    private PieChart pieChart;

    // Declare these as global variables
    private ArrayList<BarEntry> barEntries = new ArrayList<>();
    private ArrayList<PieEntry> pieEntries = new ArrayList<>();
    private ArrayList<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_statistics);

        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);

        firestore = FirebaseFirestore.getInstance();
        barChart = findViewById(R.id.home_bar_chart);
        pieChart = findViewById(R.id.home_pie_chart);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserTransactions(currentUser.getUid());
        } else {
            Log.e(TAG, "No user signed in.");
        }

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        navTransaction.setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionActivity.class));
            finish();
        });
        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });
    }

    private void loadUserTransactions(String userId) {
        CollectionReference transactionsRef = firestore.collection("users")
                .document(userId).collection("transactions");

        transactionsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                barEntries.clear();
                pieEntries.clear();
                categoryNames.clear();

                Map<String, Float> categoryTotals = new HashMap<>();
                int index = 0;

                for (QueryDocumentSnapshot document : task.getResult()) {
                    if (document.contains("amount") && document.contains("category")) {
                        float amount = Float.parseFloat(document.get("amount").toString());
                        String category = document.getString("category");

                        categoryTotals.put(category, categoryTotals.getOrDefault(category, 0f) + amount);
                    }
                }

                for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
                    barEntries.add(new BarEntry(index, entry.getValue()));
                    pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    categoryNames.add(entry.getKey());
                    index++;
                }

                applyChartUpdates();
            } else {
                Log.e(TAG, "Error fetching transactions: ", task.getException());
            }
        });
    }


    private void updateBarChart(ArrayList<BarEntry> barEntries, ArrayList<String> categoryNames, ArrayList<Integer> colorsList) {
        BarDataSet barDataSet = new BarDataSet(barEntries, "Category-wise Transactions");


        barDataSet.setColors(colorsList);
        barDataSet.setValueTextSize(14f);
        barDataSet.setValueTextColor(Color.WHITE);
        barDataSet.setValueFormatter(new PercentFormatter());

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);

        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(800);
        barChart.setDrawGridBackground(false);


        XAxis xAxis = barChart.getXAxis();
        xAxis.setEnabled(false);
        xAxis.setDrawGridLines(false);


        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setEnabled(false);
        barChart.getAxisRight().setEnabled(false);


        Legend legend = barChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(12f);

        barChart.invalidate();
    }

    private void updatePieChart(ArrayList<PieEntry> pieEntries, ArrayList<Integer> colorsList) {
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");


        pieDataSet.setColors(colorsList);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueFormatter(new PercentFormatter()); // Display percentage inside pie chart

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setDrawEntryLabels(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.getDescription().setEnabled(false);
        pieChart.animateY(800);


        Legend legend = pieChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(12f);

        pieChart.invalidate();
    }


    private void applyChartUpdates() {
        // Generate random colors for categories
        List<Integer> randomColors = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < pieEntries.size(); i++) {
            randomColors.add(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        }

        // Configure Bar Chart
        BarDataSet barDataSet = new BarDataSet(barEntries, "Category-wise Transactions");
        barDataSet.setColors(randomColors);
        barDataSet.setValueTextSize(14f);
        barDataSet.setValueTextColor(Color.BLACK); // Ensuring visibility
        barDataSet.setValueTypeface(Typeface.DEFAULT_BOLD); // Make values bold

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setDrawLabels(false);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getXAxis().setDrawGridLines(true);
        barChart.getLegend().setXEntrySpace(12f);
        barChart.invalidate();

        // Configure Pie Chart
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(randomColors);
        pieDataSet.setValueTextSize(14f);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        pieDataSet.setValueFormatter(new PercentFormatter(pieChart));
        pieDataSet.setDrawValues(true);
        pieDataSet.setDrawIcons(false);

        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);

        // Adjust Legend Size to Fit 8 Categories Properly
        Legend legend = pieChart.getLegend();
        legend.setFormSize(8f);  // Reduced icon size
        legend.setTextSize(10f); // Reduced text size for better spacing
        legend.setXEntrySpace(6f);
        legend.setYEntrySpace(4f);
        legend.setWordWrapEnabled(true);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setDirection(Legend.LegendDirection.LEFT_TO_RIGHT);
        legend.setDrawInside(false);

        pieChart.invalidate();
    }


}
