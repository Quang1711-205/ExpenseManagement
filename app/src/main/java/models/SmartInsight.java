// 🧠 SmartInsight.java - FIXED Model with proper enums
package models;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 🧠 FIXED: Smart Insight Model với enum definitions đầy đủ
 */
public class SmartInsight {

    // ✅ FIXED: Định nghĩa đầy đủ các enum
    public enum Type {
        PATTERN,        // Phát hiện pattern chi tiêu
        TREND,          // Xu hướng thay đổi
        WARNING,        // Cảnh báo
        RISK,           // Rủi ro cao
        OPTIMIZATION,   // Tối ưu hóa
        FORECAST,       // Dự báo
        POSITIVE,       // Insight tích cực
        INFO
        }

    public enum Impact {
        HIGH,           // Tác động cao
        MEDIUM,         // Tác động trung bình
        LOW,            // Tác động thấp
        POSITIVE        // Tác động tích cực
    }

    // Properties
    private int id;
    private String title;
    private String message;
    private Type type;
    private Impact impact;
    private Double value;
    private String suggestion;
    private boolean isActionable;
    private boolean isDismissed;
    private boolean isHighPriority;
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public SmartInsight() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isDismissed = false;
        this.isActionable = false;
    }

    public SmartInsight(String title, String message, Type type, Impact impact) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
        this.impact = impact;
        this.isHighPriority = (impact == Impact.HIGH);
    }

    public SmartInsight(String title, String message, Type type, Impact impact, Double value) {
        this(title, message, type, impact);
        this.value = value;
    }

    public SmartInsight(String title, String message, Type type, Impact impact, Double value, String suggestion) {
        this(title, message, type, impact, value);
        this.suggestion = suggestion;
        this.isActionable = (suggestion != null && !suggestion.isEmpty());
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Type getType() { return type; }
    public void setType(Type type) {
        this.type = type;
        this.updatedAt = new Date();
    }

    public Impact getImpact() { return impact; }
    public void setImpact(Impact impact) {
        this.impact = impact;
        this.isHighPriority = (impact == Impact.HIGH);
        this.updatedAt = new Date();
    }

    public Double getValue() { return value; }
    public void setValue(Double value) {
        this.value = value;
        this.updatedAt = new Date();
    }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
        this.isActionable = (suggestion != null && !suggestion.isEmpty());
        this.updatedAt = new Date();
    }

    public boolean isActionable() { return isActionable; }
    public void setActionable(boolean actionable) {
        this.isActionable = actionable;
        this.updatedAt = new Date();
    }

    public boolean isDismissed() { return isDismissed; }
    public void setDismissed(boolean dismissed) {
        this.isDismissed = dismissed;
        this.updatedAt = new Date();
    }

    public boolean isHighPriority() { return isHighPriority; }
    public void setHighPriority(boolean highPriority) {
        this.isHighPriority = highPriority;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // ✅ FIXED: Helper methods với proper logic
    public boolean isPositive() {
        return impact == Impact.POSITIVE ||
                (type == Type.POSITIVE) ||
                (type == Type.OPTIMIZATION && value != null && value > 0);
    }

    public String getFormattedValue() {
        if (value == null) return "";

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Format dựa trên type
        switch (type) {
            case FORECAST:
            case OPTIMIZATION:
                return currencyFormatter.format(Math.abs(value));

            case TREND:
                if (value > 0) {
                    return "+" + value.intValue() + "%";
                } else {
                    return value.intValue() + "%";
                }

            case PATTERN:
                return value.intValue() + " lần";

            case WARNING:
            case RISK:
                return currencyFormatter.format(Math.abs(value));

            default:
                if (value % 1 == 0) {
                    return String.valueOf(value.intValue());
                } else {
                    return currencyFormatter.format(value);
                }
        }
    }

    // Utility methods
    public String getTypeDisplayName() {
        if (type == null) return "Thông tin";

        switch (type) {
            case PATTERN: return "Mẫu chi tiêu";
            case TREND: return "Xu hướng";
            case WARNING: return "Cảnh báo";
            case RISK: return "Rủi ro";
            case OPTIMIZATION: return "Tối ưu hóa";
            case FORECAST: return "Dự báo";
            case POSITIVE: return "Tích cực";
            default: return "Thông tin";
        }
    }

    public String getImpactDisplayName() {
        if (impact == null) return "Bình thường";

        switch (impact) {
            case HIGH: return "Cao";
            case MEDIUM: return "Trung bình";
            case LOW: return "Thấp";
            case POSITIVE: return "Tích cực";
            default: return "Bình thường";
        }
    }

    @Override
    public String toString() {
        return "SmartInsight{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", impact=" + impact +
                ", value=" + value +
                ", isHighPriority=" + isHighPriority +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SmartInsight that = (SmartInsight) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}