// 🧠 SmartInsightAdapter.java - FIXED for enum usage
package adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensemanagement.R;
import models.SmartInsight;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * 🧠 FIXED: Smart Insight Adapter with proper enum comparisons
 */
public class SmartInsightAdapter extends RecyclerView.Adapter<SmartInsightAdapter.SmartInsightViewHolder> {

    private List<SmartInsight> insights;
    private NumberFormat currencyFormatter;
    private OnInsightClickListener listener;

    public interface OnInsightClickListener {
        void onInsightClick(SmartInsight insight, int position);
        void onActionClick(SmartInsight insight, int position);
    }

    public SmartInsightAdapter(List<SmartInsight> insights) {
        this.insights = insights;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void setOnInsightClickListener(OnInsightClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SmartInsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smart_insight, parent, false);
        return new SmartInsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmartInsightViewHolder holder, int position) {
        SmartInsight insight = insights.get(position);

        // Basic info
        holder.tvInsightTitle.setText(insight.getTitle());
        holder.tvInsightDescription.setText(insight.getMessage());

        // Icon and priority setup
        setupInsightVisuals(holder, insight);

        // Value display (if applicable)
        if (insight.getValue() != null) {
            holder.tvInsightValue.setVisibility(View.VISIBLE);

            // Sử dụng getFormattedValue() từ model
            String formattedValue = insight.getFormattedValue();
            if (formattedValue.isEmpty()) {
                // Fallback formatting nếu getFormattedValue() trả về rỗng
                // ✅ FIXED: Proper enum comparison
                if (insight.getType() == SmartInsight.Type.FORECAST ||
                        insight.getType() == SmartInsight.Type.OPTIMIZATION) {
                    holder.tvInsightValue.setText(currencyFormatter.format(Math.abs(insight.getValue())));
                } else {
                    holder.tvInsightValue.setText(String.valueOf(insight.getValue().intValue()));
                }
            } else {
                holder.tvInsightValue.setText(formattedValue);
            }

            // Color based on insight type and impact
            if (insight.isPositive()) {
                holder.tvInsightValue.setTextColor(Color.GREEN);
            } else if (insight.isHighPriority()) {
                holder.tvInsightValue.setTextColor(Color.RED);
            } else {
                holder.tvInsightValue.setTextColor(Color.parseColor("#FF9800"));
            }
        } else {
            holder.tvInsightValue.setVisibility(View.GONE);
        }

        // Action button setup
        if (insight.isActionable() && insight.getSuggestion() != null) {
            holder.tvActionButton.setVisibility(View.VISIBLE);
            holder.tvActionButton.setText(insight.getSuggestion());
            holder.tvActionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(insight, position);
                }
            });
        } else {
            holder.tvActionButton.setVisibility(View.GONE);
        }

        // Confidence indicator - tính toán dựa trên impact
        int confidence = calculateConfidence(insight);
        if (confidence > 0) {
            holder.viewConfidenceBar.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = holder.viewConfidenceBar.getLayoutParams();
            params.width = (int) (holder.itemView.getWidth() * 0.3 * (confidence / 100.0));
            holder.viewConfidenceBar.setLayoutParams(params);

            // Color based on confidence level
            if (confidence >= 80) {
                holder.viewConfidenceBar.setBackgroundColor(Color.GREEN);
            } else if (confidence >= 60) {
                holder.viewConfidenceBar.setBackgroundColor(Color.parseColor("#FF9800"));
            } else {
                holder.viewConfidenceBar.setBackgroundColor(Color.parseColor("#FF5722"));
            }
        } else {
            holder.viewConfidenceBar.setVisibility(View.GONE);
        }

        // Item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInsightClick(insight, position);
            }
        });

        // Priority indicator - sử dụng Impact enum
        setupPriorityIndicator(holder, insight.getImpact());
    }

    private int calculateConfidence(SmartInsight insight) {
        if (insight.getImpact() == null) return 60;

        // ✅ FIXED: Proper enum comparison
        switch (insight.getImpact()) {
            case HIGH: return 90;
            case MEDIUM: return 70;
            case LOW: return 50;
            case POSITIVE: return 85;
            default: return 60;
        }
    }

    private void setupInsightVisuals(SmartInsightViewHolder holder, SmartInsight insight) {
        if (insight.getType() == null) {
            holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#607D8B"));
            return;
        }

        // ✅ FIXED: Proper enum comparison
        switch (insight.getType()) {
            case PATTERN:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_search);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#FFEB3B"));
                break;

            case TREND:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#9C27B0"));
                break;

            case WARNING:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#FF9800"));
                break;

            case RISK:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#F44336"));
                break;

            case OPTIMIZATION:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_preferences);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#2196F3"));
                break;

            case FORECAST:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;

            case POSITIVE:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#607D8B"));
                break;

            default:
                holder.ivInsightIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                holder.viewInsightBackground.setBackgroundColor(Color.parseColor("#607D8B"));
                break;
        }

        // Add pulsing animation for high priority insights
        if (insight.isHighPriority()) {
            // Tạo animation đơn giản thay vì sử dụng file external
            android.view.animation.Animation pulse = new android.view.animation.AlphaAnimation(0.3f, 1.0f);
            pulse.setDuration(1000);
            pulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
            pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            holder.ivInsightIcon.startAnimation(pulse);
        }
    }

    private void setupPriorityIndicator(SmartInsightViewHolder holder, SmartInsight.Impact impact) {
        if (impact == null) {
            holder.viewPriorityIndicator.setBackgroundColor(Color.GRAY);
            holder.tvPriorityText.setText("Bình thường");
            holder.tvPriorityText.setTextColor(Color.GRAY);
            return;
        }

        // ✅ FIXED: Proper enum comparison
        switch (impact) {
            case HIGH:
                holder.viewPriorityIndicator.setBackgroundColor(Color.RED);
                holder.tvPriorityText.setText("Cao");
                holder.tvPriorityText.setTextColor(Color.RED);
                break;

            case MEDIUM:
                holder.viewPriorityIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
                holder.tvPriorityText.setText("Trung bình");
                holder.tvPriorityText.setTextColor(Color.parseColor("#FF9800"));
                break;

            case LOW:
                holder.viewPriorityIndicator.setBackgroundColor(Color.parseColor("#2196F3"));
                holder.tvPriorityText.setText("Thấp");
                holder.tvPriorityText.setTextColor(Color.parseColor("#2196F3"));
                break;

            case POSITIVE:
                holder.viewPriorityIndicator.setBackgroundColor(Color.GREEN);
                holder.tvPriorityText.setText("Tích cực");
                holder.tvPriorityText.setTextColor(Color.GREEN);
                break;

            default:
                holder.viewPriorityIndicator.setBackgroundColor(Color.GRAY);
                holder.tvPriorityText.setText("Bình thường");
                holder.tvPriorityText.setTextColor(Color.GRAY);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return insights != null ? insights.size() : 0;
    }

    public void updateInsights(List<SmartInsight> newInsights) {
        this.insights = newInsights;
        notifyDataSetChanged();
    }

    public void addInsight(SmartInsight insight) {
        if (insights != null) {
            insights.add(0, insight); // Add to top for latest insights
            notifyItemInserted(0);
        }
    }

    public void removeInsight(int position) {
        if (insights != null && position >= 0 && position < insights.size()) {
            insights.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Dismiss insight
    public void dismissInsight(int position) {
        if (insights != null && position >= 0 && position < insights.size()) {
            SmartInsight insight = insights.get(position);
            insight.setDismissed(true);
            insights.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class SmartInsightViewHolder extends RecyclerView.ViewHolder {
        TextView tvInsightTitle, tvInsightDescription, tvInsightValue;
        TextView tvActionButton, tvPriorityText;
        ImageView ivInsightIcon;
        View viewInsightBackground, viewPriorityIndicator, viewConfidenceBar;

        public SmartInsightViewHolder(@NonNull View itemView) {
            super(itemView);

            // Text Views
            tvInsightTitle = itemView.findViewById(R.id.tv_insight_title);
            tvInsightDescription = itemView.findViewById(R.id.tv_insight_description);
            tvInsightValue = itemView.findViewById(R.id.tv_insight_value);
            tvActionButton = itemView.findViewById(R.id.tv_action_button);
            tvPriorityText = itemView.findViewById(R.id.tv_priority_text);

            // Image Views
            ivInsightIcon = itemView.findViewById(R.id.iv_insight_icon);

            // Views
            viewInsightBackground = itemView.findViewById(R.id.view_insight_background);
            viewPriorityIndicator = itemView.findViewById(R.id.view_priority_indicator);
            viewConfidenceBar = itemView.findViewById(R.id.view_confidence_bar);
        }
    }
}