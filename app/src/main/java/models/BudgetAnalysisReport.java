// 📊 BudgetAnalysisReport.java - Comprehensive Budget Analysis Report Model
package models;

import java.util.ArrayList;
import java.util.List;

/**
 * 📊 BudgetAnalysisReport - Báo cáo phân tích ngân sách tổng hợp
 *
 * Chứa tất cả các kết quả phân tích từ BudgetAnalytics:
 * - Pattern insights (Phân tích mẫu)
 * - Variance insights (Phân tích độ lệch)
 * - Cash flow forecast (Dự báo dòng tiền)
 * - Risk assessment (Đánh giá rủi ro)
 * - Optimization suggestions (Đề xuất tối ưu)
 * - Anomaly insights (Phát hiện bất thường)
 * - Overall health score (Điểm sức khỏe tổng thể)
 * - Executive summary (Tóm tắt tổng quan)
 * - Top recommendations (Khuyến nghị hàng đầu)
 */
public class BudgetAnalysisReport {

    // 🔍 Pattern Analysis Results
    private List<SmartInsight> patternInsights;

    // 📈 Variance Analysis Results
    private List<SmartInsight> varianceInsights;

    // 🔮 Cash Flow Forecast
    private SmartInsight cashFlowForecast;

    // ⚠️ Risk Assessment
    private SmartInsight riskAssessment;

    // 💡 Optimization Suggestions
    private List<SmartInsight> optimizationSuggestions;

    // 🎯 Anomaly Detection Results
    private List<SmartInsight> anomalyInsights;

    // 📊 Overall Health Score (0-100)
    private int overallHealthScore;

    // 📝 Executive Summary
    private String summary;

    // 🎯 Top Recommendations
    private List<String> topRecommendations;

    // 📅 Report Generation Timestamp
    private long generatedAt;

    // 🏷️ Report ID
    private String reportId;

    // 📊 Performance Metrics
    private java.util.Map<String, Double> performanceMetrics;

    /**
     * Constructor
     */
    public BudgetAnalysisReport() {
        this.patternInsights = new ArrayList<>();
        this.varianceInsights = new ArrayList<>();
        this.optimizationSuggestions = new ArrayList<>();
        this.anomalyInsights = new ArrayList<>();
        this.topRecommendations = new ArrayList<>();
        this.performanceMetrics = new java.util.HashMap<>();
        this.generatedAt = System.currentTimeMillis();
        this.reportId = "RPT_" + System.currentTimeMillis();
        this.overallHealthScore = 0;
    }

    // ===============================
    // GETTERS AND SETTERS
    // ===============================

    /**
     * Get pattern insights
     */
    public List<SmartInsight> getPatternInsights() {
        return patternInsights;
    }

    /**
     * Set pattern insights
     */
    public void setPatternInsights(List<SmartInsight> patternInsights) {
        this.patternInsights = patternInsights != null ? patternInsights : new ArrayList<>();
    }

    /**
     * Get variance insights
     */
    public List<SmartInsight> getVarianceInsights() {
        return varianceInsights;
    }

    /**
     * Set variance insights
     */
    public void setVarianceInsights(List<SmartInsight> varianceInsights) {
        this.varianceInsights = varianceInsights != null ? varianceInsights : new ArrayList<>();
    }

    /**
     * Get cash flow forecast
     */
    public SmartInsight getCashFlowForecast() {
        return cashFlowForecast;
    }

    /**
     * Set cash flow forecast
     */
    public void setCashFlowForecast(SmartInsight cashFlowForecast) {
        this.cashFlowForecast = cashFlowForecast;
    }

    /**
     * Get risk assessment
     */
    public SmartInsight getRiskAssessment() {
        return riskAssessment;
    }

    /**
     * Set risk assessment
     */
    public void setRiskAssessment(SmartInsight riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    /**
     * Get optimization suggestions
     */
    public List<SmartInsight> getOptimizationSuggestions() {
        return optimizationSuggestions;
    }

    /**
     * Set optimization suggestions
     */
    public void setOptimizationSuggestions(List<SmartInsight> optimizationSuggestions) {
        this.optimizationSuggestions = optimizationSuggestions != null ? optimizationSuggestions : new ArrayList<>();
    }

    /**
     * Get anomaly insights
     */
    public List<SmartInsight> getAnomalyInsights() {
        return anomalyInsights;
    }

    /**
     * Set anomaly insights
     */
    public void setAnomalyInsights(List<SmartInsight> anomalyInsights) {
        this.anomalyInsights = anomalyInsights != null ? anomalyInsights : new ArrayList<>();
    }

    /**
     * Get overall health score
     */
    public int getOverallHealthScore() {
        return overallHealthScore;
    }

    /**
     * Set overall health score
     */
    public void setOverallHealthScore(int overallHealthScore) {
        this.overallHealthScore = Math.max(0, Math.min(100, overallHealthScore));
    }

    /**
     * Get summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Set summary
     */
    public void setSummary(String summary) {
        this.summary = summary != null ? summary : "";
    }

    /**
     * Get top recommendations
     */
    public List<String> getTopRecommendations() {
        return topRecommendations;
    }

    /**
     * Set top recommendations
     */
    public void setTopRecommendations(List<String> topRecommendations) {
        this.topRecommendations = topRecommendations != null ? topRecommendations : new ArrayList<>();
    }

    /**
     * Get report generation timestamp
     */
    public long getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Set report generation timestamp
     */
    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    /**
     * Get report ID
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * Set report ID
     */
    public void setReportId(String reportId) {
        this.reportId = reportId != null ? reportId : "RPT_" + System.currentTimeMillis();
    }

    /**
     * Get performance metrics
     */
    public java.util.Map<String, Double> getPerformanceMetrics() {
        return performanceMetrics;
    }

    /**
     * Set performance metrics
     */
    public void setPerformanceMetrics(java.util.Map<String, Double> performanceMetrics) {
        this.performanceMetrics = performanceMetrics != null ? performanceMetrics : new java.util.HashMap<>();
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Get total number of insights
     */
    public int getTotalInsightCount() {
        int count = 0;
        count += patternInsights.size();
        count += varianceInsights.size();
        count += optimizationSuggestions.size();
        count += anomalyInsights.size();
        if (cashFlowForecast != null) count++;
        if (riskAssessment != null) count++;
        return count;
    }

    /**
     * Get high priority insights count
     */
    public int getHighPriorityInsightCount() {
        int count = 0;

        for (SmartInsight insight : patternInsights) {
            if (insight.getImpact() == SmartInsight.Impact.HIGH) count++;
        }

        for (SmartInsight insight : varianceInsights) {
            if (insight.getImpact() == SmartInsight.Impact.HIGH) count++;
        }

        for (SmartInsight insight : optimizationSuggestions) {
            if (insight.getImpact() == SmartInsight.Impact.HIGH) count++;
        }

        for (SmartInsight insight : anomalyInsights) {
            if (insight.getImpact() == SmartInsight.Impact.HIGH) count++;
        }

        if (cashFlowForecast != null && cashFlowForecast.getImpact() == SmartInsight.Impact.HIGH) count++;
        if (riskAssessment != null && riskAssessment.getImpact() == SmartInsight.Impact.HIGH) count++;

        return count;
    }

    /**
     * Check if report has critical issues
     */
    public boolean hasCriticalIssues() {
        return getHighPriorityInsightCount() > 0 || overallHealthScore < 40;
    }

    /**
     * Get health status text
     */
    public String getHealthStatusText() {
        if (overallHealthScore >= 80) {
            return "Tuyệt vời";
        } else if (overallHealthScore >= 60) {
            return "Tốt";
        } else if (overallHealthScore >= 40) {
            return "Cần cải thiện";
        } else {
            return "Cần chú ý";
        }
    }

    /**
     * Get health status color (for UI)
     */
    public String getHealthStatusColor() {
        if (overallHealthScore >= 80) {
            return "#4CAF50"; // Green
        } else if (overallHealthScore >= 60) {
            return "#FF9800"; // Orange
        } else if (overallHealthScore >= 40) {
            return "#FF5722"; // Deep Orange
        } else {
            return "#F44336"; // Red
        }
    }

    /**
     * Add pattern insight
     */
    public void addPatternInsight(SmartInsight insight) {
        if (insight != null) {
            this.patternInsights.add(insight);
        }
    }

    /**
     * Add variance insight
     */
    public void addVarianceInsight(SmartInsight insight) {
        if (insight != null) {
            this.varianceInsights.add(insight);
        }
    }

    /**
     * Add optimization suggestion
     */
    public void addOptimizationSuggestion(SmartInsight insight) {
        if (insight != null) {
            this.optimizationSuggestions.add(insight);
        }
    }

    /**
     * Add anomaly insight
     */
    public void addAnomalyInsight(SmartInsight insight) {
        if (insight != null) {
            this.anomalyInsights.add(insight);
        }
    }

    /**
     * Add recommendation
     */
    public void addRecommendation(String recommendation) {
        if (recommendation != null && !recommendation.trim().isEmpty()) {
            this.topRecommendations.add(recommendation.trim());
        }
    }

    /**
     * Add performance metric
     */
    public void addPerformanceMetric(String key, Double value) {
        if (key != null && value != null) {
            this.performanceMetrics.put(key, value);
        }
    }

    /**
     * Get all insights as a single list
     */
    public List<SmartInsight> getAllInsights() {
        List<SmartInsight> allInsights = new ArrayList<>();
        allInsights.addAll(patternInsights);
        allInsights.addAll(varianceInsights);
        allInsights.addAll(optimizationSuggestions);
        allInsights.addAll(anomalyInsights);

        if (cashFlowForecast != null) {
            allInsights.add(cashFlowForecast);
        }

        if (riskAssessment != null) {
            allInsights.add(riskAssessment);
        }

        return allInsights;
    }

    /**
     * Get insights by impact level
     */
    public List<SmartInsight> getInsightsByImpact(SmartInsight.Impact impact) {
        List<SmartInsight> filteredInsights = new ArrayList<>();

        for (SmartInsight insight : getAllInsights()) {
            if (insight.getImpact() == impact) {
                filteredInsights.add(insight);
            }
        }

        return filteredInsights;
    }

    /**
     * Get insights by type
     */
    public List<SmartInsight> getInsightsByType(SmartInsight.Type type) {
        List<SmartInsight> filteredInsights = new ArrayList<>();

        for (SmartInsight insight : getAllInsights()) {
            if (insight.getType() == type) {
                filteredInsights.add(insight);
            }
        }

        return filteredInsights;
    }

    /**
     * Check if report is empty
     */
    public boolean isEmpty() {
        return getTotalInsightCount() == 0;
    }

    /**
     * Generate formatted report summary for logging
     */
    @Override
    public String toString() {
        return String.format("BudgetAnalysisReport{" +
                        "reportId='%s', " +
                        "healthScore=%d, " +
                        "totalInsights=%d, " +
                        "highPriorityInsights=%d, " +
                        "generatedAt=%d" +
                        "}", reportId, overallHealthScore, getTotalInsightCount(),
                getHighPriorityInsightCount(), generatedAt);
    }

    /**
     * Check equality based on report ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BudgetAnalysisReport that = (BudgetAnalysisReport) obj;
        return reportId != null ? reportId.equals(that.reportId) : that.reportId == null;
    }

    /**
     * Generate hash code based on report ID
     */
    @Override
    public int hashCode() {
        return reportId != null ? reportId.hashCode() : 0;
    }
}