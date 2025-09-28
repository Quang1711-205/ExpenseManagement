// 🎯 GoalsActivity.java - FIXED VERSION
// Fixed: Date calculation bug with proper date parsing

package com.example.expensemanagement;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import database.DatabaseHelper;
import models.Goal;

public class GoalsActivity extends AppCompatActivity {
    private static final String TAG = "GoalsActivity";

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerViewGoals;
    private GoalsAdapter goalsAdapter;
    private TabLayout tabLayout;
    private TextView tvTotalGoals, tvActiveGoals, tvCompletedGoals;
    private FloatingActionButton fabAddGoal, fabAddMoney;
    private LinearLayout layoutEmptyState;

    private int currentUserId;
    private String currentFilter = "all"; // all, active, completed, paused

    // ✅ FIXED: Multiple date formatters to handle different date formats
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // For database dates
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // For display
    private SimpleDateFormat deadlineFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // For goal deadlines

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        // Get userId from intent
        currentUserId = getIntent().getIntExtra("user_id", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "❌ Lỗi: Không xác định được user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupTabs();
        setupRecyclerView();
        loadGoalsStats();
        loadGoals(currentFilter);
    }

    private void initViews() {
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals);
        tabLayout = findViewById(R.id.tabLayoutGoals);
        tvTotalGoals = findViewById(R.id.tvTotalGoals);
        tvActiveGoals = findViewById(R.id.tvActiveGoals);
        tvCompletedGoals = findViewById(R.id.tvCompletedGoals);
        fabAddGoal = findViewById(R.id.fabAddGoal);
        fabAddMoney = findViewById(R.id.fabAddMoney);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Set up click listeners
        fabAddGoal.setOnClickListener(v -> openAddGoalActivity());
        fabAddMoney.setOnClickListener(v -> openAddMoneyActivity());

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initDatabase() {
        dbHelper = new DatabaseHelper(this);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả").setTag("all"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang làm").setTag("active"));
        tabLayout.addTab(tabLayout.newTab().setText("Hoàn thành").setTag("completed"));
        tabLayout.addTab(tabLayout.newTab().setText("Tạm dừng").setTag("paused"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = (String) tab.getTag();
                loadGoals(currentFilter);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        goalsAdapter = new GoalsAdapter();
        recyclerViewGoals.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGoals.setAdapter(goalsAdapter);
    }

    private void loadGoalsStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Count total goals
            Cursor totalCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_GOALS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(currentUserId)}
            );
            int totalCount = 0;
            if (totalCursor.moveToFirst()) {
                totalCount = totalCursor.getInt(0);
            }
            totalCursor.close();

            // Count active goals
            Cursor activeCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_GOALS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_GOAL_STATUS + " = 'active'",
                    new String[]{String.valueOf(currentUserId)}
            );
            int activeCount = 0;
            if (activeCursor.moveToFirst()) {
                activeCount = activeCursor.getInt(0);
            }
            activeCursor.close();

            // Count completed goals
            Cursor completedCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_GOALS +
                            " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_GOAL_STATUS + " = 'completed'",
                    new String[]{String.valueOf(currentUserId)}
            );
            int completedCount = 0;
            if (completedCursor.moveToFirst()) {
                completedCount = completedCursor.getInt(0);
            }
            completedCursor.close();

            // Update UI
            tvTotalGoals.setText(String.valueOf(totalCount));
            tvActiveGoals.setText(String.valueOf(activeCount));
            tvCompletedGoals.setText(String.valueOf(completedCount));

        } catch (Exception e) {
            Log.e(TAG, "Error loading goals stats: " + e.getMessage());
        }
    }

    private void loadGoals(String filter) {
        List<Goal> goals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String selection = DatabaseHelper.COLUMN_USER_ID + " = ?";
            String[] selectionArgs = {String.valueOf(currentUserId)};

            // Add filter condition
            if (!filter.equals("all")) {
                selection += " AND " + DatabaseHelper.COLUMN_GOAL_STATUS + " = ?";
                selectionArgs = new String[]{String.valueOf(currentUserId), filter};
            }

            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_GOALS,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    DatabaseHelper.COLUMN_CREATED_AT + " DESC"
            );

            while (cursor.moveToNext()) {
                Goal goal = new Goal();
                goal.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                // ✅ FIXED: Use COLUMN_NAME instead of COLUMN_GOAL_NAME
                goal.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
                goal.setTargetAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_TARGET_AMOUNT)));
                goal.setCurrentAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_CURRENT_AMOUNT)));
                goal.setDeadline(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_DEADLINE)));
                goal.setIcon(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_ICON)));
                goal.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_GOAL_STATUS)));
                goal.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));

                goals.add(goal);
            }
            cursor.close();

            // Update adapter
            goalsAdapter.updateGoals(goals);

            // Show/hide empty state
            if (goals.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                recyclerViewGoals.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                recyclerViewGoals.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading goals: " + e.getMessage());
            Toast.makeText(this, "❌ Lỗi tải danh sách mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAddGoalActivity() {
        try {
            Intent intent = new Intent(this, AddGoalActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error opening AddGoalActivity: " + e.getMessage());
            Toast.makeText(this, "❌ Chưa tạo màn hình thêm mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAddMoneyActivity() {
        try {
            Intent intent = new Intent(this, AddMoneyToGoalActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivityForResult(intent, 101);
        } catch (Exception e) {
            Log.e(TAG, "Error opening AddMoneyToGoalActivity: " + e.getMessage());
            Toast.makeText(this, "❌ Chưa tạo màn hình thêm tiền vào mục tiêu", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // Refresh data after adding goal or money
            loadGoalsStats();
            loadGoals(currentFilter);
        }
    }

    // ✅ FIXED: Smart date parsing to handle different date formats
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Try different date formats
        SimpleDateFormat[] formatters = {
                deadlineFormat,     // dd/MM/yyyy (for goal deadlines)
                dbDateFormat,       // yyyy-MM-dd (for database dates)
                displayDateFormat   // dd/MM/yyyy (for display dates)
        };

        for (SimpleDateFormat formatter : formatters) {
            try {
                return formatter.parse(dateStr.trim());
            } catch (ParseException e) {
                // Continue to next format
            }
        }

        Log.e(TAG, "Could not parse date: " + dateStr);
        return null;
    }

    // ✅ FIXED: Improved date calculation with proper error handling
    private long calculateDaysLeft(String deadlineStr) {
        Date deadline = parseDate(deadlineStr);
        if (deadline == null) {
            return -999; // Parsing error
        }

        Date today = new Date();
        long diffInMillis = deadline.getTime() - today.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private String formatDeadline(String deadlineStr) {
        Date deadline = parseDate(deadlineStr);
        if (deadline == null) {
            return deadlineStr; // Return original if parsing fails
        }
        return displayDateFormat.format(deadline);
    }

    // Inner class for RecyclerView Adapter
    private class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
        private List<Goal> goals = new ArrayList<>();

        public void updateGoals(List<Goal> newGoals) {
            this.goals.clear();
            this.goals.addAll(newGoals);
            notifyDataSetChanged();
        }

        @Override
        public GoalViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_goal, parent, false);
            return new GoalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(GoalViewHolder holder, int position) {
            Goal goal = goals.get(position);
            holder.bind(goal);
        }

        @Override
        public int getItemCount() {
            return goals.size();
        }

        class GoalViewHolder extends RecyclerView.ViewHolder {
            private TextView tvGoalName, tvAmount, tvDeadline, tvDaysLeft, tvDailyAmount;
            private ProgressBar progressBar;
            private TextView tvProgress, tvIcon;
            private CardView cardGoal;
            private View statusIndicator;

            public GoalViewHolder(View itemView) {
                super(itemView);

                tvGoalName = itemView.findViewById(R.id.tvGoalName);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvDeadline = itemView.findViewById(R.id.tvDeadline);
                tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
                tvDailyAmount = itemView.findViewById(R.id.tvDailyAmount);
                progressBar = itemView.findViewById(R.id.progressBar);
                tvProgress = itemView.findViewById(R.id.tvProgress);
                tvIcon = itemView.findViewById(R.id.tvIcon);
                cardGoal = itemView.findViewById(R.id.cardGoal);
                statusIndicator = itemView.findViewById(R.id.statusIndicator);

                // Click listener to open goal details
                cardGoal.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Goal goal = goals.get(position);
                        openGoalDetails(goal);
                    }
                });
            }

            public void bind(Goal goal) {
                tvGoalName.setText(goal.getName());
                tvIcon.setText(goal.getIcon() != null ? goal.getIcon() : "🎯");

                // Format amounts
                String amountText = String.format("%.0f / %.0f đ",
                        goal.getCurrentAmount(), goal.getTargetAmount());
                tvAmount.setText(amountText);

                // Calculate progress
                int progress = (int) ((goal.getCurrentAmount() * 100) / goal.getTargetAmount());
                progressBar.setProgress(Math.min(progress, 100));
                tvProgress.setText(progress + "%");

                // ✅ FIXED: Deadline formatting and calculation using improved date parsing
                tvDeadline.setText("Hạn: " + formatDeadline(goal.getDeadline()));

                long daysLeft = calculateDaysLeft(goal.getDeadline());

                if (daysLeft == -999) {
                    // Parsing error
                    tvDaysLeft.setText("Lỗi ngày");
                    tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvDailyAmount.setVisibility(View.GONE);
                } else if (daysLeft > 0) {
                    tvDaysLeft.setText("Còn " + daysLeft + " ngày");

                    // Set color based on days left
                    if (daysLeft <= 7) {
                        tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    } else {
                        tvDaysLeft.setTextColor(getResources().getColor(android.R.color.black));
                    }

                    double remaining = goal.getTargetAmount() - goal.getCurrentAmount();
                    double dailyAmount = remaining / daysLeft;

                    if (dailyAmount > 0) {
                        tvDailyAmount.setText(String.format("Cần: %.0f đ/ngày", dailyAmount));
                        tvDailyAmount.setVisibility(View.VISIBLE);
                    } else {
                        tvDailyAmount.setVisibility(View.GONE);
                    }
                } else if (daysLeft == 0) {
                    tvDaysLeft.setText("Hết hạn hôm nay!");
                    tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvDailyAmount.setVisibility(View.GONE);
                } else {
                    tvDaysLeft.setText("Đã quá hạn " + Math.abs(daysLeft) + " ngày");
                    tvDaysLeft.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    tvDailyAmount.setVisibility(View.GONE);
                }

                // Set status indicator color
                int indicatorColor;
                switch (goal.getStatus()) {
                    case "completed":
                        indicatorColor = getResources().getColor(android.R.color.holo_green_dark);
                        break;
                    case "paused":
                        indicatorColor = getResources().getColor(android.R.color.holo_orange_dark);
                        break;
                    case "active":
                    default:
                        indicatorColor = getResources().getColor(android.R.color.holo_blue_bright);
                        break;
                }
                statusIndicator.setBackgroundColor(indicatorColor);
            }
        }
    }

    private void openGoalDetails(Goal goal) {
        try {
            Intent intent = new Intent(this, GoalDetailsActivity.class);
            intent.putExtra("goal_id", goal.getId());
            intent.putExtra("user_id", currentUserId);
            startActivityForResult(intent, 102);
        } catch (Exception e) {
            Log.e(TAG, "Error opening GoalDetailsActivity: " + e.getMessage());
            Toast.makeText(this, "📱 Chi tiết mục tiêu: " + goal.getName() + " (Chưa tạo màn hình chi tiết)", Toast.LENGTH_SHORT).show();
        }
    }
}