package adapters;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import models.Budget;
import com.example.expensemanagement.R;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    private List<Budget> budgetList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Budget budget);
        void onMoreOptionsClick(Budget budget);
        void onQuickExpenseClick(Budget budget);
    }

    public BudgetAdapter(Context context, List<Budget> budgetList) {
        this.context = context;
        this.budgetList = budgetList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        // Bind data to views
        holder.tvBudgetIcon.setText(budget.getIcon());
        holder.tvBudgetName.setText(budget.getName());
        holder.tvBudgetPeriod.setText(budget.getPeriod());
        holder.tvBudgetStatus.setText(budget.getStatus());
        holder.tvTotalBudget.setText(budget.getFormattedTotalAmount());
        holder.tvSpentAmount.setText(budget.getFormattedSpentAmount());
        holder.tvProgressPercentage.setText(budget.getProgressPercentage() + "%");
        holder.tvRemainingDays.setText("• " + budget.getRemainingDays() + " ngày còn lại");
        holder.tvCategoriesCount.setText(budget.getCategoriesCount() + " danh mục");
        holder.tvHealthScore.setText(budget.getHealthScore());

        // Set progress bar
        holder.pbBudgetProgress.setProgress(budget.getProgressPercentage());

        // Set status color based on status
        setStatusColor(holder, budget.getStatus());

        // Set health score color
        setHealthScoreColor(holder, budget.getHealthScore());

        // Set click listeners
        holder.layoutBudgetItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(budget);
            }
        });

        holder.ivMoreOptions.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreOptionsClick(budget);
            }
        });

        holder.ivQuickExpense.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuickExpenseClick(budget);
            }
        });

        holder.ivViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(budget);
            }
        });
    }

    private void setStatusColor(ViewHolder holder, String status) {
        switch (status) {
            case "Đang hoạt động":
                holder.tvBudgetStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "Hoàn thành":
                holder.tvBudgetStatus.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "Tạm dừng":
                holder.tvBudgetStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            default:
                holder.tvBudgetStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void setHealthScoreColor(ViewHolder holder, String healthScore) {
        switch (healthScore) {
            case "Tốt":
                holder.tvHealthScore.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "Trung bình":
                holder.tvHealthScore.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "Cần cải thiện":
                holder.tvHealthScore.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public void updateList(List<Budget> newList) {
        this.budgetList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBudgetIcon, tvBudgetName, tvBudgetPeriod, tvBudgetStatus;
        TextView tvTotalBudget, tvSpentAmount, tvProgressPercentage, tvRemainingDays;
        TextView tvCategoriesCount, tvHealthScore;
        ProgressBar pbBudgetProgress;
        ImageView ivMoreOptions, ivQuickExpense, ivViewDetails;
        View layoutBudgetItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutBudgetItem = itemView.findViewById(R.id.layout_budget_item);
            tvBudgetIcon = itemView.findViewById(R.id.tv_budget_icon);
            tvBudgetName = itemView.findViewById(R.id.tv_budget_name);
            tvBudgetPeriod = itemView.findViewById(R.id.tv_budget_period);
            tvBudgetStatus = itemView.findViewById(R.id.tv_budget_status);
            tvTotalBudget = itemView.findViewById(R.id.tv_total_budget);
            tvSpentAmount = itemView.findViewById(R.id.tv_spent_amount);
            tvProgressPercentage = itemView.findViewById(R.id.tv_progress_percentage);
            tvRemainingDays = itemView.findViewById(R.id.tv_remaining_days);
            tvCategoriesCount = itemView.findViewById(R.id.tv_categories_count);
            tvHealthScore = itemView.findViewById(R.id.tv_health_score);
            pbBudgetProgress = itemView.findViewById(R.id.pb_budget_progress);
            ivMoreOptions = itemView.findViewById(R.id.iv_more_options);
            ivQuickExpense = itemView.findViewById(R.id.iv_quick_expense);
            ivViewDetails = itemView.findViewById(R.id.iv_view_details);
        }
    }
}