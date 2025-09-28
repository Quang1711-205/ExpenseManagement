// 💰 BudgetPlan.java - Complete Model with CategoryBudget - FULLY FIXED
package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 💰 FIXED: Complete BudgetPlan model with proper CategoryBudget inner class
 */
public class BudgetPlan implements Serializable {

    public enum Period {
        WEEKLY("weekly", "Hàng tuần"),
        MONTHLY("monthly", "Hàng tháng"),
        YEARLY("yearly", "Hàng năm");

        private final String value;
        private final String displayName;

        Period(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }

        public static Period fromValue(String value) {
            for (Period period : values()) {
                if (period.value.equals(value)) {
                    return period;
                }
            }
            return MONTHLY; // default
        }
    }

    // Main properties
    private int id;
    private String name;
    private Period period;
    private String startDate;  // ✅ FIXED: Changed from Date to String
    private String endDate;    // ✅ FIXED: Changed from Date to String
    private String userId;
    private Date createdAt;
    private Date updatedAt;
    private List<CategoryBudget> categories;

    // ✅ ADDED: New fields for SmartBudgetActivity compatibility
    private double totalAllocated;
    private double totalSpent;
    private double overallVariance;

    // Constructors
    public BudgetPlan() {
        this.categories = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public BudgetPlan(String name, Period period, String userId) {
        this();
        this.name = name;
        this.period = period;
        this.userId = userId;
    }

    // ✅ FIXED: CategoryBudget inner class with proper methods
    public static class CategoryBudget implements Serializable {
        private int id;
        private int categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private double allocatedAmount;
        private double spentAmount;
        private double remainingAmount;
        private String categoryType; // income/expense

        // ✅ ADDED: New fields for SmartBudgetActivity compatibility
        private double variance;
        private double percentageUsed;
        private String healthStatus;
        private int transactionCount;
        public int getTransactionCount() {
            return transactionCount;
        }
        public void setTransactionCount(int transactionCount) {
            this.transactionCount = transactionCount;
        }

        // Constructors
        public CategoryBudget() {}

        public CategoryBudget(int categoryId, String categoryName, double allocatedAmount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.allocatedAmount = allocatedAmount;
            this.spentAmount = 0.0;
            this.remainingAmount = allocatedAmount;
        }

        public CategoryBudget(int categoryId, String categoryName, String categoryIcon,
                              String categoryColor, double allocatedAmount, String categoryType) {
            this(categoryId, categoryName, allocatedAmount);
            this.categoryIcon = categoryIcon;
            this.categoryColor = categoryColor;
            this.categoryType = categoryType;
        }

        // ✅ FIXED: Add getId() method that was missing
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        // ✅ ALIAS METHODS: For adapter compatibility
        public String getName() {
            return getCategoryName();
        }

        public double getPercentageUsed() {
            // Use field if set, otherwise calculate
            if (this.percentageUsed > 0) {
                return this.percentageUsed;
            }
            return getUsagePercentage();
        }

        public String getIcon() {
            return getCategoryIcon();
        }

        // ✅ Method getVariance() - tính toán chênh lệch ngân sách
        public double getVariance() {
            // Use field if set, otherwise calculate
            if (this.variance != 0) {
                return this.variance;
            }
            return getAllocatedAmount() - getSpentAmount();
        }

        // ✅ Method getHealthStatus() - đánh giá tình trạng ngân sách
        public String getHealthStatus() {
            // Use field if set, otherwise calculate
            if (this.healthStatus != null && !this.healthStatus.isEmpty()) {
                return this.healthStatus;
            }

            double percentage = getUsagePercentage();
            if (percentage <= 50) {
                return "excellent";  // ≤ 50%: Tuyệt vời
            } else if (percentage <= 70) {
                return "good";       // 51-70%: Tốt
            } else if (percentage <= 80) {
                return "warning";    // 71-80%: Cảnh báo
            } else if (percentage <= 100) {
                return "danger";     // 81-100%: Nguy hiểm
            } else {
                return "critical";   // > 100%: Nghiêm trọng
            }
        }

        // ✅ Method getColor() - alias cho getCategoryColor()
        public String getColor() {
            return getCategoryColor() != null ? getCategoryColor() : "#808080";
        }

        // ✅ ADDED: Setter methods for SmartBudgetActivity
        public void setName(String name) {
            this.categoryName = name;
        }

        public void setIcon(String icon) {
            this.categoryIcon = icon;
        }

        public void setColor(String color) {
            this.categoryColor = color;
        }

        public void setVariance(double variance) {
            this.variance = variance;
        }

        public void setPercentageUsed(double percentageUsed) {
            this.percentageUsed = percentageUsed;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        // ORIGINAL GETTERS AND SETTERS
        public int getCategoryId() { return categoryId; }
        public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getCategoryIcon() { return categoryIcon; }
        public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

        public String getCategoryColor() { return categoryColor; }
        public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

        public double getAllocatedAmount() { return allocatedAmount; }
        public void setAllocatedAmount(double allocatedAmount) {
            this.allocatedAmount = allocatedAmount;
            updateRemainingAmount();
        }

        public double getSpentAmount() { return spentAmount; }
        public void setSpentAmount(double spentAmount) {
            this.spentAmount = spentAmount;
            updateRemainingAmount();
        }

        public double getRemainingAmount() { return remainingAmount; }

        public String getCategoryType() { return categoryType; }
        public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

        // Helper methods
        private void updateRemainingAmount() {
            this.remainingAmount = this.allocatedAmount - this.spentAmount;
        }

        public double getUsagePercentage() {
            if (allocatedAmount <= 0) return 0.0;
            return (spentAmount / allocatedAmount) * 100.0;
        }

        public boolean isOverBudget() {
            return spentAmount > allocatedAmount;
        }

        public boolean isWarningLevel() {
            return getUsagePercentage() >= 80.0;
        }

        public String getStatusText() {
            if (isOverBudget()) {
                return "Vượt ngân sách";
            } else if (isWarningLevel()) {
                return "Gần hết ngân sách";
            } else if (getUsagePercentage() >= 50.0) {
                return "Đang sử dụng";
            } else {
                return "Còn nhiều";
            }
        }

        @Override
        public String toString() {
            return "CategoryBudget{" +
                    "categoryId=" + categoryId +
                    ", categoryName='" + categoryName + '\'' +
                    ", allocatedAmount=" + allocatedAmount +
                    ", spentAmount=" + spentAmount +
                    ", remainingAmount=" + remainingAmount +
                    '}';
        }
    }

    // Main class getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    // ✅ FIXED: setPeriod method for String compatibility
    public void setPeriod(String periodValue) {
        this.period = Period.fromValue(periodValue);
    }

    // ✅ FIXED: Add getPeriod() string method for compatibility
    public String getPeriodValue() {
        return period != null ? period.getValue() : "monthly";
    }

    public String getPeriodDisplayName() {
        return period != null ? period.getDisplayName() : "Hàng tháng";
    }

    // ✅ FIXED: Date methods changed to String
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public List<CategoryBudget> getCategories() {
        return categories != null ? categories : new ArrayList<>();
    }
    public void setCategories(List<CategoryBudget> categories) { this.categories = categories; }

    // ✅ ADDED: New getters/setters for SmartBudgetActivity
    public double getTotalAllocated() {
        // Use field if set, otherwise calculate
        if (this.totalAllocated > 0) {
            return this.totalAllocated;
        }
        return calculateTotalAllocated();
    }

    public void setTotalAllocated(double totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public double getTotalSpent() {
        // Use field if set, otherwise calculate
        if (this.totalSpent > 0) {
            return this.totalSpent;
        }
        return calculateTotalSpent();
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public double getOverallVariance() {
        // Use field if set, otherwise calculate
        if (this.overallVariance != 0) {
            return this.overallVariance;
        }
        return getTotalAllocated() - getTotalSpent();
    }

    public void setOverallVariance(double overallVariance) {
        this.overallVariance = overallVariance;
    }

    // Helper methods - renamed to avoid confusion
    public void addCategoryBudget(CategoryBudget categoryBudget) {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        categories.add(categoryBudget);
    }

    public void removeCategoryBudget(int categoryId) {
        if (categories != null) {
            categories.removeIf(cb -> cb.getCategoryId() == categoryId);
        }
    }

    public CategoryBudget getCategoryBudget(int categoryId) {
        if (categories == null) return null;

        for (CategoryBudget cb : categories) {
            if (cb.getCategoryId() == categoryId) {
                return cb;
            }
        }
        return null;
    }

    // ✅ RENAMED: Original calculation methods
    private double calculateTotalAllocated() {
        if (categories == null) return 0.0;

        double total = 0.0;
        for (CategoryBudget cb : categories) {
            total += cb.getAllocatedAmount();
        }
        return total;
    }

    private double calculateTotalSpent() {
        if (categories == null) return 0.0;

        double total = 0.0;
        for (CategoryBudget cb : categories) {
            total += cb.getSpentAmount();
        }
        return total;
    }

    public double getTotalRemaining() {
        return getTotalAllocated() - getTotalSpent();
    }

    public double getOverallUsagePercentage() {
        double allocated = getTotalAllocated();
        if (allocated <= 0) return 0.0;

        return (getTotalSpent() / allocated) * 100.0;
    }

    public boolean isOverBudget() {
        return getTotalSpent() > getTotalAllocated();
    }

    public int getOverBudgetCategoriesCount() {
        if (categories == null) return 0;

        int count = 0;
        for (CategoryBudget cb : categories) {
            if (cb.isOverBudget()) {
                count++;
            }
        }
        return count;
    }

    public int getWarningCategoriesCount() {
        if (categories == null) return 0;

        int count = 0;
        for (CategoryBudget cb : categories) {
            if (cb.isWarningLevel() && !cb.isOverBudget()) {
                count++;
            }
        }
        return count;
    }

    public String getBudgetStatusText() {
        if (isOverBudget()) {
            return "Vượt ngân sách";
        } else if (getOverallUsagePercentage() >= 80.0) {
            return "Gần hết ngân sách";
        } else if (getOverallUsagePercentage() >= 50.0) {
            return "Đang theo dõi";
        } else {
            return "Trong tầm kiểm soát";
        }
    }

    @Override
    public String toString() {
        return "BudgetPlan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", period=" + period +
                ", totalAllocated=" + getTotalAllocated() +
                ", totalSpent=" + getTotalSpent() +
                ", categoriesCount=" + (categories != null ? categories.size() : 0) +
                '}';
    }
}