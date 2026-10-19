package com.example.gymsync;

public class WorkoutPlanModel {
    private long id;
    private String planName;
    private String goal;
    private String difficulty;
    private String description;
    private boolean isStarter;

    public WorkoutPlanModel(long id, String planName, String goal, String difficulty, String description, boolean isStarter) {
        this.id = id;
        this.planName = planName;
        this.goal = goal;
        this.difficulty = difficulty;
        this.description = description;
        this.isStarter = isStarter;
    }

    public long getId() { return id; }
    public String getPlanName() { return planName; }
    public String getGoal() { return goal; }
    public String getDifficulty() { return difficulty; }
    public String getDescription() { return description; }
    public boolean isStarter() { return isStarter; }

    @Override
    public String toString() {
        return planName + (isStarter ? " (Starter)" : " (Custom)");
    }
}
