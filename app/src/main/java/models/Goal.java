// 🎯 Goal.java - Model class cho mục tiêu

package models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Goal {
    private int id;
    private String name;
    private double targetAmount;
    private double currentAmount;
    private String deadline;
    private String icon;
    private String status;
    private int userId;
    private String createdAt;

    // Constructors
    public Goal() {}

    public Goal(String name, double targetAmount, double currentAmount, String deadline,
                String icon, String status, int userId) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.icon = icon;
        this.status = status;
        this.userId = userId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods

    /**
     * Get progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (targetAmount <= 0) return 0;
        return Math.min(100, (int) ((currentAmount * 100) / targetAmount));
    }

    /**
     * Get remaining amount needed
     */
    public double getRemainingAmount() {
        return Math.max(0, targetAmount - currentAmount);
    }

    /**
     * Check if goal is completed
     */
    public boolean isCompleted() {
        return currentAmount >= targetAmount || "completed".equals(status);
    }

    /**
     * Check if goal is overdue
     */
    public boolean isOverdue() {
        try {
            LocalDate deadlineDate = LocalDate.parse(deadline);
            return LocalDate.now().isAfter(deadlineDate) && !isCompleted();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get days left until deadline
     */
    public long getDaysLeft() {
        try {
            LocalDate deadlineDate = LocalDate.parse(deadline);
            return ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get daily amount needed to reach goal
     */
    public double getDailyAmountNeeded() {
        long daysLeft = getDaysLeft();
        if (daysLeft <= 0 || isCompleted()) {
            return 0;
        }
        return getRemainingAmount() / daysLeft;
    }

    /**
     * Get monthly amount needed to reach goal
     */
    public double getMonthlyAmountNeeded() {
        return getDailyAmountNeeded() * 30;
    }

    /**
     * Get formatted deadline string
     */
    public String getFormattedDeadline() {
        try {
            LocalDate deadlineDate = LocalDate.parse(deadline);
            return deadlineDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return deadline;
        }
    }

    /**
     * Get days left as formatted string
     */
    public String getDaysLeftText() {
        long daysLeft = getDaysLeft();
        if (daysLeft < 0) {
            return "Đã quá hạn " + Math.abs(daysLeft) + " ngày";
        } else if (daysLeft == 0) {
            return "Hết hạn hôm nay!";
        } else if (daysLeft == 1) {
            return "Còn 1 ngày";
        } else {
            return "Còn " + daysLeft + " ngày";
        }
    }

    /**
     * Get status display text
     */
    public String getStatusDisplayText() {
        switch (status) {
            case "active":
                return "Đang thực hiện";
            case "completed":
                return "Hoàn thành";
            case "paused":
                return "Tạm dừng";
            default:
                return "Không xác định";
        }
    }

    /**
     * Get status color resource
     */
    public int getStatusColor() {
        switch (status) {
            case "completed":
                return android.R.color.holo_green_dark;
            case "paused":
                return android.R.color.holo_orange_dark;
            case "active":
            default:
                return android.R.color.holo_blue_bright;
        }
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", targetAmount=" + targetAmount +
                ", currentAmount=" + currentAmount +
                ", deadline='" + deadline + '\'' +
                ", icon='" + icon + '\'' +
                ", status='" + status + '\'' +
                ", userId=" + userId +
                ", progress=" + getProgressPercentage() + "%" +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Goal goal = (Goal) obj;
        return id == goal.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}