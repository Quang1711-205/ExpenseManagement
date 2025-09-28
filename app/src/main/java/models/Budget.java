package models;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Budget {
    private String id;
    private String name;
    private String period;
    private String startDate;
    private String endDate;
    private double totalAmount;
    private double spentAmount;
    private String status;
    private String icon;
    private int categoriesCount;
    private String healthScore;
    private String remainingDays;
    private String userId;

    // Original constructor
    public Budget(String id, String name, String period, String endDate,
                  double totalAmount, double spentAmount, String status,
                  String icon, int categoriesCount, String healthScore, String remainingDays) {
        this.id = id;
        this.name = name;
        this.period = period;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
        this.spentAmount = spentAmount;
        this.status = status;
        this.icon = icon;
        this.categoriesCount = categoriesCount;
        this.healthScore = healthScore;
        this.remainingDays = remainingDays;
    }

    // Full constructor
    public Budget(String id, String name, String period, String startDate, String endDate,
                  double totalAmount, double spentAmount, String status,
                  String icon, int categoriesCount, String healthScore, String remainingDays, String userId) {
        this.id = id;
        this.name = name;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
        this.spentAmount = spentAmount;
        this.status = status;
        this.icon = icon;
        this.categoriesCount = categoriesCount;
        this.healthScore = healthScore;
        this.remainingDays = remainingDays;
        this.userId = userId;
    }

    private String formatAmount(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount) + " VND";
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    // Phương thức format tổng số tiền ngân sách
    public String getFormattedTotalAmount() {
        return formatCurrency(totalAmount);
    }

    // Phương thức format số tiền đã chi tiêu
    public String getFormattedSpentAmount() {
        return formatCurrency(spentAmount);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getCategoriesCount() { return categoriesCount; }
    public void setCategoriesCount(int categoriesCount) { this.categoriesCount = categoriesCount; }

    public String getHealthScore() { return healthScore; }
    public void setHealthScore(String healthScore) { this.healthScore = healthScore; }

    public String getRemainingDays() { return remainingDays; }
    public void setRemainingDays(String remainingDays) { this.remainingDays = remainingDays; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Calculate progress percentage
    public int getProgressPercentage() {
        if (totalAmount <= 0) return 0;
        return (int) Math.min(100, (spentAmount / totalAmount) * 100);
    }


    // Get remaining amount
    public double getRemainingAmount() {
        return totalAmount - spentAmount;
    }

    // Check if budget is over spent
    public boolean isOverBudget() {
        return spentAmount > totalAmount;
    }

    // Get formatted date range
    public String getDateRange() {
        if (startDate != null && endDate != null) {
            return startDate + " - " + endDate;
        }
        return "Không xác định";
    }

    // ToString for debugging
    @Override
    public String toString() {
        return "Budget{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", period='" + period + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", totalAmount=" + totalAmount +
                ", spentAmount=" + spentAmount +
                ", status='" + status + '\'' +
                ", categoriesCount=" + categoriesCount +
                ", healthScore='" + healthScore + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}