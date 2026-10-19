package com.example.gymsync;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView tvName, tvEmail, tvAge, tvHeight, tvWeight;
    TextView tvGoalBadge, tvBmiCategory, tvBmiValue, tvBmiExplanation, tvProfileWorkoutsCount, tvProfileVisitsCount;
    Button btBackImage, btEditProfileBig;
    ImageButton btBack, btEditProfile;
    DBHelper dbHelper;
    SessionManager sessionManager;
    String userEmail;

    // Cached profile values
    private String currentName = "Athlete";
    private int currentAge = 22;
    private double currentHeight = 175.0;
    private double currentWeight = 70.0;
    private String currentGoal = "General Fitness";
    private String currentPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = sessionManager.getUserEmail();
        }

        initViews();
        setupListeners();
        loadProfileData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvAge = findViewById(R.id.tvAge);
        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);

        tvGoalBadge = findViewById(R.id.tvGoalBadge);
        tvBmiCategory = findViewById(R.id.tvBmiCategory);
        tvBmiValue = findViewById(R.id.tvBmiValue);
        tvBmiExplanation = findViewById(R.id.tvBmiExplanation);
        tvProfileWorkoutsCount = findViewById(R.id.tvProfileWorkoutsCount);
        tvProfileVisitsCount = findViewById(R.id.tvProfileVisitsCount);

        btBack = findViewById(R.id.btBack);
        btEditProfile = findViewById(R.id.btEditProfile);
        btEditProfileBig = findViewById(R.id.btEditProfileBig);
    }

    private void setupListeners() {
        if (btBack != null) btBack.setOnClickListener(v -> finish());
        if (btEditProfile != null) btEditProfile.setOnClickListener(v -> showEditProfileDialog());
        if (btEditProfileBig != null) btEditProfileBig.setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadProfileData() {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = dbHelper.getUserDetails(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            currentName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            currentAge = cursor.getInt(cursor.getColumnIndexOrThrow("age"));
            currentHeight = cursor.getDouble(cursor.getColumnIndexOrThrow("height"));
            currentWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));

            try {
                currentGoal = cursor.getString(cursor.getColumnIndexOrThrow("goal"));
                if (currentGoal == null || currentGoal.isEmpty()) currentGoal = "General Fitness";
            } catch (Exception e) {
                currentGoal = "General Fitness";
            }

            try {
                currentPhone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                if (currentPhone == null) currentPhone = "";
            } catch (Exception e) {
                currentPhone = "";
            }

            tvName.setText("Name: " + currentName);
            tvEmail.setText("Email: " + email);
            tvAge.setText("Age: " + currentAge + " yrs");
            tvHeight.setText("Height: " + currentHeight + " cm");
            tvWeight.setText("Weight: " + currentWeight + " kg");
            tvGoalBadge.setText("Goal: " + currentGoal);

            updateBmiDisplay(currentHeight, currentWeight);
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            Toast.makeText(this, "Profile record not found", Toast.LENGTH_SHORT).show();
        }

        // Lifetime stats
        int totalWorkouts = dbHelper.getTotalCompletedCount(userEmail);
        int totalVisits = dbHelper.getUserBookingsCount(userEmail);

        if (tvProfileWorkoutsCount != null) {
            tvProfileWorkoutsCount.setText(String.valueOf(totalWorkouts));
        }
        if (tvProfileVisitsCount != null) {
            tvProfileVisitsCount.setText(String.valueOf(totalVisits));
        }
    }

    private void updateBmiDisplay(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) return;

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        tvBmiValue.setText(String.format(Locale.getDefault(), "BMI: %.1f", bmi));

        if (bmi < 18.5) {
            tvBmiCategory.setText("Underweight");
            tvBmiCategory.setBackgroundResource(R.drawable.bg_badge_yellow);
            tvBmiCategory.setTextColor(ContextCompat.getColor(this, R.color.status_yellow));
            tvBmiExplanation.setText("Below standard body weight. Consider strength training with a caloric surplus.");
        } else if (bmi < 25.0) {
            tvBmiCategory.setText("Normal Weight");
            tvBmiCategory.setBackgroundResource(R.drawable.bg_badge_green);
            tvBmiCategory.setTextColor(ContextCompat.getColor(this, R.color.status_green));
            tvBmiExplanation.setText("Optimal body mass ratio. Maintain active training routine and balanced macros.");
        } else if (bmi < 30.0) {
            tvBmiCategory.setText("Overweight");
            tvBmiCategory.setBackgroundResource(R.drawable.bg_badge_yellow);
            tvBmiCategory.setTextColor(ContextCompat.getColor(this, R.color.status_yellow));
            tvBmiExplanation.setText("Slightly elevated body mass. Combine progressive overload with cardio circuits.");
        } else {
            tvBmiCategory.setText("High BMI");
            tvBmiCategory.setBackgroundResource(R.drawable.bg_badge_red);
            tvBmiCategory.setTextColor(ContextCompat.getColor(this, R.color.status_red));
            tvBmiExplanation.setText("High mass index. Focus on guided nutrition and low-impact cardiovascular conditioning.");
        }
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etAge = dialogView.findViewById(R.id.etEditAge);
        EditText etHeight = dialogView.findViewById(R.id.etEditHeight);
        EditText etWeight = dialogView.findViewById(R.id.etEditWeight);
        EditText etGoal = dialogView.findViewById(R.id.etEditGoal);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);

        etName.setText(currentName);
        etAge.setText(String.valueOf(currentAge));
        etHeight.setText(String.valueOf(currentHeight));
        etWeight.setText(String.valueOf(currentWeight));
        etGoal.setText(currentGoal);
        etPhone.setText(currentPhone);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save Changes", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String ageStr = etAge.getText().toString().trim();
                    String heightStr = etHeight.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String goal = etGoal.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(ProfileActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int age = Integer.parseInt(ageStr);
                        double height = Double.parseDouble(heightStr);
                        double weight = Double.parseDouble(weightStr);

                        if (age < 10 || age > 110) {
                            Toast.makeText(ProfileActivity.this, "Enter valid age", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (height < 50 || height > 260) {
                            Toast.makeText(ProfileActivity.this, "Enter valid height in cm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (weight < 25 || weight > 350) {
                            Toast.makeText(ProfileActivity.this, "Enter valid weight in kg", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean updated = dbHelper.updateUserProfile(userEmail, name, age, height, weight, goal, phone);
                        if (updated) {
                            sessionManager.updateUserName(name);
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            loadProfileData();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }

                    } catch (NumberFormatException e) {
                        Toast.makeText(ProfileActivity.this, "Please enter numeric values for age, height and weight", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
