package com.nastel.expenseplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactionList;
    private OnTransactionClickListener listener;

    public TransactionAdapter(List<Transaction> transactionList, OnTransactionClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.categoryTextView.setText(transaction.getCategory());
        holder.amountTextView.setText(String.format("₹%.2f", transaction.getAmount())); // ₹ symbol
        holder.dateTextView.setText(transaction.getDate());
        holder.descriptionTextView.setText(transaction.getDescription());


        if (transaction.getType().equalsIgnoreCase("income")) {
            holder.amountTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
            holder.arrowTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
            holder.transactionIcon.setImageResource(R.drawable.arrowup);
        } else {
            holder.amountTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
            holder.arrowTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
            holder.transactionIcon.setImageResource(R.drawable.arrowdown);
        }

        holder.itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, amountTextView, dateTextView, descriptionTextView, arrowTextView;
        ImageView transactionIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.category_text);
            amountTextView = itemView.findViewById(R.id.amount_text);
            dateTextView = itemView.findViewById(R.id.date_text);
            descriptionTextView = itemView.findViewById(R.id.description_text);
            transactionIcon = itemView.findViewById(R.id.transaction_icon);
            arrowTextView = itemView.findViewById(R.id.arrow_text);
        }
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }
}
