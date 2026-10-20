package com.example.gymsync;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class WorkoutActivity extends AppCompatActivity {

    TextView tvGoal, tvWorkout, tvDifficulty, tvProgressHeader, tvCompletedCountBadge, tvEmptyExercises;
    Button btGenerate, btAddExercise, btDeletePlan;
    ImageButton btBack, btAddCustomPlan;
    Spinner spPlanSelector;
    ProgressBar pbWorkoutProgress;
    LinearLayout llExercisesContainer;

    String userEmail;
    DBHelper dbHelper;
    SessionManager sessionManager;

    private final List<WorkoutPlanModel> planList = new ArrayList<>();
    private WorkoutPlanModel currentPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        userEmail = getIntent().getStringExtra("user_email");
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = sessionManager.getUserEmail();
        }

        initViews();
        setupListeners();
        loadWorkoutPlans();
        updateDailyProgress();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvGoal = findViewById(R.id.tvGoal);
        tvWorkout = findViewById(R.id.tvWorkout);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvProgressHeader = findViewById(R.id.tvProgressHeader);
        tvCompletedCountBadge = findViewById(R.id.tvCompletedCountBadge);
        tvEmptyExercises = findViewById(R.id.tvEmptyExercises);

        btGenerate = findViewById(R.id.btGenerate);
        btBack = findViewById(R.id.btBack);
        btAddCustomPlan = findViewById(R.id.btAddCustomPlan);
        btAddExercise = findViewById(R.id.btAddExercise);
        btDeletePlan = findViewById(R.id.btDeletePlan);

        spPlanSelector = findViewById(R.id.spPlanSelector);
        pbWorkoutProgress = findViewById(R.id.pbWorkoutProgress);
        llExercisesContainer = findViewById(R.id.llExercisesContainer);
    }

    private void setupListeners() {

        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btAddCustomPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreatePlanDialog();
            }
        });

        btAddExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPlan!=null){
                    showAddExerciseDialog(currentPlan.getId());
                } else {
                    Toast.makeText(WorkoutActivity.this, "Please select a plan first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btDeletePlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPlan != null && !currentPlan.isStarter()) {
                    showDeletePlanDialog(currentPlan);
                }
            }
        });

        btGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPlan != null) {
                    Toast.makeText(WorkoutActivity.this, "Refreshing checklist for: " + currentPlan.getPlanName(), Toast.LENGTH_SHORT).show();
                    loadExercisesForPlan(currentPlan.getId());
                }
            }
        });


        spPlanSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < planList.size()) {
                    currentPlan = planList.get(position);
                    displayPlanDetails(currentPlan);
                    loadExercisesForPlan(currentPlan.getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadWorkoutPlans() {
        planList.clear();
        Cursor cursor = dbHelper.getWorkoutPlans(userEmail);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("plan_name"));
                String goal = cursor.getString(cursor.getColumnIndexOrThrow("goal"));
                String diff = cursor.getString(cursor.getColumnIndexOrThrow("difficulty"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                int isStarter = cursor.getInt(cursor.getColumnIndexOrThrow("is_starter"));

                planList.add(new WorkoutPlanModel(id, name, goal, diff, desc, isStarter == 1));
            }
            cursor.close();
        }

        ArrayAdapter<WorkoutPlanModel> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, planList);
        spPlanSelector.setAdapter(adapter);

        if (!planList.isEmpty()) {
            currentPlan = planList.get(0);
            displayPlanDetails(currentPlan);
            loadExercisesForPlan(currentPlan.getId());
        }
    }

    private void displayPlanDetails(WorkoutPlanModel plan) {
        if (plan == null) return;

        tvGoal.setText("Goal: " + plan.getGoal());
        tvDifficulty.setText(plan.getDifficulty());
        tvWorkout.setText(plan.getDescription());

        if (plan.isStarter()) {
            btDeletePlan.setVisibility(View.GONE);
        } else {
            btDeletePlan.setVisibility(View.VISIBLE);
        }
    }

    private void loadExercisesForPlan(long planId) {
        llExercisesContainer.removeAllViews();
        Cursor cursor = dbHelper.getExercisesForPlan(planId);

        int totalExercises = 0;
        int completedInPlan = 0;

        if (cursor != null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            while (cursor.moveToNext()) {
                totalExercises++;
                long exerciseId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String dayName = cursor.getString(cursor.getColumnIndexOrThrow("day_name"));
                String exerciseName = cursor.getString(cursor.getColumnIndexOrThrow("exercise_name"));
                int sets = cursor.getInt(cursor.getColumnIndexOrThrow("sets"));
                int reps = cursor.getInt(cursor.getColumnIndexOrThrow("reps"));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight_kg"));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration_mins"));
                int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed"));

                if (isCompleted == 1) completedInPlan++;

                View itemView = inflater.inflate(R.layout.item_exercise, llExercisesContainer, false);

                CheckBox cb = itemView.findViewById(R.id.cbExercise);
                TextView tvName = itemView.findViewById(R.id.tvExerciseName);
                TextView tvDay = itemView.findViewById(R.id.tvDayName);
                TextView tvSets = itemView.findViewById(R.id.tvSetsReps);
                TextView tvWeightDur = itemView.findViewById(R.id.tvWeightDuration);

                tvName.setText(exerciseName);
                tvDay.setText(dayName);
                tvSets.setText(sets + " sets × " + reps + " reps");

                if (duration > 0) {
                    tvWeightDur.setText(duration + " mins");
                } else if (weight > 0) {
                    tvWeightDur.setText(weight + " kg");
                } else {
                    tvWeightDur.setText("Bodyweight");
                }

                cb.setChecked(isCompleted == 1);
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String pName = (currentPlan != null) ? currentPlan.getPlanName() : "General";
                    dbHelper.toggleExerciseCompleted(exerciseId, isChecked, userEmail, pName, exerciseName);
                    if (isChecked) {
                        Toast.makeText(WorkoutActivity.this, "Great job! " + exerciseName + " logged.", Toast.LENGTH_SHORT).show();
                    }
                    updateDailyProgress();
                });

                llExercisesContainer.addView(itemView);
            }
            cursor.close();
        }

        if (totalExercises == 0) {
            tvEmptyExercises.setVisibility(View.VISIBLE);
        } else {
            tvEmptyExercises.setVisibility(View.GONE);
        }
    }

    private void updateDailyProgress() {
        int todayCompleted = dbHelper.getTodayCompletedCount(userEmail);
        tvCompletedCountBadge.setText(todayCompleted + " Done Today");

        int progressPct = Math.min(100, todayCompleted * 15);
        pbWorkoutProgress.setProgress(progressPct);
    }

    private void showCreatePlanDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_plan, null);
        EditText etName = dialogView.findViewById(R.id.etPlanName);
        EditText etGoal = dialogView.findViewById(R.id.etPlanGoal);
        EditText etDiff = dialogView.findViewById(R.id.etPlanDifficulty);
        EditText etDesc = dialogView.findViewById(R.id.etPlanDesc);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Create Routine", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String goal = etGoal.getText().toString().trim();
                    String diff = etDiff.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(WorkoutActivity.this, "Routine name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (goal.isEmpty()) goal = "General Fitness";
                    if (diff.isEmpty()) diff = "Custom";
                    if (desc.isEmpty()) desc = "Custom athlete workout plan.";

                    long newId = dbHelper.createUserPlan(userEmail, name, goal, diff, desc);
                    if (newId != -1) {
                        Toast.makeText(WorkoutActivity.this, "Custom routine created!", Toast.LENGTH_SHORT).show();
                        loadWorkoutPlans();
                    } else {
                        Toast.makeText(WorkoutActivity.this, "Failed to create routine", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddExerciseDialog(long planId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        EditText etDay = dialogView.findViewById(R.id.etExerciseDay);
        EditText etName = dialogView.findViewById(R.id.etExerciseName);
        EditText etSets = dialogView.findViewById(R.id.etSets);
        EditText etReps = dialogView.findViewById(R.id.etReps);
        EditText etWeight = dialogView.findViewById(R.id.etWeight);
        EditText etDuration = dialogView.findViewById(R.id.etDuration);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Add Exercise", (dialog, which) -> {
                    String day = etDay.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String setsStr = etSets.getText().toString().trim();
                    String repsStr = etReps.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String durStr = etDuration.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(WorkoutActivity.this, "Exercise name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int sets = setsStr.isEmpty() ? 3 : Integer.parseInt(setsStr);
                    int reps = repsStr.isEmpty() ? 10 : Integer.parseInt(repsStr);
                    double weight = weightStr.isEmpty() ? 0 : Double.parseDouble(weightStr);
                    int dur = durStr.isEmpty() ? 0 : Integer.parseInt(durStr);
                    if (day.isEmpty()) day = "Day 1";

                    long exId = dbHelper.addExercise(planId, day, name, sets, reps, weight, dur);
                    if (exId != -1) {
                        Toast.makeText(WorkoutActivity.this, "Exercise added to routine!", Toast.LENGTH_SHORT).show();
                        loadExercisesForPlan(planId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeletePlanDialog(WorkoutPlanModel plan) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Routine")
                .setMessage("Are you sure you want to delete '" + plan.getPlanName() + "' and its exercises?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteUserPlan(plan.getId(), userEmail);
                    if (deleted) {
                        Toast.makeText(WorkoutActivity.this, "Routine deleted", Toast.LENGTH_SHORT).show();
                        loadWorkoutPlans();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}