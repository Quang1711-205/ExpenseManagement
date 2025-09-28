package adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanagement.R;
import models.BudgetPlan;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdjustBudgetAdapter extends RecyclerView.Adapter<AdjustBudgetAdapter.ViewHolder> {

    private List<BudgetPlan.CategoryBudget> categories;
    private OnBudgetChangedListener listener;
    private NumberFormat currencyFormatter;

    public interface OnBudgetChangedListener {
        void onBudgetAmountChanged();
    }

    public AdjustBudgetAdapter(List<BudgetPlan.CategoryBudget> categories, OnBudgetChangedListener listener) {
        this.categories = categories;
        this.listener = listener;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_adjust_budget_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetPlan.CategoryBudget category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName;
        private TextView tvCurrentSpent;
        private EditText etBudgetAmount;
        private TextView tvUsagePercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCurrentSpent = itemView.findViewById(R.id.tv_current_spent);
            etBudgetAmount = itemView.findViewById(R.id.et_budget_amount);
            tvUsagePercentage = itemView.findViewById(R.id.tv_usage_percentage);
        }

        public void bind(BudgetPlan.CategoryBudget category) {
            tvCategoryName.setText(category.getName());
            tvCurrentSpent.setText("Đã chi: " + currencyFormatter.format(category.getSpentAmount()));

            // Set current budget amount
            etBudgetAmount.setText(String.valueOf((int) category.getAllocatedAmount()));

            // Update usage percentage
            updateUsagePercentage(category);

            // Add text watcher to track changes
            etBudgetAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        double newAmount = Double.parseDouble(s.toString());
                        category.setAllocatedAmount(newAmount);
                        updateUsagePercentage(category);

                        if (listener != null) {
                            listener.onBudgetAmountChanged();
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid input
                    }
                }
            });
        }

        private void updateUsagePercentage(BudgetPlan.CategoryBudget category) {
            double percentage = category.getPercentageUsed();
            tvUsagePercentage.setText(String.format("%.1f%%", percentage));

            // Color coding based on usage
            int color;
            if (percentage > 100) {
                color = android.R.color.holo_red_dark;
            } else if (percentage > 80) {
                color = android.R.color.holo_orange_dark;
            } else {
                color = android.R.color.holo_green_dark;
            }
            tvUsagePercentage.setTextColor(itemView.getContext().getResources().getColor(color));
        }
    }
}