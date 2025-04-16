package com.nastel.expenseplanner.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nastel.expenseplanner.R;
import com.nastel.expenseplanner.models.Budget;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    private List<Budget> budgetList;
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
        void onEditBudget(Budget budget);  // New edit action
        void onDeleteBudget(Budget budget);  // New delete action
    }

    public BudgetAdapter(List<Budget> budgetList, OnBudgetClickListener listener) {
        this.budgetList = budgetList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        // Convert budget name to Title Case
        String titleCaseName = budget.getName().substring(0, 1).toUpperCase() + budget.getName().substring(1).toLowerCase();
        holder.budgetName.setText(titleCaseName);

        // Make the amount bold and increase text size
        holder.budgetAmount.setText("₹" + budget.getAmount());
        holder.budgetAmount.setTypeface(null, Typeface.BOLD);
        holder.budgetAmount.setTextSize(16);

        // Display start & end dates properly
        holder.startDate.setText("Start: " + (budget.getStartDate().isEmpty() ? "N/A" : budget.getStartDate()));
        holder.endDate.setText("End: " + (budget.getEndDate().isEmpty() ? "N/A" : budget.getEndDate()));

        // Click listener for opening budget details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBudgetClick(budget);
            }
        });

        // Long click for Edit/Delete options
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEditBudget(budget);  // Placeholder for edit action
                listener.onDeleteBudget(budget);  // Placeholder for delete action
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView budgetName, budgetAmount, startDate, endDate;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            budgetName = itemView.findViewById(R.id.budget_name);
            budgetAmount = itemView.findViewById(R.id.budget_amount);
            startDate = itemView.findViewById(R.id.budget_start_date);
            endDate = itemView.findViewById(R.id.budget_end_date);
        }
    }
}
