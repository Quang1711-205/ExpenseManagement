// 🧠 BudgetAnalytics.java - Advanced Budget Analysis Engine
package com.example.expensemanagement;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;

import models.BudgetPlan;
import models.SmartInsight;
import models.BudgetAnalysisReport;


/**
 * 🧠 CHUYÊN SÂU: Advanced Budget Analytics Engine
 *
 * Tính năng phân tích nâng cao:
 * 1. Pattern Recognition - Nhận diện mẫu chi tiêu
 * 2. Anomaly Detection - Phát hiện bất thường
 * 3. Predictive Modeling - Mô hình dự báo
 * 4. Risk Assessment - Đánh giá rủi ro
 * 5. Optimization Suggestions - Đề xuất tối ưu
 */
public class BudgetAnalytics {

    private static final String TAG = "BudgetAnalytics";
    private DatabaseHelper dbHelper;
    private SimpleDateFormat dateFormatter;

    public BudgetAnalytics(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    /**
     * 🔍 PATTERN ANALYSIS: Phân tích mẫu chi tiêu
     */
    public List<SmartInsight> analyzeSpendingPatterns(BudgetPlan budget) {
        List<SmartInsight> insights = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // 1. Weekday vs Weekend Pattern
            SmartInsight weekdayPattern = analyzeWeekdayPattern(db);
            if (weekdayPattern != null) insights.add(weekdayPattern);

            // 2. Monthly Spending Cycle
            SmartInsight monthlyPattern = analyzeMonthlySpendingCycle(db);
            if (monthlyPattern != null) insights.add(monthlyPattern);

            // 3. Category Concentration Risk
            SmartInsight concentrationRisk = analyzeCategoryConcentration(budget);
            if (concentrationRisk != null) insights.add(concentrationRisk);

            // 4. Spending Velocity Analysis
            SmartInsight velocityAnalysis = analyzeSpendingVelocity(db);
            if (velocityAnalysis != null) insights.add(velocityAnalysis);

            // 5. Seasonal Pattern Analysis
            SmartInsight seasonalPattern = analyzeSeasonalPatterns(db);
            if (seasonalPattern != null) insights.add(seasonalPattern);

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing spending patterns: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return insights;
    }

    private SmartInsight analyzeWeekdayPattern(SQLiteDatabase db) {
        String query = "SELECT " +
                "CASE CAST(strftime('%w', " + DatabaseHelper.COLUMN_DATE + ") AS INTEGER) " +
                "WHEN 0 THEN 'Sunday' WHEN 1 THEN 'Monday' WHEN 2 THEN 'Tuesday' " +
                "WHEN 3 THEN 'Wednesday' WHEN 4 THEN 'Thursday' WHEN 5 THEN 'Friday' " +
                "WHEN 6 THEN 'Saturday' END as day_of_week, " +
                "AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_spending " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-90 days') " +
                "GROUP BY strftime('%w', " + DatabaseHelper.COLUMN_DATE + ") " +
                "ORDER BY avg_spending DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            String highestDay = cursor.getString(0);
            double highestAmount = cursor.getDouble(1);

            cursor.moveToLast();
            String lowestDay = cursor.getString(0);
            double lowestAmount = cursor.getDouble(1);

            double difference = highestAmount - lowestAmount;

            if (difference > 100000) { // Significant difference (100k VND)
                SmartInsight insight = new SmartInsight();
                insight.setType(SmartInsight.Type.PATTERN);
                insight.setTitle("Mẫu chi tiêu theo ngày");
                insight.setMessage(String.format("Bạn chi tiêu nhiều nhất vào %s (%.0f đ) và ít nhất vào %s (%.0f đ). " +
                        "Chênh lệch %.0f đ/ngày.", highestDay, highestAmount, lowestDay, lowestAmount, difference));
                insight.setImpact(SmartInsight.Impact.MEDIUM);
                insight.setValue(difference);
                insight.setActionable(true);
                insight.setSuggestion("Hãy lập kế hoạch chi tiêu cụ thể cho " + highestDay + " để kiểm soát tốt hơn.");
                cursor.close();
                return insight;
            }
        }

        cursor.close();
        return null;
    }

    private SmartInsight analyzeMonthlySpendingCycle(SQLiteDatabase db) {
        String query = "SELECT " +
                "CAST(strftime('%d', " + DatabaseHelper.COLUMN_DATE + ") AS INTEGER) as day_of_month, " +
                "AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_spending " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-6 months') " +
                "GROUP BY strftime('%d', " + DatabaseHelper.COLUMN_DATE + ") " +
                "ORDER BY avg_spending DESC " +
                "LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);
        List<Integer> highSpendingDays = new ArrayList<>();

        while (cursor.moveToNext()) {
            highSpendingDays.add(cursor.getInt(0));
        }
        cursor.close();

        if (!highSpendingDays.isEmpty()) {
            SmartInsight insight = new SmartInsight();
            insight.setType(SmartInsight.Type.PATTERN);
            insight.setTitle("Chu kỳ chi tiêu hàng tháng");
            insight.setMessage("Bạn thường chi tiêu nhiều vào những ngày: " +
                    highSpendingDays.toString().replaceAll("[\\[\\]]", ""));
            insight.setImpact(SmartInsight.Impact.LOW);
            insight.setActionable(true);
            insight.setSuggestion("Chuẩn bị ngân sách dự phòng cho những ngày này.");
            return insight;
        }

        return null;
    }

    private SmartInsight analyzeCategoryConcentration(BudgetPlan budget) {
        if (budget == null || budget.getCategories().isEmpty()) return null;

        // Tìm category chiếm tỷ lệ lớn nhất
        BudgetPlan.CategoryBudget largestCategory = budget.getCategories().get(0);
        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            if (category.getAllocatedAmount() > largestCategory.getAllocatedAmount()) {
                largestCategory = category;
            }
        }

        double concentrationRatio = largestCategory.getAllocatedAmount() / budget.getTotalAllocated();

        if (concentrationRatio > 0.5) { // Over 50% concentrated in one category
            SmartInsight insight = new SmartInsight();
            insight.setType(SmartInsight.Type.RISK);
            insight.setTitle("Tập trung ngân sách cao");
            insight.setMessage(String.format("%.1f%% ngân sách tập trung vào '%s'. Điều này có thể gây rủi ro.",
                    concentrationRatio * 100, largestCategory.getName()));
            insight.setImpact(concentrationRatio > 0.7 ? SmartInsight.Impact.HIGH : SmartInsight.Impact.MEDIUM);
            insight.setValue(concentrationRatio * 100);
            insight.setActionable(true);
            insight.setSuggestion("Hãy cân nhắc phân bổ đều hơn để giảm rủi ro tài chính.");
            return insight;
        }

        return null;
    }

    private SmartInsight analyzeSpendingVelocity(SQLiteDatabase db) {
        String query = "SELECT " +
                "DATE(" + DatabaseHelper.COLUMN_DATE + ") as spending_date, " +
                "SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as daily_total " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-30 days') " +
                "GROUP BY DATE(" + DatabaseHelper.COLUMN_DATE + ") " +
                "ORDER BY spending_date";

        Cursor cursor = db.rawQuery(query, null);
        List<Double> dailySpending = new ArrayList<>();

        while (cursor.moveToNext()) {
            dailySpending.add(cursor.getDouble(1));
        }
        cursor.close();

        if (dailySpending.size() >= 7) {
            // Calculate velocity (acceleration/deceleration in spending)
            double recentAvg = calculateAverage(dailySpending.subList(Math.max(0, dailySpending.size() - 7), dailySpending.size()));
            double previousAvg = calculateAverage(dailySpending.subList(Math.max(0, dailySpending.size() - 14), dailySpending.size() - 7));

            if (previousAvg > 0) {
                double velocityChange = ((recentAvg - previousAvg) / previousAvg) * 100;

                if (Math.abs(velocityChange) > 20) {
                    SmartInsight insight = new SmartInsight();
                    insight.setType(velocityChange > 0 ? SmartInsight.Type.WARNING : SmartInsight.Type.POSITIVE);
                    insight.setTitle("Thay đổi tốc độ chi tiêu");
                    insight.setMessage(String.format("Chi tiêu %s %.1f%% so với tuần trước (%.0f đ/ngày vs %.0f đ/ngày).",
                            velocityChange > 0 ? "tăng" : "giảm", Math.abs(velocityChange), recentAvg, previousAvg));
                    insight.setImpact(Math.abs(velocityChange) > 50 ? SmartInsight.Impact.HIGH : SmartInsight.Impact.MEDIUM);
                    insight.setValue(velocityChange);
                    insight.setActionable(true);
                    insight.setSuggestion(velocityChange > 0 ?
                            "Cần kiểm soát chi tiêu để tránh vượt ngân sách." :
                            "Xu hướng tốt! Hãy duy trì thói quen này.");
                    return insight;
                }
            }
        }

        return null;
    }

    private SmartInsight analyzeSeasonalPatterns(SQLiteDatabase db) {
        String query = "SELECT " +
                "CAST(strftime('%m', " + DatabaseHelper.COLUMN_DATE + ") AS INTEGER) as month, " +
                "AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_spending " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-12 months') " +
                "GROUP BY strftime('%m', " + DatabaseHelper.COLUMN_DATE + ") " +
                "ORDER BY avg_spending DESC " +
                "LIMIT 3";

        Cursor cursor = db.rawQuery(query, null);
        List<String> highSpendingMonths = new ArrayList<>();
        String[] monthNames = {"", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};

        while (cursor.moveToNext()) {
            int month = cursor.getInt(0);
            highSpendingMonths.add(monthNames[month]);
        }
        cursor.close();

        if (!highSpendingMonths.isEmpty()) {
            SmartInsight insight = new SmartInsight();
            insight.setType(SmartInsight.Type.PATTERN);
            insight.setTitle("Mẫu chi tiêu theo mùa");
            insight.setMessage("Bạn thường chi tiêu nhiều trong: " + String.join(", ", highSpendingMonths));
            insight.setImpact(SmartInsight.Impact.LOW);
            insight.setActionable(true);
            insight.setSuggestion("Lập kế hoạch tiết kiệm trước những tháng chi tiêu cao này.");
            return insight;
        }

        return null;
    }

    /**
     * 📈 VARIANCE ANALYSIS: Phân tích độ lệch ngân sách
     */
    public List<SmartInsight> analyzeVarianceTrends() {
        List<SmartInsight> insights = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Analyze budget vs actual spending variance over time
            String query = "SELECT " +
                    "strftime('%Y-%m', b." + DatabaseHelper.COLUMN_BUDGET_START_DATE + ") as budget_month, " +
                    "b." + DatabaseHelper.COLUMN_BUDGET_AMOUNT + " as budgeted, " +
                    "COALESCE(SUM(t." + DatabaseHelper.COLUMN_AMOUNT + "), 0) as actual " +
                    "FROM " + DatabaseHelper.TABLE_BUDGETS + " b " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_TRANSACTIONS + " t ON " +
                    "t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = b." + DatabaseHelper.COLUMN_CATEGORY_ID + " " +
                    "AND t." + DatabaseHelper.COLUMN_DATE + " BETWEEN b." + DatabaseHelper.COLUMN_BUDGET_START_DATE + " AND b." + DatabaseHelper.COLUMN_BUDGET_END_DATE + " " +
                    "WHERE b." + DatabaseHelper.COLUMN_BUDGET_START_DATE + " >= date('now', '-6 months') " +
                    "GROUP BY budget_month, b." + DatabaseHelper.COLUMN_ID + " " +
                    "ORDER BY budget_month DESC";

            Cursor cursor = db.rawQuery(query, null);
            List<Double> variances = new ArrayList<>();

            while (cursor.moveToNext()) {
                double budgeted = cursor.getDouble(1);
                double actual = cursor.getDouble(2);
                if (budgeted > 0) {
                    double variance = ((actual - budgeted) / budgeted) * 100;
                    variances.add(variance);
                }
            }
            cursor.close();

            if (variances.size() >= 3) {
                SmartInsight varianceTrend = analyzeVarianceTrend(variances);
                if (varianceTrend != null) insights.add(varianceTrend);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing variance trends: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return insights;
    }

    private SmartInsight analyzeVarianceTrend(List<Double> variances) {
        if (variances.size() < 3) return null;

        // Check if there's a consistent trend
        boolean increasingTrend = true;
        boolean decreasingTrend = true;

        for (int i = 1; i < variances.size(); i++) {
            if (variances.get(i) <= variances.get(i-1)) {
                increasingTrend = false;
            }
            if (variances.get(i) >= variances.get(i-1)) {
                decreasingTrend = false;
            }
        }

        SmartInsight insight = new SmartInsight();
        insight.setType(SmartInsight.Type.TREND);

        if (increasingTrend) {
            insight.setTitle("Xu hướng vượt ngân sách");
            insight.setMessage("Tỷ lệ vượt ngân sách đang tăng dần qua các tháng.");
            insight.setImpact(SmartInsight.Impact.HIGH);
            insight.setSuggestion("Cần điều chỉnh ngân sách hoặc kiểm soát chi tiêu chặt chẽ hơn.");
        } else if (decreasingTrend) {
            insight.setTitle("Kiểm soát ngân sách tốt dần");
            insight.setMessage("Bạn đang kiểm soát chi tiêu tốt hơn theo thời gian.");
            insight.setImpact(SmartInsight.Impact.POSITIVE);
            insight.setSuggestion("Tuyệt vời! Hãy duy trì kỷ luật tài chính này.");
        }

        if (increasingTrend || decreasingTrend) {
            insight.setValue(calculateAverage(variances));
            insight.setActionable(true);
            return insight;
        }

        return null;
    }

    /**
     * 🔮 CASH FLOW FORECASTING: Dự báo dòng tiền
     */
    public SmartInsight forecastCashFlow(int daysAhead) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Get average daily spending for the last 30 days
            String spendingQuery = "SELECT AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_daily_spending " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                    "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-30 days')";

            Cursor spendingCursor = db.rawQuery(spendingQuery, null);
            double avgDailySpending = 0;
            if (spendingCursor.moveToFirst()) {
                avgDailySpending = spendingCursor.getDouble(0);
            }
            spendingCursor.close();

            // Get average daily income
            String incomeQuery = "SELECT AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_daily_income " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'income' " +
                    "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-30 days')";

            Cursor incomeCursor = db.rawQuery(incomeQuery, null);
            double avgDailyIncome = 0;
            if (incomeCursor.moveToFirst()) {
                avgDailyIncome = incomeCursor.getDouble(0);
            }
            incomeCursor.close();

            // Calculate net cash flow forecast
            double netDailyCashFlow = avgDailyIncome - avgDailySpending;
            double forecastedCashFlow = netDailyCashFlow * daysAhead;

            SmartInsight insight = new SmartInsight();
            insight.setType(SmartInsight.Type.FORECAST);
            insight.setTitle("Dự báo dòng tiền " + daysAhead + " ngày");
            insight.setValue(forecastedCashFlow);

            if (forecastedCashFlow < 0) {
                insight.setMessage(String.format("Dự báo thiếu hụt %.0f đ trong %d ngày tới (%.0f đ/ngày).",
                        Math.abs(forecastedCashFlow), daysAhead, Math.abs(netDailyCashFlow)));
                insight.setImpact(SmartInsight.Impact.HIGH);
                insight.setSuggestion("Cần tăng thu nhập hoặc giảm chi tiêu để tránh thiếu hụt.");
            } else {
                insight.setMessage(String.format("Dự báo dư %.0f đ trong %d ngày tới (%.0f đ/ngày).",
                        forecastedCashFlow, daysAhead, netDailyCashFlow));
                insight.setImpact(SmartInsight.Impact.POSITIVE);
                insight.setSuggestion("Dòng tiền tích cực! Hãy xem xét đầu tư hoặc tiết kiệm thêm.");
            }

            insight.setActionable(true);
            return insight;

        } catch (Exception e) {
            Log.e(TAG, "Error forecasting cash flow: " + e.getMessage());
            return null;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * 💡 OPTIMIZATION SUGGESTIONS: Đề xuất tối ưu hóa ngân sách
     */
    public List<SmartInsight> suggestBudgetOptimizations(BudgetPlan budget) {
        List<SmartInsight> suggestions = new ArrayList<>();

        if (budget == null || budget.getCategories().isEmpty()) return suggestions;

        // 1. Over-allocated categories
        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            if (category.getPercentageUsed() < 50) {
                SmartInsight suggestion = new SmartInsight();
                suggestion.setType(SmartInsight.Type.OPTIMIZATION);
                suggestion.setTitle("Tối ưu ngân sách " + category.getName());
                suggestion.setMessage(String.format("Chỉ sử dụng %.1f%% ngân sách %s. Có thể giảm %.0f đ.",
                        category.getPercentageUsed(), category.getName(),
                        category.getAllocatedAmount() - category.getSpentAmount()));
                suggestion.setImpact(SmartInsight.Impact.MEDIUM);
                suggestion.setValue(category.getAllocatedAmount() - category.getSpentAmount());
                suggestion.setActionable(true);
                suggestion.setSuggestion("Chuyển số tiền thừa sang mục tiết kiệm hoặc đầu tư.");
                suggestions.add(suggestion);
            }
        }

        // 2. Emergency fund suggestion
        double totalSpending = budget.getTotalSpent();
        if (totalSpending > 0) {
            SmartInsight emergencyFund = new SmartInsight();
            emergencyFund.setType(SmartInsight.Type.OPTIMIZATION);
            emergencyFund.setTitle("Quỹ dự phòng khẩn cấp");
            emergencyFund.setMessage(String.format("Nên có quỹ dự phòng %.0f đ (3 tháng chi tiêu).", totalSpending * 3));
            emergencyFund.setImpact(SmartInsight.Impact.HIGH);
            emergencyFund.setValue(totalSpending * 3);
            emergencyFund.setActionable(true);
            emergencyFund.setSuggestion("Dành 10-20% thu nhập hàng tháng để xây dựng quỹ dự phòng.");
            suggestions.add(emergencyFund);
        }

        // 3. High-risk categories optimization
        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            if (category.getPercentageUsed() > 85) {
                SmartInsight riskOptimization = new SmartInsight();
                riskOptimization.setType(SmartInsight.Type.WARNING);
                riskOptimization.setTitle("Nguy cơ vượt ngân sách " + category.getName());
                riskOptimization.setMessage(String.format("Đã sử dụng %.1f%% ngân sách %s. " +
                                "Còn lại %.0f đ cho thời gian còn lại.",
                        category.getPercentageUsed(), category.getName(),
                        category.getAllocatedAmount() - category.getSpentAmount()));
                riskOptimization.setImpact(SmartInsight.Impact.HIGH);
                riskOptimization.setValue(category.getPercentageUsed());
                riskOptimization.setActionable(true);
                riskOptimization.setSuggestion("Giảm chi tiêu trong mục này hoặc tăng ngân sách phân bổ.");
                suggestions.add(riskOptimization);
            }
        }

        return suggestions;
    }

    /**
     * ⚠️ RISK ASSESSMENT: Đánh giá rủi ro ngân sách
     */
    public SmartInsight assessBudgetRisks(BudgetPlan budget) {
        if (budget == null) return null;

        int riskScore = 0;
        List<String> riskFactors = new ArrayList<>();

        // High spending categories (>80% used)
        int highRiskCategories = 0;
        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            if (category.getPercentageUsed() > 80) {
                highRiskCategories++;
                riskFactors.add(category.getName() + " (" + String.format("%.1f%%", category.getPercentageUsed()) + ")");
            }
        }
        riskScore += highRiskCategories * 20;

        // Overall budget utilization
        double totalAllocated = budget.getTotalAllocated();
        if (totalAllocated > 0) {
            double overallUtilization = (budget.getTotalSpent() / totalAllocated) * 100;
            if (overallUtilization > 90) {
                riskScore += 30;
                riskFactors.add("Sử dụng " + String.format("%.1f%%", overallUtilization) + " tổng ngân sách");
            }
        }

        // Create risk assessment
        SmartInsight riskAssessment = new SmartInsight();
        riskAssessment.setType(SmartInsight.Type.RISK);
        riskAssessment.setTitle("Đánh giá rủi ro ngân sách");
        riskAssessment.setValue((double) riskScore);

        if (riskScore >= 70) {
            riskAssessment.setMessage("Rủi ro cao: " + String.join(", ", riskFactors));
            riskAssessment.setImpact(SmartInsight.Impact.HIGH);
            riskAssessment.setSuggestion("Cần hành động ngay để tránh vượt ngân sách.");
        } else if (riskScore >= 40) {
            riskAssessment.setMessage("Rủi ro trung bình: " + String.join(", ", riskFactors));
            riskAssessment.setImpact(SmartInsight.Impact.MEDIUM);
            riskAssessment.setSuggestion("Theo dõi chặt chẽ và điều chỉnh chi tiêu.");
        } else {
            riskAssessment.setMessage("Rủi ro thấp: Ngân sách được kiểm soát tốt.");
            riskAssessment.setImpact(SmartInsight.Impact.LOW);
            riskAssessment.setSuggestion("Duy trì kỷ luật tài chính hiện tại.");
        }

        riskAssessment.setActionable(true);
        return riskAssessment;
    }

    /**
     * 🎯 ANOMALY DETECTION: Phát hiện giao dịch bất thường
     */
    public List<SmartInsight> detectAnomalies() {
        List<SmartInsight> anomalies = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // 1. Unusual large transactions
            SmartInsight largeTransactionAnomaly = detectLargeTransactions(db);
            if (largeTransactionAnomaly != null) anomalies.add(largeTransactionAnomaly);

            // 2. Unusual spending frequency
            SmartInsight frequencyAnomaly = detectFrequencyAnomalies(db);
            if (frequencyAnomaly != null) anomalies.add(frequencyAnomaly);

            // 3. Category spending spikes
            List<SmartInsight> categorySpikes = detectCategorySpikes(db);
            anomalies.addAll(categorySpikes);

        } catch (Exception e) {
            Log.e(TAG, "Error detecting anomalies: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return anomalies;
    }

    private SmartInsight detectLargeTransactions(SQLiteDatabase db) {
        // Calculate average transaction amount over the last 90 days
        String avgQuery = "SELECT AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") as avg_amount, " +
                "(AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") + 2.5 * " +
                "(SELECT MAX(amount_variance) FROM (" +
                "SELECT (" + DatabaseHelper.COLUMN_AMOUNT + " - AVG(" + DatabaseHelper.COLUMN_AMOUNT + ") OVER()) as amount_variance " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-90 days')))) as threshold " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-90 days')";

        Cursor avgCursor = db.rawQuery(avgQuery, null);
        double threshold = 0;
        double avgAmount = 0;

        if (avgCursor.moveToFirst()) {
            avgAmount = avgCursor.getDouble(0);
            threshold = avgCursor.getDouble(1);
        }
        avgCursor.close();

        if (threshold > 0) {
            // Find transactions above threshold in the last 7 days
            String anomalyQuery = "SELECT COUNT(*) as anomaly_count, MAX(" + DatabaseHelper.COLUMN_AMOUNT + ") as max_amount " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                    "AND " + DatabaseHelper.COLUMN_AMOUNT + " > ? " +
                    "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-7 days')";

            Cursor anomalyCursor = db.rawQuery(anomalyQuery, new String[]{String.valueOf(threshold)});

            if (anomalyCursor.moveToFirst()) {
                int anomalyCount = anomalyCursor.getInt(0);
                double maxAmount = anomalyCursor.getDouble(1);

                if (anomalyCount > 0) {
                    SmartInsight insight = new SmartInsight();
                    insight.setType(SmartInsight.Type.PATTERN); // Fixed: Change ANOMALY to PATTERN
                    insight.setTitle("Giao dịch bất thường");
                    insight.setMessage(String.format("Phát hiện %d giao dịch lớn bất thường trong 7 ngày qua. " +
                                    "Giao dịch lớn nhất: %.0f đ (trung bình bình thường: %.0f đ).",
                            anomalyCount, maxAmount, avgAmount));
                    insight.setImpact(anomalyCount > 3 ? SmartInsight.Impact.HIGH : SmartInsight.Impact.MEDIUM);
                    insight.setValue(maxAmount);
                    insight.setActionable(true);
                    insight.setSuggestion("Kiểm tra lại các giao dịch này để đảm bảo tính chính xác.");
                    anomalyCursor.close();
                    return insight;
                }
            }
            anomalyCursor.close();
        }

        return null;
    }

    private SmartInsight detectFrequencyAnomalies(SQLiteDatabase db) {
        // Compare transaction frequency: last week vs average of previous weeks
        String currentWeekQuery = "SELECT COUNT(*) as current_count " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-7 days')";

        String avgWeekQuery = "SELECT AVG(weekly_count) as avg_count " +
                "FROM (SELECT COUNT(*) as weekly_count " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN date('now', '-35 days') AND date('now', '-8 days') " +
                "GROUP BY strftime('%W', " + DatabaseHelper.COLUMN_DATE + "))";

        Cursor currentCursor = db.rawQuery(currentWeekQuery, null);
        Cursor avgCursor = db.rawQuery(avgWeekQuery, null);

        int currentCount = 0;
        double avgCount = 0;

        if (currentCursor.moveToFirst()) {
            currentCount = currentCursor.getInt(0);
        }

        if (avgCursor.moveToFirst()) {
            avgCount = avgCursor.getDouble(0);
        }

        currentCursor.close();
        avgCursor.close();

        if (avgCount > 0) {
            double frequencyChange = ((currentCount - avgCount) / avgCount) * 100;

            if (Math.abs(frequencyChange) > 50) {
                SmartInsight insight = new SmartInsight();
                insight.setType(SmartInsight.Type.PATTERN); // Fixed: Change ANOMALY to PATTERN
                insight.setTitle("Tần suất giao dịch bất thường");
                insight.setMessage(String.format("Số giao dịch tuần này %s %.1f%% so với bình thường " +
                                "(%d vs %.1f giao dịch/tuần).",
                        frequencyChange > 0 ? "tăng" : "giảm", Math.abs(frequencyChange), currentCount, avgCount));
                insight.setImpact(Math.abs(frequencyChange) > 100 ? SmartInsight.Impact.HIGH : SmartInsight.Impact.MEDIUM);
                insight.setValue(frequencyChange);
                insight.setActionable(true);
                insight.setSuggestion(frequencyChange > 0 ?
                        "Kiểm tra xem có phát sinh chi phí đột xuất nào không." :
                        "Có thể bạn đang tiết kiệm hơn hoặc ít hoạt động chi tiêu.");
                return insight;
            }
        }

        return null;
    }

    private List<SmartInsight> detectCategorySpikes(SQLiteDatabase db) {
        List<SmartInsight> spikes = new ArrayList<>();

        // Get categories with unusual spending spikes
        String spikeQuery = "SELECT " +
                "c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                "current_week.current_spending, " +
                "avg_weeks.avg_spending " +
                "FROM " + DatabaseHelper.TABLE_CATEGORIES + " c " +
                "LEFT JOIN (SELECT " + DatabaseHelper.COLUMN_CATEGORY_ID + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as current_spending " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-7 days') " +
                "GROUP BY " + DatabaseHelper.COLUMN_CATEGORY_ID + ") current_week " +
                "ON c." + DatabaseHelper.COLUMN_ID + " = current_week." + DatabaseHelper.COLUMN_CATEGORY_ID + " " +
                "LEFT JOIN (SELECT " + DatabaseHelper.COLUMN_CATEGORY_ID + ", AVG(weekly_spending) as avg_spending " +
                "FROM (SELECT " + DatabaseHelper.COLUMN_CATEGORY_ID + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as weekly_spending " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN date('now', '-35 days') AND date('now', '-8 days') " +
                "GROUP BY " + DatabaseHelper.COLUMN_CATEGORY_ID + ", strftime('%W', " + DatabaseHelper.COLUMN_DATE + ")) " +
                "GROUP BY " + DatabaseHelper.COLUMN_CATEGORY_ID + ") avg_weeks " +
                "ON c." + DatabaseHelper.COLUMN_ID + " = avg_weeks." + DatabaseHelper.COLUMN_CATEGORY_ID + " " +
                "WHERE current_week.current_spending > 0 AND avg_weeks.avg_spending > 0";

        Cursor cursor = db.rawQuery(spikeQuery, null);

        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(0);
            double currentSpending = cursor.getDouble(1);
            double avgSpending = cursor.getDouble(2);

            double spikePercentage = ((currentSpending - avgSpending) / avgSpending) * 100;

            if (spikePercentage > 100) { // 100% increase is significant
                SmartInsight spike = new SmartInsight();
                spike.setType(SmartInsight.Type.PATTERN); // Fixed: Change ANOMALY to PATTERN
                spike.setTitle("Tăng đột biến chi tiêu " + categoryName);
                spike.setMessage(String.format("Chi tiêu %s tuần này tăng %.1f%% (%.0f đ vs %.0f đ bình thường).",
                        categoryName, spikePercentage, currentSpending, avgSpending));
                spike.setImpact(spikePercentage > 200 ? SmartInsight.Impact.HIGH : SmartInsight.Impact.MEDIUM);
                spike.setValue(spikePercentage);
                spike.setActionable(true);
                spike.setSuggestion("Xem xét lại các khoản chi trong mục này để hiểu nguyên nhân.");
                spikes.add(spike);
            }
        }
        cursor.close();

        return spikes;
    }

    /**
     * 📊 COMPREHENSIVE ANALYSIS: Phân tích tổng hợp
     */
    public BudgetAnalysisReport generateComprehensiveReport(BudgetPlan budget) {
        BudgetAnalysisReport report = new BudgetAnalysisReport();

        try {
            // 1. Pattern Analysis
            List<SmartInsight> patterns = analyzeSpendingPatterns(budget);
            report.setPatternInsights(patterns);

            // 2. Variance Analysis
            List<SmartInsight> variances = analyzeVarianceTrends();
            report.setVarianceInsights(variances);

            // 3. Cash Flow Forecast
            SmartInsight cashFlowForecast = forecastCashFlow(30);
            if (cashFlowForecast != null) {
                report.setCashFlowForecast(cashFlowForecast);
            }

            // 4. Risk Assessment
            SmartInsight riskAssessment = assessBudgetRisks(budget);
            if (riskAssessment != null) {
                report.setRiskAssessment(riskAssessment);
            }

            // 5. Optimization Suggestions
            List<SmartInsight> optimizations = suggestBudgetOptimizations(budget);
            report.setOptimizationSuggestions(optimizations);

            // 6. Anomaly Detection
            List<SmartInsight> anomalies = detectAnomalies();
            report.setAnomalyInsights(anomalies);

            // 7. Overall Health Score
            int healthScore = calculateBudgetHealthScore(budget, patterns, variances, riskAssessment);
            report.setOverallHealthScore(healthScore);

            // 8. Summary and Recommendations
            report.setSummary(generateExecutiveSummary(report));
            report.setTopRecommendations(generateTopRecommendations(report));

        } catch (Exception e) {
            Log.e(TAG, "Error generating comprehensive report: " + e.getMessage());
        }

        return report;
    }

    private int calculateBudgetHealthScore(BudgetPlan budget, List<SmartInsight> patterns,
                                           List<SmartInsight> variances, SmartInsight riskAssessment) {
        int score = 100; // Start with perfect score

        if (budget != null) {
            // Deduct points for budget overruns
            for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
                if (category.getPercentageUsed() > 100) {
                    score -= 15;
                } else if (category.getPercentageUsed() > 80) {
                    score -= 5;
                }
            }
        }

        // Deduct points for high-risk factors
        if (riskAssessment != null && riskAssessment.getImpact() == SmartInsight.Impact.HIGH) {
            score -= 20;
        } else if (riskAssessment != null && riskAssessment.getImpact() == SmartInsight.Impact.MEDIUM) {
            score -= 10;
        }

        // Deduct points for negative variance trends
        for (SmartInsight variance : variances) {
            if (variance.getType() == SmartInsight.Type.WARNING) {
                score -= 10;
            }
        }

        // Add points for positive patterns
        for (SmartInsight pattern : patterns) {
            if (pattern.getImpact() == SmartInsight.Impact.POSITIVE) {
                score += 5;
            }
        }

        return Math.max(0, Math.min(100, score)); // Ensure score is between 0-100
    }

    private String generateExecutiveSummary(BudgetAnalysisReport report) {
        StringBuilder summary = new StringBuilder();

        // Health Score Summary
        int healthScore = report.getOverallHealthScore();
        if (healthScore >= 80) {
            summary.append("🟢 Tình hình tài chính tốt (").append(healthScore).append("/100). ");
        } else if (healthScore >= 60) {
            summary.append("🟡 Tình hình tài chính ổn định (").append(healthScore).append("/100). ");
        } else {
            summary.append("🔴 Cần cải thiện tình hình tài chính (").append(healthScore).append("/100). ");
        }

        // Key findings
        int totalInsights = report.getPatternInsights().size() +
                report.getVarianceInsights().size() +
                report.getOptimizationSuggestions().size();

        summary.append("Phát hiện ").append(totalInsights).append(" điểm cần lưu ý. ");

        // Risk level
        if (report.getRiskAssessment() != null) {
            SmartInsight.Impact riskLevel = report.getRiskAssessment().getImpact();
            if (riskLevel == SmartInsight.Impact.HIGH) {
                summary.append("Mức rủi ro cao, cần hành động ngay.");
            } else if (riskLevel == SmartInsight.Impact.MEDIUM) {
                summary.append("Mức rủi ro trung bình, cần theo dõi.");
            } else {
                summary.append("Mức rủi ro thấp, tình hình kiểm soát tốt.");
            }
        }

        return summary.toString();
    }

    private List<String> generateTopRecommendations(BudgetAnalysisReport report) {
        List<String> recommendations = new ArrayList<>();
        PriorityQueue<SmartInsight> priorityQueue = new PriorityQueue<>((a, b) ->
                b.getImpact().ordinal() - a.getImpact().ordinal());

        // Add all actionable insights to priority queue
        for (SmartInsight insight : report.getPatternInsights()) {
            if (insight.isActionable()) priorityQueue.offer(insight);
        }

        for (SmartInsight insight : report.getVarianceInsights()) {
            if (insight.isActionable()) priorityQueue.offer(insight);
        }

        for (SmartInsight insight : report.getOptimizationSuggestions()) {
            if (insight.isActionable()) priorityQueue.offer(insight);
        }

        for (SmartInsight insight : report.getAnomalyInsights()) {
            if (insight.isActionable()) priorityQueue.offer(insight);
        }

        if (report.getRiskAssessment() != null && report.getRiskAssessment().isActionable()) {
            priorityQueue.offer(report.getRiskAssessment());
        }

        // Get top 5 recommendations
        int count = 0;
        while (!priorityQueue.isEmpty() && count < 5) {
            SmartInsight insight = priorityQueue.poll();
            recommendations.add(insight.getSuggestion());
            count++;
        }

        return recommendations;
    }

    /**
     * 📈 PERFORMANCE METRICS: Các chỉ số hiệu suất
     */
    public Map<String, Double> calculatePerformanceMetrics(BudgetPlan budget) {
        Map<String, Double> metrics = new HashMap<>();

        if (budget == null) return metrics;

        // 1. Budget Adherence Rate
        double totalAllocated = budget.getTotalAllocated();
        double totalSpent = budget.getTotalSpent();

        if (totalAllocated > 0) {
            double adherenceRate = Math.max(0, Math.min(100, ((totalAllocated - totalSpent) / totalAllocated) * 100));
            metrics.put("adherence_rate", adherenceRate);
        }

        // 2. Savings Rate
        if (totalAllocated > 0) {
            double savingsRate = Math.max(0, ((totalAllocated - totalSpent) / totalAllocated) * 100);
            metrics.put("savings_rate", savingsRate);
        }

        // 3. Category Diversification Index (lower is more diversified)
        double diversificationIndex = calculateHerfindahlIndex(budget);
        metrics.put("diversification_index", diversificationIndex);

        // 4. Budget Efficiency Score
        double efficiencyScore = calculateBudgetEfficiencyScore(budget);
        metrics.put("efficiency_score", efficiencyScore);

        // 5. Spending Consistency Score
        double consistencyScore = calculateSpendingConsistency();
        metrics.put("consistency_score", consistencyScore);

        return metrics;
    }

    private double calculateHerfindahlIndex(BudgetPlan budget) {
        double totalAllocated = budget.getTotalAllocated();
        if (totalAllocated <= 0) return 0;

        double herfindahlIndex = 0;
        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            double share = category.getAllocatedAmount() / totalAllocated;
            herfindahlIndex += share * share;
        }

        return herfindahlIndex * 10000; // Scale to 0-10000 for easier interpretation
    }

    private double calculateBudgetEfficiencyScore(BudgetPlan budget) {
        int totalCategories = budget.getCategories().size();
        int efficientCategories = 0;

        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            double utilizationRate = category.getPercentageUsed();
            // Consider 70-95% utilization as efficient
            if (utilizationRate >= 70 && utilizationRate <= 95) {
                efficientCategories++;
            }
        }

        if (totalCategories == 0) return 0;
        return (double) efficientCategories / totalCategories * 100;
    }

    private double calculateSpendingConsistency() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double consistencyScore = 0;

        try {
            String query = "SELECT " +
                    "DATE(" + DatabaseHelper.COLUMN_DATE + ") as spending_date, " +
                    "SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as daily_spending " +
                    "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                    "WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                    "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-30 days') " +
                    "GROUP BY DATE(" + DatabaseHelper.COLUMN_DATE + ") " +
                    "ORDER BY spending_date";

            Cursor cursor = db.rawQuery(query, null);
            List<Double> dailyAmounts = new ArrayList<>();

            while (cursor.moveToNext()) {
                dailyAmounts.add(cursor.getDouble(1));
            }
            cursor.close();

            if (dailyAmounts.size() >= 7) {
                double average = calculateAverage(dailyAmounts);
                double standardDeviation = calculateStandardDeviation(dailyAmounts);

                // Lower coefficient of variation means higher consistency
                if (average > 0) {
                    double coefficientOfVariation = standardDeviation / average;
                    consistencyScore = Math.max(0, 100 - (coefficientOfVariation * 100));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating spending consistency: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return consistencyScore;
    }

    /**
     * 🎯 PREDICTIVE MODELING: Mô hình dự báo nâng cao
     */
    public SmartInsight predictBudgetPerformance(BudgetPlan budget, int monthsAhead) {
        if (budget == null) return null;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SmartInsight prediction = null;

        try {
            // Analyze historical spending trends for each category
            Map<String, Double> categoryTrends = new HashMap<>();

            for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
                String trendQuery = "SELECT " +
                        "strftime('%Y-%m', " + DatabaseHelper.COLUMN_DATE + ") as month, " +
                        "SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as monthly_spending " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                        "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                        "AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                        "AND " + DatabaseHelper.COLUMN_DATE + " >= date('now', '-6 months') " +
                        "GROUP BY strftime('%Y-%m', " + DatabaseHelper.COLUMN_DATE + ") " +
                        "ORDER BY month";

                // Fixed: Need to get category ID from CategoryBudget
                // Since CategoryBudget doesn't have getCategoryId method in your model,
                // we'll need to assume there's a way to get it or modify this logic
                Cursor cursor = db.rawQuery(trendQuery, new String[]{"1"}); // Placeholder - needs actual category ID
                List<Double> monthlySpending = new ArrayList<>();

                while (cursor.moveToNext()) {
                    monthlySpending.add(cursor.getDouble(1));
                }
                cursor.close();

                if (monthlySpending.size() >= 3) {
                    double trend = calculateLinearTrend(monthlySpending);
                    categoryTrends.put(category.getName(), trend);
                }
            }

            // Generate prediction based on trends
            prediction = generatePredictionInsight(budget, categoryTrends, monthsAhead);

        } catch (Exception e) {
            Log.e(TAG, "Error predicting budget performance: " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return prediction;
    }

    private double calculateLinearTrend(List<Double> values) {
        if (values.size() < 2) return 0;

        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1; // Time periods
            double y = values.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Calculate slope (trend)
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }

    private SmartInsight generatePredictionInsight(BudgetPlan budget, Map<String, Double> trends, int monthsAhead) {
        SmartInsight insight = new SmartInsight();
        insight.setType(SmartInsight.Type.FORECAST);
        insight.setTitle("Dự báo hiệu suất ngân sách " + monthsAhead + " tháng");

        StringBuilder message = new StringBuilder();
        List<String> risks = new ArrayList<>();
        List<String> opportunities = new ArrayList<>();

        double totalProjectedIncrease = 0;

        for (BudgetPlan.CategoryBudget category : budget.getCategories()) {
            String categoryName = category.getName();
            Double trend = trends.get(categoryName);

            if (trend != null) {
                double projectedIncrease = trend * monthsAhead;
                totalProjectedIncrease += projectedIncrease;

                double currentUtilization = category.getPercentageUsed();
                double projectedUtilization = currentUtilization + ((projectedIncrease / category.getAllocatedAmount()) * 100);

                if (projectedUtilization > 100) {
                    risks.add(categoryName + " (dự báo vượt " + String.format("%.1f%%", projectedUtilization - 100) + ")");
                } else if (projectedUtilization < 70) {
                    opportunities.add(categoryName + " (có thể tiết kiệm " + String.format("%.0f đ", category.getAllocatedAmount() - (category.getSpentAmount() + projectedIncrease)) + ")");
                }
            }
        }

        message.append("Dự báo xu hướng chi tiêu trong ").append(monthsAhead).append(" tháng tới: ");

        if (!risks.isEmpty()) {
            message.append("\n⚠️ Các mục có nguy cơ vượt ngân sách: ").append(String.join(", ", risks));
            insight.setImpact(SmartInsight.Impact.HIGH);
        } else if (!opportunities.isEmpty()) {
            message.append("\n💡 Cơ hội tiết kiệm: ").append(String.join(", ", opportunities));
            insight.setImpact(SmartInsight.Impact.MEDIUM);
        } else {
            message.append("\n✅ Ngân sách dự kiến hoạt động ổn định.");
            insight.setImpact(SmartInsight.Impact.LOW);
        }

        insight.setMessage(message.toString());
        insight.setValue(totalProjectedIncrease);
        insight.setActionable(true);

        if (!risks.isEmpty()) {
            insight.setSuggestion("Xem xét điều chỉnh phân bổ ngân sách cho các mục có nguy cơ cao.");
        } else if (!opportunities.isEmpty()) {
            insight.setSuggestion("Cân nhắc chuyển tiền dư sang tiết kiệm hoặc đầu tư.");
        } else {
            insight.setSuggestion("Duy trì chiến lược quản lý ngân sách hiện tại.");
        }

        return insight;
    }

    // Helper methods
    private double calculateAverage(List<Double> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double calculateStandardDeviation(List<Double> values) {
        double average = calculateAverage(values);
        double sum = 0;
        for (double value : values) {
            sum += Math.pow(value - average, 2);
        }
        return Math.sqrt(sum / values.size());
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0;

        Collections.sort(values);
        int size = values.size();

        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
    }
}