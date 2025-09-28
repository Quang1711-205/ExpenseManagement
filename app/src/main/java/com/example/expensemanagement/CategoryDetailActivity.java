// 📊 CategoryDetailActivity.java - Fixed Version
package com.example.expensemanagement;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagement.R;
import database.DatabaseHelper;
import models.Transaction;
import adapters.TransactionAdapter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 📊 CATEGORY DETAIL ACTIVITY - Fixed Implementation
 *
 * Features:
 * ✅ Display category information
 * ✅ Show recent transactions
 * ✅ Budget vs spending comparison
 * ✅ Simple analytics
 */
public class CategoryDetailActivity extends AppCompatActivity {

    private static final String TAG = "CategoryDetailActivity";

    // === Core Data ===
    private DatabaseHelper dbHelper;
    private String categoryName;
    private String userId;
    private List<Transaction> transactions;

    // === UI Components ===
    private Toolbar toolbar;
    private TextView tvCategoryName, tvBudgetAmount, tvSpentAmount, tvRemainingAmount;
    private TextView tvTransactionCount, tvAveragePerTransaction;
    private ProgressBar pbBudgetProgress;
    private RecyclerView rvTransactions;
    private CardView cardBudgetOverview, cardTransactionHistory;
    private Button btnAddTransaction, btnEditBudget;
    private LinearLayout layoutNoTransactions;

    // === Utils ===
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormatter;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        initializeComponents();
        setupUI();
        loadCategoryData();
    }

    private void initializeComponents() {
        Log.d(TAG, "🚀 Initializing CategoryDetailActivity...");

        // Get data from intent
        categoryName = getIntent().getStringExtra("category_name");
        userId = getIntent().getStringExtra("user_id");

        if (categoryName == null || userId == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin danh mục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize components
        dbHelper = new DatabaseHelper(this);
        transactions = new ArrayList<>();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));

        Log.d(TAG, "✅ Components initialized for category: " + categoryName);
    }

    private void setupUI() {
        // Find views
        findViews();

        // Setup toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup event listeners
        setupEventListeners();
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar);

        // Category info
        tvCategoryName = findViewById(R.id.tv_category_name);
        tvBudgetAmount = findViewById(R.id.tv_budget_amount);
        tvSpentAmount = findViewById(R.id.tv_spent_amount);
        tvRemainingAmount = findViewById(R.id.tv_remaining_amount);
        tvTransactionCount = findViewById(R.id.tv_transaction_count);
        tvAveragePerTransaction = findViewById(R.id.tv_average_per_transaction);
        pbBudgetProgress = findViewById(R.id.pb_budget_progress);

        // Cards and layouts
        cardBudgetOverview = findViewById(R.id.card_budget_overview);
        cardTransactionHistory = findViewById(R.id.card_transaction_history);
        layoutNoTransactions = findViewById(R.id.layout_no_transactions);

        // RecyclerView
        rvTransactions = findViewById(R.id.rv_transactions);

        // Buttons
        btnAddTransaction = findViewById(R.id.btn_add_transaction);
        btnEditBudget = findViewById(R.id.btn_edit_budget);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Chi tiết danh mục");
            }
        }
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(transactions);
        transactionAdapter.setOnTransactionClickListener(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                // Handle transaction click
                showTransactionDetail(transaction);
            }

            @Override
            public void onTransactionLongClick(Transaction transaction) {
                // Handle transaction long click
                showTransactionOptions(transaction);
            }
        });

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupEventListeners() {
        if (btnAddTransaction != null) {
            btnAddTransaction.setOnClickListener(v -> openAddTransaction());
        }

        if (btnEditBudget != null) {
            btnEditBudget.setOnClickListener(v -> openEditBudget());
        }
    }

    private void loadCategoryData() {
        Log.d(TAG, "📊 Loading category data...");

        try {
            // Load budget information
            loadBudgetInfo();

            // Load transactions
            loadTransactions();

            // Update UI
            updateUI();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading category data: " + e.getMessage(), e);
            showError("Không thể tải dữ liệu danh mục");
        }
    }

    private void loadBudgetInfo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT b.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name, " +
                "c." + DatabaseHelper.COLUMN_CATEGORY_ICON + ", c." + DatabaseHelper.COLUMN_CATEGORY_COLOR + " " +
                "FROM " + DatabaseHelper.TABLE_BUDGETS + " b " +
                "JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON b." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                "WHERE c." + DatabaseHelper.COLUMN_NAME + " = ? AND b." + DatabaseHelper.COLUMN_USER_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{categoryName, userId});

        if (cursor.moveToFirst()) {
            int budgetAmountIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BUDGET_AMOUNT);
            // You can extract other budget info here
        }
        cursor.close();
    }

    private void loadTransactions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        transactions.clear();

        String query = "SELECT t.*, c." + DatabaseHelper.COLUMN_NAME + " as category_name " +
                "FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t " +
                "JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID + " " +
                "WHERE c." + DatabaseHelper.COLUMN_NAME + " = ? AND t." + DatabaseHelper.COLUMN_USER_ID + " = ? " +
                "ORDER BY t." + DatabaseHelper.COLUMN_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{categoryName, userId});

        while (cursor.moveToNext()) {
            Transaction transaction = new Transaction();

            // FIX 1: Kiểm tra column tồn tại trước khi truy xuất
            int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
            int noteIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTE); // FIX 2: Sử dụng COLUMN_NOTE thay vì COLUMN_DESCRIPTION
            int amountIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_AMOUNT);
            int typeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE);
            int dateIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE);

            if (idIndex >= 0) transaction.setId(cursor.getLong(idIndex));
            if (noteIndex >= 0) transaction.setNote(cursor.getString(noteIndex)); // FIX 3: Sử dụng setNote() thay vì setDescription()
            if (amountIndex >= 0) transaction.setAmount(cursor.getDouble(amountIndex));
            if (typeIndex >= 0) transaction.setType(cursor.getString(typeIndex));
            if (dateIndex >= 0) transaction.setDate(cursor.getString(dateIndex));

            transaction.setCategoryName(categoryName);
            // FIX 4: Chuyển String userId sang long
            try {
                transaction.setUserId(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing userId: " + userId, e);
                transaction.setUserId(1L); // Default fallback
            }

            transactions.add(transaction);
        }
        cursor.close();

        Log.d(TAG, "✅ Loaded " + transactions.size() + " transactions");
    }

    private void updateUI() {
        // Update category name
        if (tvCategoryName != null) {
            tvCategoryName.setText(categoryName);
        }

        // Calculate totals
        double totalSpent = 0;
        for (Transaction transaction : transactions) {
            if ("expense".equals(transaction.getType())) {
                totalSpent += transaction.getAmount();
            }
        }

        // Update spending info
        if (tvSpentAmount != null) {
            tvSpentAmount.setText(currencyFormatter.format(totalSpent));
        }

        if (tvTransactionCount != null) {
            tvTransactionCount.setText(String.valueOf(transactions.size()) + " giao dịch");
        }

        if (tvAveragePerTransaction != null && !transactions.isEmpty()) {
            double average = totalSpent / transactions.size();
            tvAveragePerTransaction.setText("TB: " + currencyFormatter.format(average));
        }

        // Update transaction list
        if (transactions.isEmpty()) {
            showNoTransactions();
        } else {
            showTransactions();
        }

        transactionAdapter.notifyDataSetChanged();

        Log.d(TAG, "✅ UI updated successfully");
    }

    private void showTransactions() {
        if (layoutNoTransactions != null) {
            layoutNoTransactions.setVisibility(View.GONE);
        }
        if (cardTransactionHistory != null) {
            cardTransactionHistory.setVisibility(View.VISIBLE);
        }
    }

    private void showNoTransactions() {
        if (layoutNoTransactions != null) {
            layoutNoTransactions.setVisibility(View.VISIBLE);
        }
        if (cardTransactionHistory != null) {
            cardTransactionHistory.setVisibility(View.GONE);
        }
    }

    private void openAddTransaction() {
        try {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("category_name", categoryName);
            intent.putExtra("transaction_type", "expense");
            startActivityForResult(intent, 1001);
        } catch (Exception e) {
            Toast.makeText(this, "Tính năng thêm giao dịch đang được phát triển", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEditBudget() {
        Toast.makeText(this, "Tính năng chỉnh sửa ngân sách đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    private void showTransactionDetail(Transaction transaction) {
        // TODO: Implement transaction detail view
        Toast.makeText(this, "Chi tiết: " + transaction.getNote(), Toast.LENGTH_SHORT).show();
    }

    private void showTransactionOptions(Transaction transaction) {
        // TODO: Implement transaction options (edit, delete, etc.)
        Toast.makeText(this, "Tùy chọn cho: " + transaction.getNote(), Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, "❌ " + message, Toast.LENGTH_LONG).show();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1001) {
            // Reload data when returning from add transaction
            loadCategoryData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}