package com.example.gymsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {

    EditText et1, et2, et3, et4, et5, et6, et7;
    Button bt1;
    ImageButton btBack;
    TextView tvBackToLogin;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        et1 = findViewById(R.id.et1); // Name
        et2 = findViewById(R.id.et2); // Email
        et3 = findViewById(R.id.et3); // Password
        et4 = findViewById(R.id.et4); // Confirm Password
        et5 = findViewById(R.id.et5); // Age
        et6 = findViewById(R.id.et6); // Height
        et7 = findViewById(R.id.et7); // Weight

        bt1 = findViewById(R.id.bt1);
        btBack = findViewById(R.id.btBack);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        if (btBack != null) {
            btBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        dbHelper = new DBHelper(this);

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = et1.getText().toString().trim();
                String email = et2.getText().toString().trim().toLowerCase();
                String password = et3.getText().toString();
                String confirmPassword = et4.getText().toString();
                String ageText = et5.getText().toString().trim();
                String heightText = et6.getText().toString().trim();
                String weightText = et7.getText().toString().trim();

                if (name.isEmpty()) {
                    et1.setError("Enter your full name");
                    et1.requestFocus();
                    return;
                }

                if (email.isEmpty()) {
                    et2.setError("Enter your email address");
                    et2.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    et2.setError("Enter a valid email address");
                    et2.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    et3.setError("Password must have at least 6 characters");
                    et3.requestFocus();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    et4.setError("Passwords do not match");
                    et4.requestFocus();
                    return;
                }

                if (ageText.isEmpty()) {
                    et5.setError("Enter your age");
                    et5.requestFocus();
                    return;
                }

                if (heightText.isEmpty()) {
                    et6.setError("Enter your height");
                    et6.requestFocus();
                    return;
                }

                if (weightText.isEmpty()) {
                    et7.setError("Enter your weight");
                    et7.requestFocus();
                    return;
                }

                try {
                    int age = Integer.parseInt(ageText);
                    double height = Double.parseDouble(heightText);
                    double weight = Double.parseDouble(weightText);

                    if (age < 10 || age > 110) {
                        et5.setError("Enter a realistic age (10 - 110)");
                        et5.requestFocus();
                        return;
                    }

                    if (height < 50 || height > 260) {
                        et6.setError("Enter a valid height in cm (50 - 260)");
                        et6.requestFocus();
                        return;
                    }

                    if (weight < 25 || weight > 350) {
                        et7.setError("Enter a valid weight in kg (25 - 350)");
                        et7.requestFocus();
                        return;
                    }

                    if (dbHelper.checkEmail(email)) {
                        et2.setError("This email is already registered");
                        et2.requestFocus();
                        return;
                    }

                    boolean inserted = dbHelper.insertUser(name, email, password, age, height, weight);

                    if (inserted) {
                        Toast.makeText(RegisterActivity.this, "Account created successfully! Please sign in.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("registered_email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(RegisterActivity.this, "Please enter numeric values for age, height and weight", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
