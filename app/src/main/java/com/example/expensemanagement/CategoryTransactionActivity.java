// CategoryTransactionActivity.java - Show transactions for specific category in budget date range
package com.example.expensemanagement;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import database.DatabaseHelper;
import models.Transaction;
import adapters.TransactionAdapter;

public class CategoryTransactionActivity extends AppCompatActivity {

    private static final String TAG = "CategoryTransaction";

    // UI Components
    private ImageView ivBackButton;
    private TextView tvCategoryName, tvBudgetName, tvDateRange;
    private TextView tvTotalTransactions, tvTotalExpense, tvTotalIncome;
    private TextView tvBudgetAllocated, tvBudgetSpent, tvBudgetRemaining;
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;

    // Data
    private DatabaseHelper dbHelper;
    private List<Transaction> transactions;
    private TransactionAdapter transactionAdapter;
    private NumberFormat currencyFormatter;

    // Parameters from Intent
    private int categoryId;
    private String categoryName;
    private String userId;
    private String budgetName;
    private String startDate;
    private String endDate;
    private double budgetAllocated;
    private double budgetSpent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_transactions);

        Log.d(TAG, "CategoryTransactionActivity onCreate started");

        getIntentData();
        initializeComponents();
        setupUI();
        loadTransactionData();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        categoryId = intent.getIntExtra("category_id", 0);
        categoryName = intent.getStringExtra("category_name");
        userId = intent.getStringExtra("user_id");
        budgetName = intent.getStringExtra("budget_name");
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        budgetAllocated = intent.getDoubleExtra("budget_amount", 0.0);
        budgetSpent = intent.getDoubleExtra("spent_amount", 0.0);

        Log.d(TAG, "Intent Data:");
        Log.d(TAG, "  Category ID: " + categoryId);
        Log.d(TAG, "  Category Name: " + categoryName);
        Log.d(TAG, "  User ID: " + userId);
        Log.d(TAG, "  Budget Name: " + budgetName);
        Log.d(TAG, "  Date Range: " + startDate + " to " + endDate);
        Log.d(TAG, "  Budget: " + budgetAllocated + " / Spent: " + budgetSpent);
    }

    private void initializeComponents() {
        dbHelper = new DatabaseHelper(this);
        transactions = new ArrayList<>();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Find views
        ivBackButton = findViewById(R.id.iv_back_button);
        tvCategoryName = findViewById(R.id.tv_category_name);
        tvBudgetName = findViewById(R.id.tv_budget_name);
        tvDateRange = findViewById(R.id.tv_date_range);
        tvTotalTransactions = findViewById(R.id.tv_total_transactions);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvBudgetAllocated = findViewById(R.id.tv_budget_allocated);
        tvBudgetSpent = findViewById(R.id.tv_budget_spent);
        tvBudgetRemaining = findViewById(R.id.tv_budget_remaining);
        rvTransactions = findViewById(R.id.rv_transactions);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupUI() {
        // Setup action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("📊 Chi tiết danh mục");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Back button
        ivBackButton.setOnClickListener(v -> finish());

        // Display basic info
        tvCategoryName.setText(categoryName);
        tvBudgetName.setText(budgetName);
        tvDateRange.setText(formatDateRange(startDate, endDate));

        // Setup RecyclerView
        transactionAdapter = new TransactionAdapter(transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);

        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void loadTransactionData() {
        Log.d(TAG, "Loading transaction data...");

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        new Thread(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                // Convert date format for query
                String dbStartDate = convertToDbDate(startDate);
                String dbEndDate = convertToDbDate(endDate);

                Log.d(TAG, "Querying transactions:");
                Log.d(TAG, "  Category ID: " + categoryId);
                Log.d(TAG, "  User ID: " + userId);
                Log.d(TAG, "  Date range: " + dbStartDate + " to " + dbEndDate);

                // Query transactions for this category within date range
                String query = "SELECT " +
                        DatabaseHelper.COLUMN_ID + ", " +
                        DatabaseHelper.COLUMN_TYPE + ", " +
                        DatabaseHelper.COLUMN_AMOUNT + ", " +
                        DatabaseHelper.COLUMN_DATE + ", " +
                        DatabaseHelper.COLUMN_NOTE + ", " +
                        DatabaseHelper.COLUMN_PAYMENT_METHOD + " " +
                        "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " " +
                        "WHERE " + DatabaseHelper.COLUMN_CATEGORY_ID + " = ? " +
                        "AND " + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                        "AND " + DatabaseHelper.COLUMN_DATE + " BETWEEN ? AND ? " +
                        "ORDER BY " + DatabaseHelper.COLUMN_DATE + " DESC, " +
                        DatabaseHelper.COLUMN_ID + " DESC";

                String[] args = {
                        String.valueOf(categoryId),
                        userId,
                        dbStartDate,
                        dbEndDate
                };

                Cursor cursor = db.rawQuery(query, args);
                List<Transaction> tempTransactions = new ArrayList<>();

                if (cursor != null) {
                    Log.d(TAG, "Found " + cursor.getCount() + " transactions");

                    while (cursor.moveToNext()) {
                        Transaction transaction = new Transaction();

                        // Sửa: Cast int thành Long cho setId
                        transaction.setId(Long.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))));

                        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
                        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
                        transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
                        transaction.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));
                        transaction.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD)));

                        // Sửa: Cast int thành Long cho setCategoryId
                        transaction.setCategoryId(Long.valueOf(categoryId));

                        transaction.setCategoryName(categoryName);

                        // Sửa: Cast String thành Long cho setUserId (nếu userId là số)
                        // Nếu userId là String thuần túy, bạn cần kiểm tra Transaction model
                        try {
                            transaction.setUserId(Long.valueOf(userId));
                        } catch (NumberFormatException e) {
                            // Nếu userId không phải là số, bạn cần sửa Transaction model
                            // hoặc xử lý khác
                            Log.e(TAG, "UserId is not a number: " + userId);
                            // Có thể skip transaction này hoặc set giá trị default
                            continue;
                        }

                        tempTransactions.add(transaction);

                        Log.d(TAG, String.format("Transaction: %s %.0f on %s - %s",
                                transaction.getType(), transaction.getAmount(),
                                transaction.getDate(), transaction.getNote()));
                    }
                    cursor.close();
                }

                db.close();

                // Update UI on main thread
                runOnUiThread(() -> {
                    transactions.clear();
                    transactions.addAll(tempTransactions);
                    transactionAdapter.notifyDataSetChanged();
                    updateSummaryInfo();
                    updateUIVisibility();

                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    Log.d(TAG, "UI updated with " + transactions.size() + " transactions");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading transaction data: " + e.getMessage(), e);

                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateSummaryInfo() {
        // Calculate totals from loaded transactions
        int totalCount = transactions.size();
        double totalExpense = 0;
        double totalIncome = 0;

        for (Transaction transaction : transactions) {
            if ("expense".equals(transaction.getType())) {
                totalExpense += transaction.getAmount();
            } else if ("income".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            }
        }

        // Update transaction summary
        tvTotalTransactions.setText(String.valueOf(totalCount));
        tvTotalExpense.setText(currencyFormatter.format(totalExpense));
        tvTotalIncome.setText(currencyFormatter.format(totalIncome));

        // Update budget summary
        tvBudgetAllocated.setText(currencyFormatter.format(budgetAllocated));
        tvBudgetSpent.setText(currencyFormatter.format(budgetSpent));

        double remaining = budgetAllocated - budgetSpent;
        tvBudgetRemaining.setText(currencyFormatter.format(remaining));

        // Color code remaining amount
        if (remaining < 0) {
            tvBudgetRemaining.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (remaining < budgetAllocated * 0.2) {
            tvBudgetRemaining.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvBudgetRemaining.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }

        Log.d(TAG, String.format("Summary updated: %d transactions, expense: %.0f, income: %.0f",
                totalCount, totalExpense, totalIncome));
    }

    private void updateUIVisibility() {
        boolean hasTransactions = !transactions.isEmpty();

        rvTransactions.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
        tvEmptyState.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);

        if (!hasTransactions) {
            tvEmptyState.setText("Không có giao dịch nào trong khoảng thời gian này.\n" +
                    "Hãy thêm giao dịch để theo dõi chi tiêu của bạn!");
        }
    }

    private void refreshData() {
        Log.d(TAG, "Manual refresh triggered");
        loadTransactionData();
        Toast.makeText(this, "Đang cập nhật dữ liệu...", Toast.LENGTH_SHORT).show();
    }

    private String convertToDbDate(String displayDate) {
        try {
            // If already in YYYY-MM-DD format, return as is
            if (displayDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return displayDate;
            }

            // Convert from DD/MM/YYYY to YYYY-MM-DD
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = displayFormat.parse(displayDate);
            return dbFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error converting date: " + displayDate, e);
            return displayDate; // Return original if conversion fails
        }
    }

    private String formatDateRange(String start, String end) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

            if (start != null && end != null) {
                Date startDate = inputFormat.parse(start);
                Date endDate = inputFormat.parse(end);

                return outputFormat.format(startDate) + " - " + outputFormat.format(endDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date range", e);
        }

        return (start != null ? start : "?") + " - " + (end != null ? end : "?");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
        Log.d(TAG, "CategoryTransactionActivity destroyed");
    }
}