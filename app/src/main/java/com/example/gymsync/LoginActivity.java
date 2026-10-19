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

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button bt1;
    TextView tvRegister;
    DBHelper dbHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        bt1 = findViewById(R.id.bt1);
        tvRegister = findViewById(R.id.tvRegister);

        ImageButton btBack = findViewById(R.id.btBack);
        if (btBack != null) {
            btBack.setOnClickListener(v -> finish());
        }

        String registeredEmail = getIntent().getStringExtra("registered_email");
        if (registeredEmail != null && !registeredEmail.isEmpty()) {
            etEmail.setText(registeredEmail);
            etPassword.requestFocus();
        }

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim().toLowerCase();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty()) {
                    etEmail.setError("Enter your email address");
                    etEmail.requestFocus();
                    return;
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Enter a valid email address");
                    etEmail.requestFocus();
                    return;
                }
                if (password.isEmpty()) {
                    etPassword.setError("Enter your password");
                    etPassword.requestFocus();
                    return;
                }

                boolean result = dbHelper.checkUser(email, password);
                if (result) {
                    String userName = dbHelper.getUserName(email);
                    sessionManager.createLoginSession(email, userName);

                    Toast.makeText(LoginActivity.this, "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("user_email", email);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}