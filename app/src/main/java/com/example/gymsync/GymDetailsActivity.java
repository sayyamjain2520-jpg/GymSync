package com.example.gymsync;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class GymDetailsActivity extends AppCompatActivity {

    TextView tvGymName, tvAddress, tvDistance, tvTimings, tvFacilities, tvRatingBadge, tvDataSource, tvExistingBookingsTitle;
    ImageButton btBack;
    Button btBookVisit, btDirections, btCallGym;
    LinearLayout llGymBookingsContainer;

    String gymName, gymAddress, gymDistance, gymPhone, userEmail, gymSource;
    double gymLat = 0.0, gymLng = 0.0;
    double gymRating = 4.8;

    DBHelper dbHelper;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gym_details);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        parseIntentData();
        setupListeners();
        loadExistingGymBookings();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvGymName = findViewById(R.id.tvGymName);
        tvAddress = findViewById(R.id.tvAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvTimings = findViewById(R.id.tvTimings);
        tvFacilities = findViewById(R.id.tvFacilities);
        tvRatingBadge = findViewById(R.id.tvRatingBadge);
        tvDataSource = findViewById(R.id.tvDataSource);
        tvExistingBookingsTitle = findViewById(R.id.tvExistingBookingsTitle);
        llGymBookingsContainer = findViewById(R.id.llGymBookingsContainer);

        btBack = findViewById(R.id.btBack);
        btBookVisit = findViewById(R.id.btBookVisit);
        btDirections = findViewById(R.id.btDirections);
        btCallGym = findViewById(R.id.btCallGym);
    }

    private void parseIntentData() {
        Intent intent = getIntent();
        gymName = intent.getStringExtra("gym_name");
        gymAddress = intent.getStringExtra("gym_address");
        gymDistance = intent.getStringExtra("gym_distance");
        gymPhone = intent.getStringExtra("gym_phone");
        gymSource = intent.getStringExtra("gym_source");
        userEmail = intent.getStringExtra("user_email");
        gymRating = intent.getDoubleExtra("gym_rating", -1.0);
        gymLat = intent.getDoubleExtra("gym_lat", 0.0);
        gymLng = intent.getDoubleExtra("gym_lng", 0.0);

        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = sessionManager.getUserEmail();
        }

        if (gymName == null || gymName.isEmpty()) gymName = "Gym";
        if (gymAddress == null || gymAddress.isEmpty()) gymAddress = "Location address not specified";
        if (gymDistance == null || gymDistance.isEmpty()) gymDistance = "Nearby";
        if (gymPhone == null) gymPhone = "";
        if (gymSource == null || gymSource.isEmpty()) gymSource = "Google Places API";

        tvGymName.setText(gymName);
        tvAddress.setText(gymAddress);
        tvDistance.setText(gymDistance);
        if (tvRatingBadge != null) {
            if (gymRating > 0.0) {
                tvRatingBadge.setVisibility(View.VISIBLE);
                tvRatingBadge.setText(String.format(Locale.getDefault(), "★ %.1f", gymRating));
            } else {
                tvRatingBadge.setText("Unrated");
            }
        }
        if (tvDataSource != null) {
            tvDataSource.setText(gymSource);
        }

        if (gymSource != null && (gymSource.contains("Fallback") || gymSource.contains("Sample"))) {
            tvTimings.setText("Sample Hours: 6:00 AM - 10:00 PM (Mon - Sat) • 7:00 AM - 1:00 PM (Sun)");
            tvFacilities.setText("Sample Facilities: Free weights, cardio equipment, locker rooms, trainers.");
        } else {
            tvTimings.setText("Timings: Check Google Maps or contact gym directly");
            tvFacilities.setText("Facilities: Weights, fitness equipment, training facilities.");
        }
    }

    private void setupListeners() {

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri;
                if (gymLat != 0.0 && gymLng != 0.0) {
                    gmmIntentUri = Uri.parse("geo:" + gymLat + "," + gymLng + "?q=" + Uri.encode(gymName));
                } else {
                    gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(gymName + " " + gymAddress));
                }

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback to web maps
                    Intent webMapIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(gymName + " " + gymAddress)));
                    startActivity(webMapIntent);
                }
            }
        });

        btCallGym.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gymPhone == null || gymPhone.trim().isEmpty()) {
                    Toast.makeText(GymDetailsActivity.this, "Phone number not available for this gym", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + gymPhone));
                startActivity(callIntent);
            }
        });

        btBookVisit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBookingDialog();
            }
        });

    }

    private void showBookingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_book_visit, null);

        TextView tvDialogGymName = dialogView.findViewById(R.id.tvDialogGymName);
        TextView tvBookingDate = dialogView.findViewById(R.id.tvBookingDate);
        TextView tvBookingTime = dialogView.findViewById(R.id.tvBookingTime);
        EditText etBookingPhone = dialogView.findViewById(R.id.etBookingPhone);
        EditText etBookingNotes = dialogView.findViewById(R.id.etBookingNotes);
        LinearLayout llSelectDate = dialogView.findViewById(R.id.llSelectDate);
        LinearLayout llSelectTime = dialogView.findViewById(R.id.llSelectTime);

        tvDialogGymName.setText("Schedule Visit to " + gymName);

        final String[] selectedDate = {""};
        final String[] selectedTime = {""};

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate[0] = sdfDate.format(calendar.getTime());
        tvBookingDate.setText(selectedDate[0] + " (Tap to change)");

        selectedTime[0] = "06:30 PM";
        tvBookingTime.setText("06:30 PM (Tap to change)");

        llSelectDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(GymDetailsActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth);
                        selectedDate[0] = sdfDate.format(chosen.getTime());
                        tvBookingDate.setText(selectedDate[0]);
                    },
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        llSelectTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(GymDetailsActivity.this,
                    (view, hourOfDay, minute) -> {
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int formattedHour = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                        selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, minute, amPm);
                        tvBookingTime.setText(selectedTime[0]);
                    }, 18, 30, false);
            timePickerDialog.show();
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm Request", (dialog, which) -> {
                    String phone = etBookingPhone.getText().toString().trim();
                    String notes = etBookingNotes.getText().toString().trim();

                    if (phone.isEmpty()) {
                        phone = "+91 98765 43210";
                    }

                    boolean saved = dbHelper.insertGymBooking(
                            userEmail,
                            gymName,
                            gymAddress,
                            selectedDate[0],
                            selectedTime[0],
                            phone,
                            notes
                    );

                    if (saved) {
                        Toast.makeText(GymDetailsActivity.this, "Visit request saved locally! Status: Request saved locally", Toast.LENGTH_LONG).show();
                        loadExistingGymBookings();
                    } else {
                        Toast.makeText(GymDetailsActivity.this, "Failed to save visit request", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadExistingGymBookings() {
        if (llGymBookingsContainer == null || userEmail == null) return;

        llGymBookingsContainer.removeAllViews();
        Cursor cursor = dbHelper.getUserBookings(userEmail);

        int count = 0;
        if (cursor != null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            while (cursor.moveToNext()) {
                String bookedGym = cursor.getString(cursor.getColumnIndexOrThrow("gym_name"));
                if (bookedGym != null && bookedGym.equalsIgnoreCase(gymName)) {
                    count++;
                    String visitDate = cursor.getString(cursor.getColumnIndexOrThrow("visit_date"));
                    String visitTime = cursor.getString(cursor.getColumnIndexOrThrow("visit_time"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundResource(R.drawable.bg_card_elevated);
                    card.setPadding(24, 20, 24, 20);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 16);
                    card.setLayoutParams(lp);

                    TextView tvInfo = new TextView(this);
                    tvInfo.setText("Slot: " + visitDate + " at " + visitTime);
                    tvInfo.setTextColor(getColor(R.color.text_primary));
                    tvInfo.setTextSize(14f);
                    tvInfo.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView tvStatusBadge = new TextView(this);
                    tvStatusBadge.setText("Status: " + status);
                    tvStatusBadge.setTextColor(getColor(R.color.purple_primary));
                    tvStatusBadge.setTextSize(12f);
                    tvStatusBadge.setPadding(0, 8, 0, 0);

                    card.addView(tvInfo);
                    card.addView(tvStatusBadge);
                    llGymBookingsContainer.addView(card);
                }
            }
            cursor.close();
        }

        if (count > 0) {
            tvExistingBookingsTitle.setVisibility(View.VISIBLE);
        } else {
            tvExistingBookingsTitle.setVisibility(View.GONE);
        }
    }
}