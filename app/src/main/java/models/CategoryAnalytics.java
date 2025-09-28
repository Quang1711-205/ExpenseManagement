package models;

public class CategoryAnalytics {
    private String name;
    private double amount;
    private double percentage;
    private String icon;
    private String color;
    private int transactionCount;

    // Constructors
    public CategoryAnalytics() {}

    public CategoryAnalytics(String name, double amount, double percentage, String icon, String color) {
        this.name = name;
        this.amount = amount;
        this.percentage = percentage;
        this.icon = icon;
        this.color = color;
    }

    public CategoryAnalytics(String name, double amount, double percentage, String icon, String color, int transactionCount) {
        this.name = name;
        this.amount = amount;
        this.percentage = percentage;
        this.icon = icon;
        this.color = color;
        this.transactionCount = transactionCount;
    }

    // Getters
    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getIcon() {
        return icon != null ? icon : "📦";
    }

    public String getColor() {
        return color != null ? color : "#2196F3";
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    // Utility methods
    public String getFormattedAmount() {
        return String.format("%,.0f đ", amount);
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }

    @Override
    public String toString() {
        return "CategoryAnalytics{" +
                "name='" + name + '\'' +
                ", amount=" + amount +
                ", percentage=" + percentage +
                ", icon='" + icon + '\'' +
                ", color='" + color + '\'' +
                ", transactionCount=" + transactionCount +
                '}';
    }
}