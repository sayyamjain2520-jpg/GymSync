package com.example.gymsync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GymSync.db";
    private static final int DATABASE_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table
        db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "email TEXT UNIQUE, "
                + "password TEXT, "
                + "age INTEGER, "
                + "height REAL, "
                + "weight REAL, "
                + "goal TEXT DEFAULT 'General Fitness', "
                + "phone TEXT DEFAULT '', "
                + "created_at TEXT)");

        // Workout plans table
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_plans ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT, "
                + "plan_name TEXT, "
                + "goal TEXT, "
                + "difficulty TEXT, "
                + "description TEXT, "
                + "is_starter INTEGER DEFAULT 0)");

        // Exercises table
        db.execSQL("CREATE TABLE IF NOT EXISTS exercises ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "plan_id INTEGER, "
                + "day_name TEXT, "
                + "exercise_name TEXT, "
                + "sets INTEGER, "
                + "reps INTEGER, "
                + "weight_kg REAL DEFAULT 0, "
                + "duration_mins INTEGER DEFAULT 0, "
                + "is_completed INTEGER DEFAULT 0)");

        // Workout logs table (tracks completed exercises/sessions)
        db.execSQL("CREATE TABLE IF NOT EXISTS workout_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT, "
                + "plan_name TEXT, "
                + "exercise_name TEXT, "
                + "completed_date TEXT, "
                + "timestamp INTEGER)");

        // Gym visit bookings table
        db.execSQL("CREATE TABLE IF NOT EXISTS gym_bookings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT, "
                + "gym_name TEXT, "
                + "gym_address TEXT, "
                + "visit_date TEXT, "
                + "visit_time TEXT, "
                + "contact_number TEXT, "
                + "notes TEXT, "
                + "status TEXT DEFAULT 'Request saved locally', "
                + "created_at TEXT)");

        // Subscriptions table
        db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT UNIQUE, "
                + "plan_name TEXT, "
                + "price TEXT, "
                + "billing_cycle TEXT, "
                + "start_date TEXT, "
                + "status TEXT DEFAULT 'Active')");

        // Occupancy demo/local estimates table
        db.execSQL("CREATE TABLE IF NOT EXISTS gym_occupancy ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "gym_name TEXT UNIQUE, "
                + "crowd_level TEXT, "
                + "capacity_pct INTEGER, "
                + "peak_hours TEXT, "
                + "equipment_status TEXT, "
                + "last_updated TEXT, "
                + "data_source TEXT)");

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Upgrade existing users table without dropping user data
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN goal TEXT DEFAULT 'General Fitness'");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT DEFAULT ''");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN created_at TEXT");
            } catch (Exception ignored) {}

            // Create new tables
            db.execSQL("CREATE TABLE IF NOT EXISTS workout_plans ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_email TEXT, "
                    + "plan_name TEXT, "
                    + "goal TEXT, "
                    + "difficulty TEXT, "
                    + "description TEXT, "
                    + "is_starter INTEGER DEFAULT 0)");

            db.execSQL("CREATE TABLE IF NOT EXISTS exercises ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "plan_id INTEGER, "
                    + "day_name TEXT, "
                    + "exercise_name TEXT, "
                    + "sets INTEGER, "
                    + "reps INTEGER, "
                    + "weight_kg REAL DEFAULT 0, "
                    + "duration_mins INTEGER DEFAULT 0, "
                    + "is_completed INTEGER DEFAULT 0)");

            db.execSQL("CREATE TABLE IF NOT EXISTS workout_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_email TEXT, "
                    + "plan_name TEXT, "
                    + "exercise_name TEXT, "
                    + "completed_date TEXT, "
                    + "timestamp INTEGER)");

            db.execSQL("CREATE TABLE IF NOT EXISTS gym_bookings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_email TEXT, "
                    + "gym_name TEXT, "
                    + "gym_address TEXT, "
                    + "visit_date TEXT, "
                    + "visit_time TEXT, "
                    + "contact_number TEXT, "
                    + "notes TEXT, "
                    + "status TEXT DEFAULT 'Request saved locally', "
                    + "created_at TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_email TEXT UNIQUE, "
                    + "plan_name TEXT, "
                    + "price TEXT, "
                    + "billing_cycle TEXT, "
                    + "start_date TEXT, "
                    + "status TEXT DEFAULT 'Active')");

            db.execSQL("CREATE TABLE IF NOT EXISTS gym_occupancy ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "gym_name TEXT UNIQUE, "
                    + "crowd_level TEXT, "
                    + "capacity_pct INTEGER, "
                    + "peak_hours TEXT, "
                    + "equipment_status TEXT, "
                    + "last_updated TEXT, "
                    + "data_source TEXT)");

            seedInitialData(db);
        }
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Seed starter workout plans
        long plan1Id = insertStarterPlan(db, "Full Body Foundation", "General Fitness", "Beginner",
                "3-day balanced workout focusing on functional strength and core endurance.");
        insertExercise(db, plan1Id, "Monday", "Bodyweight Squats", 3, 12, 0, 0);
        insertExercise(db, plan1Id, "Monday", "Wall/Standard Push-ups", 3, 10, 0, 0);
        insertExercise(db, plan1Id, "Monday", "Dumbbell Bent Rows", 3, 12, 10, 0);
        insertExercise(db, plan1Id, "Monday", "Plank Hold", 3, 1, 0, 1);
        insertExercise(db, plan1Id, "Wednesday", "Walking Lunges", 3, 10, 0, 0);
        insertExercise(db, plan1Id, "Wednesday", "Overhead Shoulder Press", 3, 10, 8, 0);
        insertExercise(db, plan1Id, "Wednesday", "Glute Bridges", 3, 12, 0, 0);
        insertExercise(db, plan1Id, "Friday", "Goblet Squats", 3, 12, 12, 0);
        insertExercise(db, plan1Id, "Friday", "Dumbbell Bench Press", 3, 10, 12, 0);
        insertExercise(db, plan1Id, "Friday", "Mountain Climbers", 3, 20, 0, 0);

        long plan2Id = insertStarterPlan(db, "Push-Pull-Legs Power", "Muscle Building", "Intermediate",
                "Classic 3-day split designed for hypertrophy and compound lifting strength.");
        insertExercise(db, plan2Id, "Push Day", "Incline Dumbbell Press", 4, 10, 16, 0);
        insertExercise(db, plan2Id, "Push Day", "Seated Shoulder Press", 3, 10, 12, 0);
        insertExercise(db, plan2Id, "Push Day", "Cable Tricep Pushdown", 3, 12, 15, 0);
        insertExercise(db, plan2Id, "Push Day", "Lateral Raises", 3, 15, 6, 0);
        insertExercise(db, plan2Id, "Pull Day", "Lat Pulldown", 4, 10, 45, 0);
        insertExercise(db, plan2Id, "Pull Day", "Seated Cable Row", 3, 12, 40, 0);
        insertExercise(db, plan2Id, "Pull Day", "Barbell Bicep Curls", 3, 12, 15, 0);
        insertExercise(db, plan2Id, "Legs Day", "Barbell Back Squats", 4, 8, 50, 0);
        insertExercise(db, plan2Id, "Legs Day", "Romanian Deadlift", 3, 10, 40, 0);
        insertExercise(db, plan2Id, "Legs Day", "Calf Raises", 4, 15, 30, 0);

        long plan3Id = insertStarterPlan(db, "Cardio & Core Burn", "Weight Loss", "All Levels",
                "High-tempo conditioning circuit for fat loss, stamina, and core definition.");
        insertExercise(db, plan3Id, "Session A", "Jumping Jacks", 4, 30, 0, 0);
        insertExercise(db, plan3Id, "Session A", "Burpees", 3, 10, 0, 0);
        insertExercise(db, plan3Id, "Session A", "Bicycle Crunches", 3, 20, 0, 0);
        insertExercise(db, plan3Id, "Session A", "Treadmill Intervals", 1, 1, 0, 15);

        // Seed sample occupancy records
        insertOccupancyData(db, "Iron Paradise Fitness", "Moderate", 58, "6:00 PM - 8:30 PM", "Free weights active, treadmills open");
        insertOccupancyData(db, "Power Zone Gym", "Low", 28, "7:00 PM - 9:00 PM", "Ample space, all racks available");
        insertOccupancyData(db, "FitLife Training Club", "High", 86, "5:30 PM - 8:00 PM", "High activity, wait times for benches");
        insertOccupancyData(db, "MetroFlex Athletics", "Low", 22, "6:00 AM - 8:00 AM", "Quiet morning hours, smooth flow");

        // Seed demo athlete account
        ContentValues userCv = new ContentValues();
        userCv.put("name", "Alex Mercer");
        userCv.put("email", "demo@gymsync.com");
        userCv.put("password", hashPassword("gymsync123"));
        userCv.put("age", 24);
        userCv.put("height", 178.0);
        userCv.put("weight", 74.0);
        userCv.put("goal", "Muscle Building");
        userCv.put("phone", "+91 98230 45678");
        userCv.put("created_at", "2026-10-19 08:00");
        db.insertWithOnConflict("users", null, userCv, SQLiteDatabase.CONFLICT_IGNORE);

        // Seed demo initial workout log
        ContentValues logCv = new ContentValues();
        logCv.put("user_email", "demo@gymsync.com");
        logCv.put("plan_name", "Full Body Foundation");
        logCv.put("exercise_name", "Bodyweight Squats");
        logCv.put("completed_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        logCv.put("timestamp", System.currentTimeMillis());
        db.insert("workout_logs", null, logCv);

        // Seed demo visit booking
        ContentValues bookingCv = new ContentValues();
        bookingCv.put("user_email", "demo@gymsync.com");
        bookingCv.put("gym_name", "Iron Paradise Fitness");
        bookingCv.put("gym_address", "Main Road, Station Area");
        bookingCv.put("visit_date", "Tomorrow");
        bookingCv.put("visit_time", "06:30 PM");
        bookingCv.put("contact_number", "+91 98230 45678");
        bookingCv.put("notes", "Trial workout and facility tour");
        bookingCv.put("status", "Request saved locally");
        bookingCv.put("created_at", "2026-10-19 08:00");
        db.insert("gym_bookings", null, bookingCv);
    }

    private long insertStarterPlan(SQLiteDatabase db, String name, String goal, String diff, String desc) {
        ContentValues cv = new ContentValues();
        cv.put("user_email", "system");
        cv.put("plan_name", name);
        cv.put("goal", goal);
        cv.put("difficulty", diff);
        cv.put("description", desc);
        cv.put("is_starter", 1);
        return db.insert("workout_plans", null, cv);
    }

    private void insertExercise(SQLiteDatabase db, long planId, String day, String name, int sets, int reps, double weight, int duration) {
        ContentValues cv = new ContentValues();
        cv.put("plan_id", planId);
        cv.put("day_name", day);
        cv.put("exercise_name", name);
        cv.put("sets", sets);
        cv.put("reps", reps);
        cv.put("weight_kg", weight);
        cv.put("duration_mins", duration);
        cv.put("is_completed", 0);
        db.insert("exercises", null, cv);
    }

    private void insertOccupancyData(SQLiteDatabase db, String gymName, String crowd, int pct, String peak, String equip) {
        ContentValues cv = new ContentValues();
        cv.put("gym_name", gymName);
        cv.put("crowd_level", crowd);
        cv.put("capacity_pct", pct);
        cv.put("peak_hours", peak);
        cv.put("equipment_status", equip);
        cv.put("last_updated", "Just now");
        cv.put("data_source", "Historical Check-in Demo Mode");
        db.insertWithOnConflict("gym_occupancy", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ==========================================
    // USER AUTHENTICATION & PROFILE METHODS
    // ==========================================

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback
        }
    }

    public boolean insertUser(String name, String email, String password, int age, double height, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email.toLowerCase().trim());
        cv.put("password", hashPassword(password));
        cv.put("age", age);
        cv.put("height", height);
        cv.put("weight", weight);
        cv.put("goal", "General Fitness");
        cv.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

        long result = db.insert("users", null, cv);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String cleanEmail = email.toLowerCase().trim();
        String hashed = hashPassword(password);

        // Check hashed password first
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND password = ?", new String[]{cleanEmail, hashed});
        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            return true;
        }
        if (cursor != null) cursor.close();

        // Check plain text (for existing legacy records) and upgrade
        Cursor legacyCursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND password = ?", new String[]{cleanEmail, password});
        if (legacyCursor != null && legacyCursor.getCount() > 0) {
            legacyCursor.close();
            // Upgrade to hashed
            SQLiteDatabase writableDb = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("password", hashed);
            writableDb.update("users", cv, "email = ?", new String[]{cleanEmail});
            return true;
        }
        if (legacyCursor != null) legacyCursor.close();

        return false;
    }

    public boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email.toLowerCase().trim()});
        boolean result = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return result;
    }

    public Cursor getUserDetails(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, email, age, height, weight, goal, phone, created_at "
                + "FROM users WHERE email = ?", new String[]{email.toLowerCase().trim()});
    }

    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM users WHERE email = ?", new String[]{email.toLowerCase().trim()});
        String name = "Athlete";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }
        return name;
    }

    public boolean updateUserProfile(String email, String name, int age, double height, double weight, String goal, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("age", age);
        cv.put("height", height);
        cv.put("weight", weight);
        if (goal != null && !goal.isEmpty()) {
            cv.put("goal", goal);
        }
        if (phone != null) {
            cv.put("phone", phone);
        }

        int rows = db.update("users", cv, "email = ?", new String[]{email.toLowerCase().trim()});
        return rows > 0;
    }

    // ==========================================
    // WORKOUT PLANNER METHODS
    // ==========================================

    public Cursor getWorkoutPlans(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, user_email, plan_name, goal, difficulty, description, is_starter "
                + "FROM workout_plans WHERE is_starter = 1 OR user_email = ? ORDER BY is_starter DESC, id DESC",
                new String[]{userEmail.toLowerCase().trim()});
    }

    public long createUserPlan(String userEmail, String planName, String goal, String difficulty, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_email", userEmail.toLowerCase().trim());
        cv.put("plan_name", planName);
        cv.put("goal", goal);
        cv.put("difficulty", difficulty);
        cv.put("description", description);
        cv.put("is_starter", 0);
        return db.insert("workout_plans", null, cv);
    }

    public boolean deleteUserPlan(long planId, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Starter plans cannot be deleted
        int rows = db.delete("workout_plans", "id = ? AND user_email = ? AND is_starter = 0",
                new String[]{String.valueOf(planId), userEmail.toLowerCase().trim()});
        if (rows > 0) {
            db.delete("exercises", "plan_id = ?", new String[]{String.valueOf(planId)});
        }
        return rows > 0;
    }

    public Cursor getExercisesForPlan(long planId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, plan_id, day_name, exercise_name, sets, reps, weight_kg, duration_mins, is_completed "
                + "FROM exercises WHERE plan_id = ? ORDER BY id ASC", new String[]{String.valueOf(planId)});
    }

    public long addExercise(long planId, String dayName, String exerciseName, int sets, int reps, double weight, int duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("plan_id", planId);
        cv.put("day_name", dayName);
        cv.put("exercise_name", exerciseName);
        cv.put("sets", sets);
        cv.put("reps", reps);
        cv.put("weight_kg", weight);
        cv.put("duration_mins", duration);
        cv.put("is_completed", 0);
        return db.insert("exercises", null, cv);
    }

    public boolean toggleExerciseCompleted(long exerciseId, boolean isCompleted, String userEmail, String planName, String exerciseName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_completed", isCompleted ? 1 : 0);
        int rows = db.update("exercises", cv, "id = ?", new String[]{String.valueOf(exerciseId)});

        if (isCompleted && userEmail != null && !userEmail.isEmpty()) {
            ContentValues logCv = new ContentValues();
            logCv.put("user_email", userEmail.toLowerCase().trim());
            logCv.put("plan_name", planName);
            logCv.put("exercise_name", exerciseName);
            logCv.put("completed_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            logCv.put("timestamp", System.currentTimeMillis());
            db.insert("workout_logs", null, logCv);
        }
        return rows > 0;
    }

    public int getTodayCompletedCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM workout_logs WHERE user_email = ? AND completed_date = ?",
                new String[]{userEmail.toLowerCase().trim(), today});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getTotalCompletedCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM workout_logs WHERE user_email = ?",
                new String[]{userEmail.toLowerCase().trim()});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // ==========================================
    // GYM VISIT BOOKINGS
    // ==========================================

    public boolean insertGymBooking(String email, String gymName, String gymAddress, String visitDate, String visitTime, String contact, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_email", email.toLowerCase().trim());
        cv.put("gym_name", gymName);
        cv.put("gym_address", gymAddress);
        cv.put("visit_date", visitDate);
        cv.put("visit_time", visitTime);
        cv.put("contact_number", contact);
        cv.put("notes", notes != null ? notes : "");
        cv.put("status", "Request saved locally");
        cv.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

        long result = db.insert("gym_bookings", null, cv);
        return result != -1;
    }

    public Cursor getUserBookings(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, gym_name, gym_address, visit_date, visit_time, contact_number, status, created_at "
                + "FROM gym_bookings WHERE user_email = ? ORDER BY id DESC", new String[]{email.toLowerCase().trim()});
    }

    public int getUserBookingsCount(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM gym_bookings WHERE user_email = ?",
                new String[]{email.toLowerCase().trim()});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // ==========================================
    // SUBSCRIPTIONS
    // ==========================================

    public boolean saveSubscription(String email, String planName, String price, String cycle) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_email", email.toLowerCase().trim());
        cv.put("plan_name", planName);
        cv.put("price", price);
        cv.put("billing_cycle", cycle);
        cv.put("start_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        cv.put("status", "Active");

        long result = db.insertWithOnConflict("subscriptions", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public String getUserSubscriptionPlan(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT plan_name FROM subscriptions WHERE user_email = ?",
                new String[]{email.toLowerCase().trim()});
        String plan = "Free Tier";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                plan = cursor.getString(0);
            }
            cursor.close();
        }
        return plan;
    }

    // ==========================================
    // OCCUPANCY
    // ==========================================

    public Cursor getGymOccupancy(String gymName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, gym_name, crowd_level, capacity_pct, peak_hours, equipment_status, last_updated, data_source "
                + "FROM gym_occupancy WHERE gym_name = ?", new String[]{gymName});
    }

    public Cursor getAllGymOccupancy() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, gym_name, crowd_level, capacity_pct, peak_hours, equipment_status, last_updated, data_source "
                + "FROM gym_occupancy ORDER BY id ASC", null);
    }

    public boolean updateGymOccupancy(String gymName, String crowdLevel, int capacityPct, String equipmentStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("gym_name", gymName);
        cv.put("crowd_level", crowdLevel);
        cv.put("capacity_pct", capacityPct);
        cv.put("equipment_status", equipmentStatus);
        cv.put("last_updated", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
        cv.put("data_source", "Locally updated estimate");

        long result = db.insertWithOnConflict("gym_occupancy", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }
}