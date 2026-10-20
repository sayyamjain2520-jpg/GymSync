package com.example.gymsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button bt1, btRegisterIntro;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("user_email", sessionManager.getUserEmail());
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bt1 = findViewById(R.id.bt1);
        btRegisterIntro = findViewById(R.id.btRegisterIntro);

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i1 = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i1);
            }
        });

        if (btRegisterIntro != null) {
            btRegisterIntro.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(intent);
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