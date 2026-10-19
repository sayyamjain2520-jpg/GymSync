package com.example.gymsync;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    Button btFindGym, btWorkout, btProfile;
    ImageButton btLogout;
    TextView tvGreetingName, tvStatWorkouts, tvStatBmi, tvStatBmiLabel, tvStatVisits;

    String userEmail;
    DBHelper dbHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        dbHelper = new DBHelper(this);

        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = sessionManager.getUserEmail();
        }

        btFindGym = findViewById(R.id.btFindGym);
        btWorkout = findViewById(R.id.btWorkout);
        btProfile = findViewById(R.id.btProfile);
        btLogout = findViewById(R.id.btLogout);

        tvGreetingName = findViewById(R.id.tvGreetingName);
        tvStatWorkouts = findViewById(R.id.tvStatWorkouts);
        tvStatBmi = findViewById(R.id.tvStatBmi);
        tvStatBmiLabel = findViewById(R.id.tvStatBmiLabel);
        tvStatVisits = findViewById(R.id.tvStatVisits);

        btFindGym.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, NearbyGymsActivity.class);
                intent.putExtra("user_email", userEmail);
                startActivity(intent);
            }
        });

        btWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, WorkoutActivity.class);
                intent.putExtra("user_email", userEmail);
                startActivity(intent);
            }
        });

        btProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                intent.putExtra("user_email", userEmail);
                startActivity(intent);
            }
        });

        if (btLogout != null) {
            btLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLogoutDialog();
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        // Load user name and BMI
        Cursor cursor = dbHelper.getUserDetails(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            double height = cursor.getDouble(cursor.getColumnIndexOrThrow("height"));
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));

            if (tvGreetingName != null) {
                tvGreetingName.setText("Hello, " + name + "!");
            }

            // Calculate BMI: weight (kg) / [height (m)]^2
            if (height > 0 && weight > 0) {
                double heightM = height / 100.0;
                double bmi = weight / (heightM * heightM);
                if (tvStatBmi != null) {
                    tvStatBmi.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                }
                if (tvStatBmiLabel != null) {
                    if (bmi < 18.5) {
                        tvStatBmiLabel.setText("BMI (Underweight)");
                    } else if (bmi < 25.0) {
                        tvStatBmiLabel.setText("BMI (Normal)");
                    } else if (bmi < 30.0) {
                        tvStatBmiLabel.setText("BMI (Overweight)");
                    } else {
                        tvStatBmiLabel.setText("BMI (Obese)");
                    }
                }
            }
            cursor.close();
        }

        // Load workout counts
        int workoutsLogged = dbHelper.getTotalCompletedCount(userEmail);
        if (tvStatWorkouts != null) {
            tvStatWorkouts.setText(String.valueOf(workoutsLogged));
        }

        // Load gym visit bookings
        int visitsCount = dbHelper.getUserBookingsCount(userEmail);
        if (tvStatVisits != null) {
            tvStatVisits.setText(String.valueOf(visitsCount));
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of GymSync?")
                .setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.logoutUser();
                        Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}