package com.example.expensemanagement;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import database.DatabaseHelper;
import models.Transaction;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailsActivity extends AppCompatActivity {

    // Database
    private DatabaseHelper dbHelper;

    // Views
    private MaterialToolbar toolbar;
    private TextView tvCategoryIcon, tvCategoryName, tvAmount, tvType;
    private TextView tvDate, tvPaymentMethod, tvNote, tvCreatedAt;
    private MaterialCardView cardTransaction;
    private MaterialButton btnEdit, btnDelete;

    // Data
    private Transaction transaction;
    private Long transactionId;

    // Formatters
    private DecimalFormat currencyFormatter;
    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat displayDateFormatter;
    private SimpleDateFormat dateTimeFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_details);

        // Get transaction ID from intent
        transactionId = getIntent().getLongExtra("transaction_id", -1);
        if (transactionId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFormatters();
        initViews();
        setupDatabase();
        setupClickListeners();
        loadTransactionDetails();
    }

    private void initializeFormatters() {
        currencyFormatter = new DecimalFormat("#,###,###");
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chi tiết giao dịch");

        tvCategoryIcon = findViewById(R.id.tvCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvAmount = findViewById(R.id.tvAmount);
        tvType = findViewById(R.id.tvType);
        tvDate = findViewById(R.id.tvDate);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvNote = findViewById(R.id.tvNote);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        cardTransaction = findViewById(R.id.cardTransaction);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setupDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> editTransaction());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadTransactionDetails() {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT " +
                    "t." + DatabaseHelper.COLUMN_ID + ", " +
                    "t." + DatabaseHelper.COLUMN_TYPE + ", " +
                    "t." + DatabaseHelper.COLUMN_AMOUNT + ", " +
                    "t." + DatabaseHelper.COLUMN_CATEGORY_ID + ", " +
                    "t." + DatabaseHelper.COLUMN_NOTE + ", " +
                    "t." + DatabaseHelper.COLUMN_DATE + ", " +
                    "t." + DatabaseHelper.COLUMN_PAYMENT_METHOD + ", " +
                    "t." + DatabaseHelper.COLUMN_CREATED_AT + ", " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_NAME + ", " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_ICON + ", " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_COLOR + ", " +
                    "c." + DatabaseHelper.COLUMN_CATEGORY_TYPE +
                    " FROM " + DatabaseHelper.TABLE_TRANSACTIONS + " t" +
                    " INNER JOIN " + DatabaseHelper.TABLE_CATEGORIES + " c" +
                    " ON t." + DatabaseHelper.COLUMN_CATEGORY_ID + " = c." + DatabaseHelper.COLUMN_ID +
                    " WHERE t." + DatabaseHelper.COLUMN_ID + " = ?";

            cursor = db.rawQuery(query, new String[]{String.valueOf(transactionId)});

            if (cursor.moveToFirst()) {
                transaction = createTransactionFromCursor(cursor);
                displayTransactionDetails();
            } else {
                Toast.makeText(this, "Không tìm thấy giao dịch", Toast.LENGTH_SHORT).show();
                finish();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tải chi tiết giao dịch", Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private Transaction createTransactionFromCursor(Cursor cursor) {
        Transaction transaction = new Transaction();

        transaction.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        transaction.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
        transaction.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)));
        transaction.setCategoryId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ID)));
        transaction.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));
        transaction.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
        transaction.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAYMENT_METHOD)));
        transaction.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));

        // Category information
        transaction.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_NAME)));
        transaction.setCategoryIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_ICON)));
        transaction.setCategoryColor(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_COLOR)));
        transaction.setCategoryType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY_TYPE)));

        return transaction;
    }

    private void displayTransactionDetails() {
        if (transaction == null) return;

        // Category icon and name
        tvCategoryIcon.setText(transaction.getCategoryIcon());
        tvCategoryName.setText(transaction.getCategoryName());

        // Amount
        String formattedAmount = currencyFormatter.format(transaction.getAmount()) + "₫";
        tvAmount.setText(formattedAmount);

        // Set amount color based on type
        int amountColor = transaction.isIncome() ?
                getResources().getColor(R.color.success) :
                getResources().getColor(R.color.error);
        tvAmount.setTextColor(amountColor);

        // Transaction type
        String typeText = transaction.isIncome() ? "Thu nhập" : "Chi tiêu";
        tvType.setText(typeText);
        tvType.setTextColor(amountColor);

        // Date
        String formattedDate = formatDate(transaction.getDate());
        tvDate.setText(formattedDate);

        // Payment method
        if (transaction.hasPaymentMethod()) {
            tvPaymentMethod.setText(transaction.getPaymentMethod());
        } else {
            tvPaymentMethod.setText("Không có");
        }

        // Note
        if (transaction.hasNote()) {
            tvNote.setText(transaction.getNote());
        } else {
            tvNote.setText("Không có ghi chú");
        }

        // Created date
        String formattedCreatedDate = formatDateTime(transaction.getCreatedAt());
        tvCreatedAt.setText("Tạo lúc: " + formattedCreatedDate);

        // Set card background color based on transaction type
        if (transaction.getCategoryColor() != null) {
            try {
                int color = android.graphics.Color.parseColor(transaction.getCategoryColor());
                // Make the color more transparent for background
                int transparentColor = (color & 0x00FFFFFF) | 0x20000000;
                cardTransaction.setCardBackgroundColor(transparentColor);
            } catch (Exception e) {
                // Use default color if parsing fails
            }
        }
    }

    private String formatDate(String dateStr) {
        try {
            Date date = dateFormatter.parse(dateStr);
            return displayDateFormatter.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatDateTime(String dateTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeStr);
            return dateTimeFormatter.format(date);
        } catch (ParseException e) {
            return dateTimeStr;
        }
    }

    private void editTransaction() {
        if (transaction == null) return;

        try {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("transaction_id", transaction.getId());
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chức năng chỉnh sửa đang được phát triển", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        if (transaction == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch \"" + transaction.getCategoryName() + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTransaction())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTransaction() {
        if (transaction == null) return;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            int result = db.delete(DatabaseHelper.TABLE_TRANSACTIONS,
                    DatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(transaction.getId())});

            if (result > 0) {
                Toast.makeText(this, "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // Notify parent activity to refresh
                finish();
            } else {
                Toast.makeText(this, "Không thể xóa giao dịch", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi xóa giao dịch", Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null) db.close();
        }
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
    protected void onResume() {
        super.onResume();
        // Reload transaction details in case it was edited
        if (transactionId != -1) {
            loadTransactionDetails();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}