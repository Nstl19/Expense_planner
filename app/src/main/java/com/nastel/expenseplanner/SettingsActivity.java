package com.nastel.expenseplanner;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private TextView usernameText, userEmailText;
    private ImageView profileImage;
    private FirebaseAuth auth;
    private Button signOutButton;
    private FirebaseFirestore db;
    private LinearLayout navHome, navSettings, navTransaction;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        usernameText = findViewById(R.id.username_text);
        userEmailText = findViewById(R.id.user_email_text);
        profileImage = findViewById(R.id.profile_image);
        navHome = findViewById(R.id.nav_home);
        navSettings = findViewById(R.id.nav_settings);
        navTransaction = findViewById(R.id.nav_transaction);
        Button changeUsernameButton = findViewById(R.id.change_username_button);
        Button viewTransactionHistoryButton = findViewById(R.id.view_transaction_history_button);
        Button logoutButton = findViewById(R.id.logout_button);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // Display user details if logged in
        if (user != null) {
            userEmailText.setText(user.getEmail() != null ? user.getEmail() : "No email provided");

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.accountlogo) // Default profile placeholder
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.accountlogo);
            }

            // Fetch the latest username from Firestore
            fetchUsernameFromDatabase(user.getUid());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Change Username Button Click
        changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());

        // View Transaction History Button
        viewTransactionHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });

        // Logout Button Click
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Navigation to Home
        navHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(homeIntent);
            finish();
        });

        // Navigation to Transaction
        navTransaction.setOnClickListener(v -> {
            Intent transactionIntent = new Intent(SettingsActivity.this, TransactionActivity.class);
            startActivity(transactionIntent);
            finish();
        });

        // Navigation to Settings (refreshes the current page)
        navSettings.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            finish();
        });
    }

    // Function to show Change Username Dialog
    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter new username");


        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);


        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                updateUsernameInDatabase(newUsername);
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });


        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Function to update username in Firebase Firestore
    private void updateUsernameInDatabase(String newUsername) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.update("username", newUsername)
                    .addOnSuccessListener(aVoid -> {
                        // Fetch the updated username again from Firestore
                        fetchUsernameFromDatabase(userId);
                        Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show());
        }
    }

    // Function to fetch username from Firebase Firestore
    private void fetchUsernameFromDatabase(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("username")) {
                String username = documentSnapshot.getString("username");
                usernameText.setText(username);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch username", Toast.LENGTH_SHORT).show());
    }
}