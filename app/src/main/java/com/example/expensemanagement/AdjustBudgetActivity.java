package com.example.expensemanagement;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import database.DatabaseHelper;
import models.BudgetPlan;
import adapters.AdjustBudgetAdapter;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdjustBudgetActivity extends AppCompatActivity {

    private TextView tvBudgetTitle, tvCurrentTotal, tvNewTotal, tvAdjustmentAmount;
    private Button btnSaveAdjustments, btnResetToOriginal;
    private RecyclerView rvAdjustCategories;
    private LinearLayout layoutAdjustmentSummary;

    private DatabaseHelper dbHelper;
    private AdjustBudgetAdapter adapter;
    private BudgetPlan currentBudget;
    private NumberFormat currencyFormatter;

    private double originalTotal;
    private double currentTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjust_budget);

        initializeComponents();
        loadBudgetData();
        setupEventListeners();
    }

    private void initializeComponents() {
        tvBudgetTitle = findViewById(R.id.tv_budget_title);
        tvCurrentTotal = findViewById(R.id.tv_current_total);
        tvNewTotal = findViewById(R.id.tv_new_total);
        tvAdjustmentAmount = findViewById(R.id.tv_adjustment_amount);
        btnSaveAdjustments = findViewById(R.id.btn_save_adjustments);
        btnResetToOriginal = findViewById(R.id.btn_reset_to_original);
        rvAdjustCategories = findViewById(R.id.rv_adjust_categories);
        layoutAdjustmentSummary = findViewById(R.id.layout_adjustment_summary);

        dbHelper = new DatabaseHelper(this);
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Setup RecyclerView
        rvAdjustCategories.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadBudgetData() {
        currentBudget = (BudgetPlan) getIntent().getSerializableExtra("budget_data");

        if (currentBudget == null) {
            Toast.makeText(this, "Không thể tải dữ liệu ngân sách", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvBudgetTitle.setText("Điều chỉnh ngân sách: " + currentBudget.getPeriodDisplayName());
        originalTotal = currentBudget.getTotalAllocated();
        currentTotal = originalTotal;

        updateTotals();

        // Setup adapter
        adapter = new AdjustBudgetAdapter(currentBudget.getCategories(), this::onBudgetAmountChanged);
        rvAdjustCategories.setAdapter(adapter);
    }

    private void setupEventListeners() {
        btnSaveAdjustments.setOnClickListener(v -> saveAdjustments());
        btnResetToOriginal.setOnClickListener(v -> resetToOriginal());
    }

    private void onBudgetAmountChanged() {
        // Calculate new total
        double newTotal = 0;
        for (BudgetPlan.CategoryBudget category : currentBudget.getCategories()) {
            newTotal += category.getAllocatedAmount();
        }

        currentTotal = newTotal;
        updateTotals();
    }

    private void updateTotals() {
        tvCurrentTotal.setText(currencyFormatter.format(originalTotal));
        tvNewTotal.setText(currencyFormatter.format(currentTotal));

        double adjustment = currentTotal - originalTotal;
        tvAdjustmentAmount.setText(currencyFormatter.format(Math.abs(adjustment)));

        // ✅ FIXED: Use built-in Android icons instead of custom ones
        if (adjustment > 0) {
            tvAdjustmentAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            // Use built-in up arrow or set text with symbol
            tvAdjustmentAmount.setText("↑ " + currencyFormatter.format(Math.abs(adjustment)));
        } else if (adjustment < 0) {
            tvAdjustmentAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            // Use built-in down arrow or set text with symbol
            tvAdjustmentAmount.setText("↓ " + currencyFormatter.format(Math.abs(adjustment)));
        } else {
            tvAdjustmentAmount.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvAdjustmentAmount.setText(currencyFormatter.format(Math.abs(adjustment)));
        }

        // Show/hide adjustment summary
        layoutAdjustmentSummary.setVisibility(adjustment != 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void saveAdjustments() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (BudgetPlan.CategoryBudget category : currentBudget.getCategories()) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_BUDGET_AMOUNT, category.getAllocatedAmount());

                // ✅ FIXED: Use getCategoryId() instead of getId()
                String whereClause = DatabaseHelper.COLUMN_CATEGORY_ID + " = ? AND " +
                        DatabaseHelper.COLUMN_BUDGET_PERIOD + " = ?";
                String[] whereArgs = {String.valueOf(category.getCategoryId()), currentBudget.getPeriodValue()};

                db.update(DatabaseHelper.TABLE_BUDGETS, values, whereClause, whereArgs);
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Ngân sách đã được cập nhật", Toast.LENGTH_SHORT).show();

            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Có lỗi khi cập nhật ngân sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
        }
    }

    private void resetToOriginal() {
        // Reset all categories to original amounts
        // This would require storing original amounts, for simplicity we'll reload
        loadBudgetData();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Đã khôi phục về ngân sách gốc", Toast.LENGTH_SHORT).show();
    }
}