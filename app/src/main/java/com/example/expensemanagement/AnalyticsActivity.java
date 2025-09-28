package com.example.expensemanagement;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import adapters.TopCategoryAdapter;
import database.DatabaseHelper;
import models.CategoryAnalytics;

public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";

    // UI Components
    private Toolbar toolbar;
    private Spinner spinnerPeriod;
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;
    private TextView tvFinancialScore, tvFinancialAdvice, tvSpendingTrend;
    private ProgressBar progressFinancialHealth;
    private MaterialCardView cardBalance;
    private TabLayout tabLayoutCharts;

    // Charts
    private PieChart pieChart;
    private LineChart lineChart;
    private BarChart barChart;

    // RecyclerView
    private RecyclerView recyclerViewTopCategories;
    private TopCategoryAdapter topCategoryAdapter;

    // Database
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    // Data
    private String selectedPeriod = "this_month";
    private double totalIncome = 0;
    private double totalExpense = 0;
    private double balance = 0;
    private List<CategoryAnalytics> topCategories = new ArrayList<>();

    // Formatters
    private DecimalFormat currencyFormatter = new DecimalFormat("#,###");
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        initDatabase();
        initViews();
        setupToolbar();
        setupPeriodSpinner();
        setupChartTabs();
        setupRecyclerView();

        loadAnalyticsData();
    }

    private void initDatabase() {
        try {
            dbHelper = new DatabaseHelper(this);
            db = dbHelper.getReadableDatabase();
            Log.d(TAG, "Database initialized for analytics");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerPeriod = findViewById(R.id.spinnerPeriod);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvBalance = findViewById(R.id.tvBalance);
        tvFinancialScore = findViewById(R.id.tvFinancialScore);
        tvFinancialAdvice = findViewById(R.id.tvFinancialAdvice);
        tvSpendingTrend = findViewById(R.id.tvSpendingTrend);
        progressFinancialHealth = findViewById(R.id.progressFinancialHealth);
        cardBalance = findViewById(R.id.cardBalance);
        tabLayoutCharts = findViewById(R.id.tabLayoutCharts);

        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);

        recyclerViewTopCategories = findViewById(R.id.recyclerViewTopCategories);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupPeriodSpinner() {
        String[] periods = {
                "Tháng này", "Tháng trước", "3 tháng gần đây",
                "6 tháng gần đây", "Năm này", "Năm trước", "Tất cả"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newPeriod;
                switch (position) {
                    case 0: newPeriod = "this_month"; break;
                    case 1: newPeriod = "last_month"; break;
                    case 2: newPeriod = "last_3_months"; break;
                    case 3: newPeriod = "last_6_months"; break;
                    case 4: newPeriod = "this_year"; break;
                    case 5: newPeriod = "last_year"; break;
                    case 6: newPeriod = "all_time"; break;
                    default: newPeriod = "this_month"; break;
                }

                // Chỉ reload nếu period thực sự thay đổi
                if (!newPeriod.equals(selectedPeriod)) {
                    selectedPeriod = newPeriod;
                    loadAnalyticsData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChartTabs() {
        tabLayoutCharts.addTab(tabLayoutCharts.newTab().setText("Danh mục"));
        tabLayoutCharts.addTab(tabLayoutCharts.newTab().setText("Xu hướng"));
        tabLayoutCharts.addTab(tabLayoutCharts.newTab().setText("So sánh"));

        tabLayoutCharts.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchChart(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        topCategoryAdapter = new TopCategoryAdapter(topCategories);
        recyclerViewTopCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTopCategories.setAdapter(topCategoryAdapter);
    }

    private void switchChart(int position) {
        // Hide all charts first
        pieChart.setVisibility(View.GONE);
        lineChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);

        // Show and setup selected chart
        switch (position) {
            case 0:
                pieChart.setVisibility(View.VISIBLE);
                setupPieChart();
                break;
            case 1:
                lineChart.setVisibility(View.VISIBLE);
                setupLineChart();
                break;
            case 2:
                barChart.setVisibility(View.VISIBLE);
                setupBarChart();
                break;
        }
    }

    private void loadAnalyticsData() {
        try {
            String[] dateRange = getDateRange(selectedPeriod);

            // Load all data
            loadSummaryData(dateRange);
            loadTopCategories(dateRange);
            loadSpendingTrend();

            // Update UI
            updateUI();

            // Refresh current chart
            int selectedTab = tabLayoutCharts.getSelectedTabPosition();
            switchChart(selectedTab);

            Log.d(TAG, "Analytics data loaded successfully for period: " + selectedPeriod);
            Log.d(TAG, "Date range: " + dateRange[0] + " to " + dateRange[1]);
            Log.d(TAG, "Total categories found: " + topCategories.size());
            Log.d(TAG, "Total expense: " + totalExpense + ", Total income: " + totalIncome);

        } catch (Exception e) {
            Log.e(TAG, "Error loading analytics data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String[] getDateRange(String period) {
        Calendar cal = Calendar.getInstance();
        String endDate = dateFormatter.format(cal.getTime());
        String startDate;

        switch (period) {
            case "this_month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormatter.format(cal.getTime());
                break;
            case "last_month":
                cal.add(Calendar.MONTH, -1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = dateFormatter.format(cal.getTime());
                break;
            case "last_3_months":
                cal.add(Calendar.MONTH, -3);
                startDate = dateFormatter.format(cal.getTime());
                break;
            case "last_6_months":
                cal.add(Calendar.MONTH, -6);
                startDate = dateFormatter.format(cal.getTime());
                break;
            case "this_year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                startDate = dateFormatter.format(cal.getTime());
                break;
            case "last_year":
                cal.add(Calendar.YEAR, -1);
                cal.set(Calendar.DAY_OF_YEAR, 1);
                startDate = dateFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
                endDate = dateFormatter.format(cal.getTime());
                break;
            default: // all_time
                startDate = "2020-01-01";
                break;
        }

        Log.d(TAG, "Date range for " + period + ": " + startDate + " to " + endDate);
        return new String[]{startDate, endDate};
    }

    private void loadSummaryData(String[] dateRange) {
        totalIncome = 0;
        totalExpense = 0;

        String query = "SELECT " + DatabaseHelper.COLUMN_TYPE + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as total FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS + " WHERE " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ? GROUP BY " + DatabaseHelper.COLUMN_TYPE;

        Log.d(TAG, "Loading summary data with query: " + query);
        Log.d(TAG, "Date range: " + dateRange[0] + " - " + dateRange[1]);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, dateRange);
            Log.d(TAG, "Summary cursor count: " + cursor.getCount());

            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                Log.d(TAG, "Type: " + type + ", Amount: " + amount);

                if ("income".equals(type)) {
                    totalIncome = amount;
                } else if ("expense".equals(type)) {
                    totalExpense = amount;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading summary data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        balance = totalIncome - totalExpense;
        Log.d(TAG, "Final totals - Income: " + totalIncome + ", Expense: " + totalExpense + ", Balance: " + balance);
    }

    private void loadTopCategories(String[] dateRange) {
        topCategories.clear();

        String query = "SELECT c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                "c." + DatabaseHelper.COLUMN_CATEGORY_ICON + " as icon, " +
                "c." + DatabaseHelper.COLUMN_CATEGORY_COLOR + " as color, " +
                "SUM(t." + DatabaseHelper.COLUMN_AMOUNT + ") as total " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                "JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                "WHERE t." + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ? " +
                "AND t." + DatabaseHelper.COLUMN_TYPE + " = 'expense' " +
                "GROUP BY c." + DatabaseHelper.COLUMN_ID + ", c." + DatabaseHelper.COLUMN_NAME + ", c." + DatabaseHelper.COLUMN_CATEGORY_ICON + ", c." + DatabaseHelper.COLUMN_CATEGORY_COLOR + " " +
                "ORDER BY total DESC " +
                "LIMIT 5";

        Log.d(TAG, "Loading top categories with query: " + query);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, dateRange);
            Log.d(TAG, "Top categories cursor count: " + cursor.getCount());

            while (cursor.moveToNext()) {
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
                String icon = cursor.getString(cursor.getColumnIndexOrThrow("icon"));
                String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                double percentage = totalExpense > 0 ? (amount / totalExpense) * 100 : 0;

                Log.d(TAG, "Category: " + categoryName + ", Amount: " + amount + ", Percentage: " + percentage);

                CategoryAnalytics category = new CategoryAnalytics(categoryName, amount, percentage, icon, color);
                topCategories.add(category);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading top categories: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void loadSpendingTrend() {
        try {
            if (totalExpense <= 0) {
                tvSpendingTrend.setText("Không có dữ liệu");
                tvSpendingTrend.setTextColor(Color.parseColor("#666666"));
                return;
            }

            String[] previousPeriodRange = getPreviousPeriodRange(selectedPeriod);
            Log.d(TAG, "Previous period range: " + previousPeriodRange[0] + " - " + previousPeriodRange[1]);

            String query = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as total FROM " +
                    DatabaseHelper.TABLE_TRANSACTIONS + " WHERE " + DatabaseHelper.COLUMN_DATE +
                    " BETWEEN ? AND ? AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense'";

            Cursor cursor = null;
            double previousExpense = 0;

            try {
                cursor = db.rawQuery(query, previousPeriodRange);
                if (cursor.moveToFirst()) {
                    previousExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Log.d(TAG, "Previous expense: " + previousExpense + ", Current expense: " + totalExpense);

            if (previousExpense <= 0) {
                tvSpendingTrend.setText("Kỳ đầu");
                tvSpendingTrend.setTextColor(Color.parseColor("#2196F3"));
            } else {
                double changePercent = ((totalExpense - previousExpense) / previousExpense) * 100;
                updateSpendingTrendUI(changePercent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading spending trend: " + e.getMessage());
            e.printStackTrace();
            tvSpendingTrend.setText("Lỗi tính toán");
            tvSpendingTrend.setTextColor(Color.parseColor("#666666"));
        }
    }

    private String[] getPreviousPeriodRange(String currentPeriod) {
        Calendar cal = Calendar.getInstance();
        String endDate, startDate;

        switch (currentPeriod) {
            case "this_month":
                cal.add(Calendar.MONTH, -1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = dateFormatter.format(cal.getTime());
                break;

            case "last_month":
                cal.add(Calendar.MONTH, -2);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = dateFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = dateFormatter.format(cal.getTime());
                break;

            case "this_year":
                cal.add(Calendar.YEAR, -1);
                cal.set(Calendar.DAY_OF_YEAR, 1);
                startDate = dateFormatter.format(cal.getTime());
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
                endDate = dateFormatter.format(cal.getTime());
                break;

            default:
                // Calculate previous period with same duration
                String[] currentRange = getDateRange(currentPeriod);
                try {
                    Calendar startCal = Calendar.getInstance();
                    Calendar endCal = Calendar.getInstance();
                    startCal.setTime(dateFormatter.parse(currentRange[0]));
                    endCal.setTime(dateFormatter.parse(currentRange[1]));

                    long duration = endCal.getTimeInMillis() - startCal.getTimeInMillis();
                    startCal.setTimeInMillis(startCal.getTimeInMillis() - duration);
                    endCal.setTimeInMillis(endCal.getTimeInMillis() - duration);

                    startDate = dateFormatter.format(startCal.getTime());
                    endDate = dateFormatter.format(endCal.getTime());
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating previous period: " + e.getMessage());
                    // Fallback: last month
                    cal.add(Calendar.MONTH, -1);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    startDate = dateFormatter.format(cal.getTime());
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    endDate = dateFormatter.format(cal.getTime());
                }
                break;
        }

        return new String[]{startDate, endDate};
    }

    private void updateSpendingTrendUI(double changePercent) {
        String trendText;
        int color;

        if (changePercent > 0) {
            trendText = String.format("+%.1f%%", changePercent);
            color = Color.parseColor("#F44336"); // Red for increase
        } else if (changePercent < 0) {
            trendText = String.format("%.1f%%", changePercent);
            color = Color.parseColor("#4CAF50"); // Green for decrease
        } else {
            trendText = "0%";
            color = Color.parseColor("#666666");
        }

        tvSpendingTrend.setText(trendText);
        tvSpendingTrend.setTextColor(color);
    }

    private void updateUI() {
        // Update summary cards
        tvTotalIncome.setText(formatCurrency(totalIncome));
        tvTotalExpense.setText(formatCurrency(totalExpense));
        tvBalance.setText(formatCurrency(balance));

        // Update balance card color
        if (balance > 0) {
            cardBalance.setCardBackgroundColor(Color.parseColor("#E8F5E8"));
            tvBalance.setTextColor(Color.parseColor("#1B5E20"));
        } else if (balance < 0) {
            cardBalance.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            tvBalance.setTextColor(Color.parseColor("#B71C1C"));
        } else {
            cardBalance.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            tvBalance.setTextColor(Color.parseColor("#666666"));
        }

        // Update financial health score
        updateFinancialHealthScore();

        // Refresh RecyclerView
        if (topCategoryAdapter != null) {
            topCategoryAdapter.notifyDataSetChanged();
        }
    }

    private void updateFinancialHealthScore() {
        int score = calculateFinancialScore();
        progressFinancialHealth.setProgress(score);
        tvFinancialScore.setText(score + "/100");

        String advice;
        int color;

        if (totalIncome <= 0 && totalExpense <= 0) {
            advice = "Chưa có dữ liệu. Hãy bắt đầu ghi chép thu chi.";
            color = Color.parseColor("#666666");
        } else if (score >= 80) {
            advice = "Tuyệt vời! Bạn đang quản lý tài chính rất tốt.";
            color = Color.parseColor("#4CAF50");
        } else if (score >= 60) {
            advice = "Khá tốt! Một số điều chỉnh nhỏ sẽ giúp cải thiện.";
            color = Color.parseColor("#FF9800");
        } else if (score >= 40) {
            advice = "Cần cải thiện. Hãy theo dõi chi tiêu chặt chẽ hơn.";
            color = Color.parseColor("#FF5722");
        } else {
            advice = "Cảnh báo! Cần xem xét lại kế hoạch tài chính.";
            color = Color.parseColor("#F44336");
        }

        tvFinancialScore.setTextColor(color);
        tvFinancialAdvice.setText(advice);
    }

    private int calculateFinancialScore() {
        if (totalIncome <= 0 && totalExpense <= 0) {
            return 0;
        }

        int score = 50; // Base score

        // Score based on balance
        if (balance > 0) {
            score += 30;
        } else if (balance < 0) {
            score -= 20;
        }

        // Score based on saving rate (only if there's income)
        if (totalIncome > 0) {
            double savingRate = balance / totalIncome;
            if (savingRate > 0.2) { // Saving more than 20%
                score += 20;
            } else if (savingRate > 0.1) { // Saving 10-20%
                score += 10;
            } else if (savingRate < -0.1) { // Spending more than 110% of income
                score -= 15;
            }
        } else if (totalExpense > 0) {
            // Only expenses, no income
            score = 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private void setupPieChart() {
        if (topCategories.isEmpty() || totalExpense <= 0) {
            pieChart.clear();
            pieChart.setNoDataText("Không có dữ liệu cho kỳ này");
            pieChart.setNoDataTextColor(Color.parseColor("#666666"));
            pieChart.getLegend().setEnabled(false);
            pieChart.getDescription().setEnabled(false);
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (CategoryAnalytics category : topCategories) {
            entries.add(new PieEntry((float) category.getPercentage(), category.getName()));
            try {
                colors.add(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                colors.add(ColorTemplate.MATERIAL_COLORS[colors.size() % ColorTemplate.MATERIAL_COLORS.length]);
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Chart styling
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Chi tiêu\ntheo danh mục");
        pieChart.setCenterTextSize(14f);

        // Legend
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupLineChart() {
        String[] dateRange = getDateRange(selectedPeriod);
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        boolean hasData = false;

        // Get date list for the selected period
        List<String> dateList = getDateListForPeriod(dateRange[0], dateRange[1]);
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < dateList.size(); i++) {
            String date = dateList.get(i);
            double dailyExpense = getDailyExpense(date);
            entries.add(new Entry(i, (float) dailyExpense));

            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dateFormatter.parse(date));
                labels.add(labelFormat.format(cal.getTime()));
            } catch (Exception e) {
                labels.add(date.substring(8)); // fallback: just the day
            }

            if (dailyExpense > 0) {
                hasData = true;
            }
        }

        if (!hasData || entries.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("Không có dữ liệu chi tiêu trong kỳ này");
            lineChart.setNoDataTextColor(Color.parseColor("#666666"));
            lineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Chi tiêu hàng ngày");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E3F2FD"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 0) {
                    if (value >= 1000000) {
                        return String.format("%.1fM", value / 1000000);
                    } else if (value >= 1000) {
                        return String.format("%.0fK", value / 1000);
                    }
                    return String.format("%.0f", value);
                }
                return "";
            }
        });

        LineData data = new LineData(dataSet);
        lineChart.setData(data);

        // Chart styling
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        // X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);

        // Adjust number of labels to prevent overlap
        int maxLabels = Math.min(labels.size(), 7);
        if (labels.size() > 15) {
            maxLabels = 5;
        }
        xAxis.setLabelCount(maxLabels, false);

        // Y Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format("%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format("%.0fK", value / 1000);
                }
                return String.format("%.0f", value);
            }
        });

        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private List<String> getDateListForPeriod(String startDate, String endDate) {
        List<String> dates = new ArrayList<>();
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(dateFormatter.parse(startDate));
            end.setTime(dateFormatter.parse(endDate));

            // Limit number of days to prevent UI overload
            long daysDiff = (end.getTimeInMillis() - start.getTimeInMillis()) / (24 * 60 * 60 * 1000);
            if (daysDiff > 90) {
                // If more than 90 days, sample the data
                int step = (int) Math.ceil(daysDiff / 30.0); // Get about 30 points
                Calendar current = (Calendar) start.clone();
                while (!current.after(end)) {
                    dates.add(dateFormatter.format(current.getTime()));
                    current.add(Calendar.DAY_OF_YEAR, step);
                }
            } else {
                // Get all days
                Calendar current = (Calendar) start.clone();
                while (!current.after(end)) {
                    dates.add(dateFormatter.format(current.getTime()));
                    current.add(Calendar.DAY_OF_YEAR, 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating date list: " + e.getMessage());
            // Fallback: create last 7 days
            Calendar cal = Calendar.getInstance();
            for (int i = 6; i >= 0; i--) {
                Calendar dayCal = (Calendar) cal.clone();
                dayCal.add(Calendar.DAY_OF_YEAR, -i);
                dates.add(dateFormatter.format(dayCal.getTime()));
            }
        }
        return dates;
    }

    private double getDailyExpense(String date) {
        String query = "SELECT SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as total FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS + " WHERE " + DatabaseHelper.COLUMN_DATE +
                " = ? AND " + DatabaseHelper.COLUMN_TYPE + " = 'expense'";

        Cursor cursor = null;
        double total = 0;

        try {
            cursor = db.rawQuery(query, new String[]{date});
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily expense for " + date + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return total;
    }

    private void setupBarChart() {
        String[] dateRange = getDateRange(selectedPeriod);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        boolean hasData = false;

        // Get period data based on selected period
        List<String[]> periods = getPeriodRangesForBarChart(selectedPeriod, dateRange);

        for (int i = 0; i < periods.size(); i++) {
            String[] period = periods.get(i);
            double[] periodData = getPeriodData(period[0], period[1]);

            entries.add(new BarEntry(i, new float[]{(float)periodData[0], (float)periodData[1]}));
            labels.add(period[2]); // label

            if (periodData[0] > 0 || periodData[1] > 0) {
                hasData = true;
            }
        }

        if (!hasData || entries.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("Không có dữ liệu trong kỳ này");
            barChart.setNoDataTextColor(Color.parseColor("#666666"));
            barChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#F44336")});
        dataSet.setStackLabels(new String[]{"Thu nhập", "Chi tiêu"});
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value > 1000000) {
                    return String.format("%.1fM", value / 1000000);
                } else if (value > 100000) {
                    return String.format("%.0fK", value / 1000);
                } else if (value > 0) {
                    return String.format("%.0f", value);
                }
                return "";
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChart.setData(data);

        // Chart styling
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);

        // X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size());
        xAxis.setTextSize(10f);
        xAxis.setAvoidFirstLastClipping(true);

        // Y Axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format("%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format("%.0fK", value / 1000);
                }
                return String.format("%.0f", value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        // Legend
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);

        barChart.setExtraBottomOffset(15f);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private List<String[]> getPeriodRangesForBarChart(String selectedPeriod, String[] mainRange) {
        List<String[]> periods = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        try {
            cal.setTime(dateFormatter.parse(mainRange[1]));
        } catch (Exception e) {
            cal = Calendar.getInstance();
        }

        switch (selectedPeriod) {
            case "this_month":
            case "last_month":
                // Divide by weeks in the month
                Calendar monthStart = Calendar.getInstance();
                try {
                    monthStart.setTime(dateFormatter.parse(mainRange[0]));
                } catch (Exception e) {
                    monthStart.set(Calendar.DAY_OF_MONTH, 1);
                }

                Calendar weekStart = (Calendar) monthStart.clone();
                int weekNum = 1;

                while (weekStart.before(cal) || weekStart.equals(cal)) {
                    Calendar weekEnd = (Calendar) weekStart.clone();
                    weekEnd.add(Calendar.DAY_OF_YEAR, 6);

                    if (weekEnd.after(cal)) {
                        weekEnd = (Calendar) cal.clone();
                    }

                    periods.add(new String[]{
                            dateFormatter.format(weekStart.getTime()),
                            dateFormatter.format(weekEnd.getTime()),
                            "T" + weekNum
                    });

                    weekStart.add(Calendar.WEEK_OF_YEAR, 1);
                    weekNum++;

                    if (weekNum > 5) break; // Max 5 weeks
                }
                break;

            case "last_3_months":
                // Divide by months in 3 months
                for (int i = 2; i >= 0; i--) {
                    Calendar monthCal = Calendar.getInstance();
                    monthCal.add(Calendar.MONTH, -i);
                    monthCal.set(Calendar.DAY_OF_MONTH, 1);
                    String start = dateFormatter.format(monthCal.getTime());

                    monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    String end = dateFormatter.format(monthCal.getTime());

                    periods.add(new String[]{start, end,
                            new SimpleDateFormat("MM/yy", Locale.getDefault()).format(monthCal.getTime())});
                }
                break;

            case "last_6_months":
                // Divide by months in 6 months
                for (int i = 5; i >= 0; i--) {
                    Calendar monthCal = Calendar.getInstance();
                    monthCal.add(Calendar.MONTH, -i);
                    monthCal.set(Calendar.DAY_OF_MONTH, 1);
                    String start = dateFormatter.format(monthCal.getTime());

                    monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    String end = dateFormatter.format(monthCal.getTime());

                    periods.add(new String[]{start, end,
                            new SimpleDateFormat("MM/yy", Locale.getDefault()).format(monthCal.getTime())});
                }
                break;

            case "this_year":
            case "last_year":
                // Divide by quarters in the year
                String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
                Calendar yearCal = Calendar.getInstance();
                try {
                    if ("last_year".equals(selectedPeriod)) {
                        yearCal.add(Calendar.YEAR, -1);
                    } else {
                        // this_year - only get from beginning of year to current
                        String[] currentRange = getDateRange(selectedPeriod);
                        yearCal.setTime(dateFormatter.parse(currentRange[1])); // end date
                    }
                } catch (Exception e) {
                    // fallback
                }
                int year = yearCal.get(Calendar.YEAR);

                for (int q = 0; q < 4; q++) {
                    Calendar qStart = Calendar.getInstance();
                    qStart.set(year, q * 3, 1);

                    Calendar qEnd = Calendar.getInstance();
                    qEnd.set(year, q * 3 + 2, 1);
                    qEnd.set(Calendar.DAY_OF_MONTH, qEnd.getActualMaximum(Calendar.DAY_OF_MONTH));

                    // For this year, don't show future quarters
                    if ("this_year".equals(selectedPeriod)) {
                        Calendar now = Calendar.getInstance();
                        if (qStart.after(now)) {
                            break;
                        }
                        // If current quarter, only take up to current date
                        if (qEnd.after(now)) {
                            qEnd = now;
                        }
                    }

                    String start = dateFormatter.format(qStart.getTime());
                    String end = dateFormatter.format(qEnd.getTime());

                    periods.add(new String[]{start, end, quarters[q]});
                }
                break;

            default:
                // Default: last 4 weeks
                for (int i = 3; i >= 0; i--) {
                    Calendar weekCal = Calendar.getInstance();
                    weekCal.add(Calendar.WEEK_OF_YEAR, -i);
                    weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    String start = dateFormatter.format(weekCal.getTime());

                    weekCal.add(Calendar.DAY_OF_YEAR, 6);
                    String end = dateFormatter.format(weekCal.getTime());

                    periods.add(new String[]{start, end, "T" + (4-i)});
                }
                break;
        }

        return periods;
    }

    private double[] getPeriodData(String startDate, String endDate) {
        double income = 0, expense = 0;

        String query = "SELECT " + DatabaseHelper.COLUMN_TYPE + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") as total FROM " +
                DatabaseHelper.TABLE_TRANSACTIONS + " WHERE " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ? GROUP BY " + DatabaseHelper.COLUMN_TYPE;

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{startDate, endDate});

            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                if ("income".equals(type)) {
                    income = amount;
                } else if ("expense".equals(type)) {
                    expense = amount;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting period data from " + startDate + " to " + endDate + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return new double[]{income, expense};
    }

    private String formatCurrency(double amount) {
        return currencyFormatter.format(amount) + " đ";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}