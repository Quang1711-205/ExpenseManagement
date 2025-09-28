package adapters;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanagement.R;
import models.BudgetPlan;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.BudgetCategoryViewHolder> {

    private List<BudgetPlan.CategoryBudget> budgetCategories;
    private List<BudgetPlan.CategoryBudget> originalCategories; // For filtering/sorting
    private Context context;
    private NumberFormat currencyFormatter;
    private OnBudgetCategoryClickListener listener;

    // Enhanced interface with more interaction options
    public interface OnBudgetCategoryClickListener {
        void onCategoryClick(BudgetPlan.CategoryBudget category, int position);
        void onAdjustBudgetClick(BudgetPlan.CategoryBudget category, int position);
        void onViewDetailsClick(BudgetPlan.CategoryBudget category, int position);
        void onQuickExpenseClick(BudgetPlan.CategoryBudget category, int position);
        void onDeleteCategoryClick(BudgetPlan.CategoryBudget category, int position);
    }

    public BudgetCategoryAdapter(List<BudgetPlan.CategoryBudget> budgetCategories, Context context) {
        this.budgetCategories = budgetCategories;
        this.originalCategories = budgetCategories; // Keep reference to original
        this.context = context;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setOnBudgetCategoryClickListener(OnBudgetCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BudgetCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_category, parent, false);
        return new BudgetCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetCategoryViewHolder holder, int position) {
        BudgetPlan.CategoryBudget category = budgetCategories.get(position);

        // Category basic info with enhanced display
        holder.tvCategoryName.setText(category.getName());
        holder.tvCategoryIcon.setText(category.getIcon() != null ? category.getIcon() : "📂");

        // Budget amounts with better formatting
        holder.tvAllocatedAmount.setText(currencyFormatter.format(category.getAllocatedAmount()));
        holder.tvSpentAmount.setText(currencyFormatter.format(category.getSpentAmount()));

        // Enhanced variance calculation with visual indicators
        double variance = category.getVariance();
        holder.tvVariance.setText(currencyFormatter.format(Math.abs(variance)));

        if (variance >= 0) {
            holder.tvVariance.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvVarianceLabel.setText("Còn lại:");
            holder.ivVarianceIcon.setImageResource(R.drawable.ic_trending_up);
        } else {
            holder.tvVariance.setTextColor(Color.parseColor("#F44336"));
            holder.tvVarianceLabel.setText("Vượt chi:");
            holder.ivVarianceIcon.setImageResource(R.drawable.ic_trending_down);
        }

        // Enhanced progress calculation with smooth animation
        double percentageUsed = category.getPercentageUsed();
        int progressValue = (int) Math.min(percentageUsed, 100);

        // Animate progress bar
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(holder.pbCategoryProgress, "progress", 0, progressValue);
        progressAnimator.setDuration(1000);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.start();

        holder.tvProgressPercentage.setText(String.format("%.1f%%", percentageUsed));

        // Enhanced health status with more detailed information
        setupEnhancedHealthStatus(holder, category, percentageUsed);

        // Category color with enhanced visual feedback
        setupCategoryColor(holder, category);

        // Enhanced click listeners with haptic feedback
        setupClickListeners(holder, category, position);

        // Setup contextual actions based on category state
        setupContextualActions(holder, category, position);

        // Add expense history preview
        setupExpensePreview(holder, category);
    }

    private void setupEnhancedHealthStatus(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category, double percentageUsed) {
        String healthStatus = category.getHealthStatus();

        switch (healthStatus) {
            case "excellent":
                holder.ivHealthStatus.setImageResource(R.drawable.ic_health_excellent);
                holder.pbCategoryProgress.getProgressDrawable().setColorFilter(Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvHealthStatus.setText("Tuyệt vời");
                holder.tvHealthStatus.setTextColor(Color.parseColor("#4CAF50"));
                holder.tvHealthDescription.setText("Quản lý ngân sách rất tốt!");
                holder.layoutHealthStatus.setBackgroundResource(R.drawable.rounded_background_success);
                break;

            case "good":
                holder.ivHealthStatus.setImageResource(R.drawable.ic_health_good);
                holder.pbCategoryProgress.getProgressDrawable().setColorFilter(Color.parseColor("#8BC34A"), android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvHealthStatus.setText("Tốt");
                holder.tvHealthStatus.setTextColor(Color.parseColor("#8BC34A"));
                holder.tvHealthDescription.setText("Đang theo dõi tốt ngân sách");
                holder.layoutHealthStatus.setBackgroundResource(R.drawable.rounded_background_success);
                break;

            case "warning":
                holder.ivHealthStatus.setImageResource(R.drawable.ic_health_warning);
                holder.pbCategoryProgress.getProgressDrawable().setColorFilter(Color.parseColor("#FF9800"), android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvHealthStatus.setText("Cảnh báo");
                holder.tvHealthStatus.setTextColor(Color.parseColor("#FF9800"));
                holder.tvHealthDescription.setText("Cần chú ý chi tiêu");
                holder.layoutHealthStatus.setBackgroundResource(R.drawable.rounded_background_warning);
                break;

            case "danger":
                holder.ivHealthStatus.setImageResource(R.drawable.ic_health_danger);
                holder.pbCategoryProgress.getProgressDrawable().setColorFilter(Color.parseColor("#FF5722"), android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvHealthStatus.setText("Nguy hiểm");
                holder.tvHealthStatus.setTextColor(Color.parseColor("#FF5722"));
                holder.tvHealthDescription.setText("Ngân sách gần hết!");
                holder.layoutHealthStatus.setBackgroundResource(R.drawable.rounded_background_danger);
                break;

            case "critical":
                holder.ivHealthStatus.setImageResource(R.drawable.ic_health_critical);
                holder.pbCategoryProgress.getProgressDrawable().setColorFilter(Color.parseColor("#F44336"), android.graphics.PorterDuff.Mode.SRC_IN);
                holder.tvHealthStatus.setText("Nghiêm trọng");
                holder.tvHealthStatus.setTextColor(Color.parseColor("#F44336"));
                holder.tvHealthDescription.setText("Đã vượt ngân sách!");
                holder.layoutHealthStatus.setBackgroundResource(R.drawable.rounded_background_critical);

                // Add blinking animation for critical status
                ObjectAnimator blinkAnimation = ObjectAnimator.ofFloat(holder.ivHealthStatus, "alpha", 1.0f, 0.3f);
                blinkAnimation.setDuration(500);
                blinkAnimation.setRepeatCount(ObjectAnimator.INFINITE);
                blinkAnimation.setRepeatMode(ObjectAnimator.REVERSE);
                blinkAnimation.start();
                break;
        }

        // Show/hide overflow indicator with enhanced styling
        if (percentageUsed > 100) {
            holder.tvOverflowIndicator.setVisibility(View.VISIBLE);
            holder.tvOverflowIndicator.setText("+" + String.format("%.0f%%", percentageUsed - 100));
            holder.tvOverflowIndicator.setBackgroundResource(R.drawable.rounded_background_critical);

            // Pulse animation for overflow
            ObjectAnimator pulseAnimation = ObjectAnimator.ofFloat(holder.tvOverflowIndicator, "scaleX", 1.0f, 1.1f);
            pulseAnimation.setDuration(800);
            pulseAnimation.setRepeatCount(ObjectAnimator.INFINITE);
            pulseAnimation.setRepeatMode(ObjectAnimator.REVERSE);
            pulseAnimation.start();
        } else {
            holder.tvOverflowIndicator.setVisibility(View.GONE);
        }
    }

    private void setupCategoryColor(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category) {
        try {
            int categoryColor = Color.parseColor(category.getColor() != null ? category.getColor() : "#2196F3");
            holder.viewColorIndicator.setBackgroundColor(categoryColor);

            // Enhanced icon styling with category color
            holder.tvCategoryIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(categoryColor));

            // Add subtle gradient effect
            android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable();
            gradient.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gradient.setCornerRadius(16f);
            gradient.setColors(new int[]{categoryColor, adjustAlpha(categoryColor, 0.3f)});
            holder.layoutCategoryHeader.setBackground(gradient);

        } catch (Exception e) {
            // Fallback colors
            holder.viewColorIndicator.setBackgroundColor(Color.parseColor("#2196F3"));
            holder.tvCategoryIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3")));
        }
    }

    private void setupClickListeners(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category, int position) {
        // Main item click with ripple effect
        holder.itemView.setOnClickListener(v -> {
            // Add haptic feedback
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

            if (listener != null) {
                listener.onCategoryClick(category, position);
            }
        });

        // Long press for quick actions
        holder.itemView.setOnLongClickListener(v -> {
            showQuickActionMenu(v, category, position);
            return true;
        });

        // Individual button clicks
        holder.btnAdjustBudget.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onAdjustBudgetClick(category, position);
            }
        });

        holder.btnViewDetails.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onViewDetailsClick(category, position);
            }
        });

        holder.btnQuickExpense.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onQuickExpenseClick(category, position);
            }
        });

        // Swipe actions (if implemented)
        setupSwipeActions(holder, category, position);
    }

    private void setupContextualActions(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category, int position) {
        double percentageUsed = category.getPercentageUsed();

        // Show different actions based on category status
        if (percentageUsed > 100) {
            // Over budget - emphasize adjustment
            holder.btnAdjustBudget.setBackgroundResource(R.drawable.rounded_button_critical);
            holder.btnAdjustBudget.setText("⚠️ Điều chỉnh");
        } else if (percentageUsed > 80) {
            // Near limit - show warning
            holder.btnAdjustBudget.setBackgroundResource(R.drawable.rounded_button_warning);
            holder.btnAdjustBudget.setText("⚙️ Điều chỉnh");
        } else {
            // Normal state
            holder.btnAdjustBudget.setBackgroundResource(R.drawable.rounded_button_info);
            holder.btnAdjustBudget.setText("⚙️ Điều chỉnh");
        }

        // Show quick expense button more prominently if under budget
        if (percentageUsed < 70) {
            holder.btnQuickExpense.setVisibility(View.VISIBLE);
            holder.btnQuickExpense.setBackgroundResource(R.drawable.rounded_button_success);
        } else if (percentageUsed > 95) {
            holder.btnQuickExpense.setVisibility(View.GONE);
        } else {
            holder.btnQuickExpense.setVisibility(View.VISIBLE);
            holder.btnQuickExpense.setBackgroundResource(R.drawable.rounded_button_accent);
        }
    }

    private void setupExpensePreview(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category) {
        // Show recent expense count or trend
//        holder.tvRecentExpenses.setText("Gần đây: " + getRecentExpenseCount(category) + " giao dịch");

        // Show trend indicator
//        double trend = calculateTrend(category);
//        if (trend > 0) {
//            holder.ivTrendIndicator.setImageResource(R.drawable.ic_trending_up);
//            holder.ivTrendIndicator.setColorFilter(Color.parseColor("#F44336"));
//            holder.tvTrendText.setText("Tăng " + String.format("%.0f%%", trend));
//        } else if (trend < 0) {
//            holder.ivTrendIndicator.setImageResource(R.drawable.ic_trending_down);
//            holder.ivTrendIndicator.setColorFilter(Color.parseColor("#4CAF50"));
//            holder.tvTrendText.setText("Giảm " + String.format("%.0f%%", Math.abs(trend)));
//        } else {
//            holder.ivTrendIndicator.setImageResource(R.drawable.ic_trending_flat);
//            holder.ivTrendIndicator.setColorFilter(Color.parseColor("#666666"));
//            holder.tvTrendText.setText("Ổn định");
//        }
    }

    private void setupSwipeActions(BudgetCategoryViewHolder holder, BudgetPlan.CategoryBudget category, int position) {
        // Implementation for swipe-to-action functionality
        // This would require additional gesture detection
    }

    private void showQuickActionMenu(View anchor, BudgetPlan.CategoryBudget category, int position) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenuInflater().inflate(R.menu.category_quick_actions, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_quick_expense) {
                if (listener != null) listener.onQuickExpenseClick(category, position);
                return true;
            } else if (itemId == R.id.action_adjust_budget) {
                if (listener != null) listener.onAdjustBudgetClick(category, position);
                return true;
            } else if (itemId == R.id.action_view_details) {
                if (listener != null) listener.onViewDetailsClick(category, position);
                return true;
            } else if (itemId == R.id.action_delete_category) {
                showDeleteConfirmation(category, position);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showDeleteConfirmation(BudgetPlan.CategoryBudget category, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa danh mục ngân sách?")
                .setMessage("Bạn có chắc chắn muốn xóa ngân sách cho danh mục \"" + category.getName() + "\"?\n\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (listener != null) {
                        listener.onDeleteCategoryClick(category, position);
                    }
                })
                .setNegativeButton("Hủy", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    // Utility methods
    private int adjustAlpha(int color, float alpha) {
        int alphaValue = Math.round(Color.alpha(color) * alpha);
        return Color.argb(alphaValue, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int getRecentExpenseCount(BudgetPlan.CategoryBudget category) {
        // This would query the database for recent expenses
        // For now, return a placeholder
        return (int) (Math.random() * 10) + 1;
    }

    private double calculateTrend(BudgetPlan.CategoryBudget category) {
        // This would calculate spending trend compared to previous period
        // For now, return a placeholder
        return (Math.random() - 0.5) * 40; // Random trend between -20% to +20%
    }

    @Override
    public int getItemCount() {
        return budgetCategories != null ? budgetCategories.size() : 0;
    }

    // Public methods for external manipulation
    public void updateData(List<BudgetPlan.CategoryBudget> newCategories) {
        this.budgetCategories = newCategories;
        notifyDataSetChanged();
    }

    public void updateCategory(int position, BudgetPlan.CategoryBudget updatedCategory) {
        if (position >= 0 && position < budgetCategories.size()) {
            budgetCategories.set(position, updatedCategory);
            notifyItemChanged(position);
        }
    }

    public void removeCategory(int position) {
        if (position >= 0 && position < budgetCategories.size()) {
            budgetCategories.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, budgetCategories.size());
        }
    }

    public void addCategory(BudgetPlan.CategoryBudget category) {
        budgetCategories.add(category);
        notifyItemInserted(budgetCategories.size() - 1);
    }

    // Filter and sort methods
    public void filterCategories(String query) {
        if (query.isEmpty()) {
            budgetCategories = originalCategories;
        } else {
            budgetCategories = originalCategories.stream()
                    .filter(category -> category.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        notifyDataSetChanged();
    }

    public void sortCategories(String sortType) {
        switch (sortType.toLowerCase()) {
            case "name_asc":
                budgetCategories.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case "name_desc":
                budgetCategories.sort((a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                break;
            case "amount_desc":
                budgetCategories.sort((a, b) -> Double.compare(b.getAllocatedAmount(), a.getAllocatedAmount()));
                break;
            case "amount_asc":
                budgetCategories.sort((a, b) -> Double.compare(a.getAllocatedAmount(), b.getAllocatedAmount()));
                break;
            case "usage_desc":
                budgetCategories.sort((a, b) -> Double.compare(b.getPercentageUsed(), a.getPercentageUsed()));
                break;
            case "usage_asc":
                budgetCategories.sort((a, b) -> Double.compare(a.getPercentageUsed(), b.getPercentageUsed()));
                break;
        }
        notifyDataSetChanged();
    }

    // Enhanced ViewHolder with more UI components
    static class BudgetCategoryViewHolder extends RecyclerView.ViewHolder {
        // Basic components
        TextView tvCategoryName, tvCategoryIcon, tvAllocatedAmount, tvSpentAmount;
        TextView tvVariance, tvVarianceLabel, tvProgressPercentage, tvHealthStatus, tvHealthDescription;
        TextView tvOverflowIndicator, tvRecentExpenses, tvTrendText;
        ProgressBar pbCategoryProgress;
        ImageView ivHealthStatus, ivVarianceIcon, ivTrendIndicator;
        View viewColorIndicator;
        LinearLayout layoutCategoryHeader, layoutHealthStatus;

        // Action buttons
        Button btnAdjustBudget, btnViewDetails, btnQuickExpense;

        // Menu button
        ImageView ivMenuButton;

        public BudgetCategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find all views
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCategoryIcon = itemView.findViewById(R.id.tv_category_icon);
            tvAllocatedAmount = itemView.findViewById(R.id.tv_allocated_amount);
            tvSpentAmount = itemView.findViewById(R.id.tv_spent_amount);
            tvVariance = itemView.findViewById(R.id.tv_variance);
            tvVarianceLabel = itemView.findViewById(R.id.tv_variance_label);
            tvProgressPercentage = itemView.findViewById(R.id.tv_progress_percentage);
            tvHealthStatus = itemView.findViewById(R.id.tv_health_status);
            tvHealthDescription = itemView.findViewById(R.id.tv_health_description);
            tvOverflowIndicator = itemView.findViewById(R.id.tv_overflow_indicator);
//            tvRecentExpenses = itemView.findViewById(R.id.tv_recent_expenses);
//            tvTrendText = itemView.findViewById(R.id.tv_trend_text);

            pbCategoryProgress = itemView.findViewById(R.id.pb_category_progress);

            ivHealthStatus = itemView.findViewById(R.id.iv_health_status);
            ivVarianceIcon = itemView.findViewById(R.id.iv_variance_icon);
//            ivTrendIndicator = itemView.findViewById(R.id.iv_trend_indicator);
            ivMenuButton = itemView.findViewById(R.id.iv_menu_button);

            viewColorIndicator = itemView.findViewById(R.id.view_color_indicator);
            layoutCategoryHeader = itemView.findViewById(R.id.layout_category_header);
            layoutHealthStatus = itemView.findViewById(R.id.layout_health_status);

            btnAdjustBudget = itemView.findViewById(R.id.btn_adjust_budget);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            btnQuickExpense = itemView.findViewById(R.id.btn_quick_expense);
        }
    }
}