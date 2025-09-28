package models;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {
    private Long id;
    private String type; // "income" or "expense"
    private double amount;
    private Long categoryId;
    private Long userId;
    private String note;
    private String date;
    private String paymentMethod;
    private String createdAt;
    private String updatedAt;

    // Category information (from JOIN query)
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String categoryType;

    // Formatters
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#,###,###");
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Constructors
    public Transaction() {}

    public Transaction(String type, double amount, Long categoryId, String note, String date, String paymentMethod) {
        this.type = type;
        this.amount = amount;
        this.categoryId = categoryId;
        this.note = note;
        this.date = date;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Category information getters and setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    // Utility methods
    public boolean isIncome() {
        return "income".equals(type);
    }

    public boolean isExpense() {
        return "expense".equals(type);
    }

    public String getFormattedAmount() {
        return CURRENCY_FORMATTER.format(amount) + "₫";
    }

    public String getFormattedAmountWithSign() {
        String prefix = isIncome() ? "+" : "-";
        return prefix + CURRENCY_FORMATTER.format(amount) + "₫";
    }

    public String getFormattedDate() {
        try {
            Date parsedDate = DATE_FORMATTER.parse(date);
            return DISPLAY_DATE_FORMATTER.format(parsedDate);
        } catch (ParseException e) {
            return date;
        }
    }

    public Date getParsedDate() {
        try {
            return DATE_FORMATTER.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public Date getParsedCreatedAt() {
        try {
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return fullDateFormat.parse(createdAt);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public boolean hasNote() {
        return note != null && !note.trim().isEmpty();
    }

    public boolean hasPaymentMethod() {
        return paymentMethod != null && !paymentMethod.trim().isEmpty();
    }

    public String getDisplayTitle() {
        if (hasNote()) {
            return note;
        } else {
            return categoryName;
        }
    }

    public String getAmountColor() {
        return isIncome() ? "#4CAF50" : "#F44336";
    }

    // Validation methods
    public boolean isValid() {
        return type != null &&
                (type.equals("income") || type.equals("expense")) &&
                amount > 0 &&
                categoryId != null &&
                date != null && !date.trim().isEmpty();
    }

    public String getValidationError() {
        if (type == null || (!type.equals("income") && !type.equals("expense"))) {
            return "Loại giao dịch không hợp lệ";
        }
        if (amount <= 0) {
            return "Số tiền phải lớn hơn 0";
        }
        if (categoryId == null) {
            return "Vui lòng chọn danh mục";
        }
        if (date == null || date.trim().isEmpty()) {
            return "Vui lòng chọn ngày";
        }
        return null;
    }

    // Search utility
    public boolean matchesSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();

        return (categoryName != null && categoryName.toLowerCase().contains(lowerQuery)) ||
                (note != null && note.toLowerCase().contains(lowerQuery)) ||
                (paymentMethod != null && paymentMethod.toLowerCase().contains(lowerQuery)) ||
                getFormattedAmount().contains(query);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", categoryName='" + categoryName + '\'' +
                ", date='" + date + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Transaction that = (Transaction) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // Copy constructor for editing
    public Transaction copy() {
        Transaction copy = new Transaction();
        copy.id = this.id;
        copy.type = this.type;
        copy.amount = this.amount;
        copy.categoryId = this.categoryId;
        copy.userId = this.userId;
        copy.note = this.note;
        copy.date = this.date;
        copy.paymentMethod = this.paymentMethod;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        copy.categoryName = this.categoryName;
        copy.categoryIcon = this.categoryIcon;
        copy.categoryColor = this.categoryColor;
        copy.categoryType = this.categoryType;
        return copy;
    }
}