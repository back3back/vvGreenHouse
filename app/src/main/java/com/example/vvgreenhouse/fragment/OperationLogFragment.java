package com.example.vvgreenhouse.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.database.GreenhouseDBHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备操作日志查看页
 */
public class OperationLogFragment extends Fragment {

    private RecyclerView rvLogs;
    private TextView tvEmpty;
    private GreenhouseDBHelper dbHelper;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_operation_log, container, false);

        dbHelper = GreenhouseDBHelper.getInstance(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        rvLogs = view.findViewById(R.id.rv_logs);
        tvEmpty = view.findViewById(R.id.tv_empty_logs);
        rvLogs.setLayoutManager(new LinearLayoutManager(getContext()));

        // 返回
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        loadLogs();

        return view;
    }

    private void loadLogs() {
        new Thread(() -> {
            List<String> logs = dbHelper.getDeviceLogs(200);
            mainHandler.post(() -> {
                if (logs.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvLogs.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvLogs.setVisibility(View.VISIBLE);
                    rvLogs.setAdapter(new LogAdapter(logs));
                }
            });
        }).start();
    }

    // ==================== Adapter ====================

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private final List<String> items;

        LogAdapter(List<String> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(24, 14, 24, 14);
            tv.setTextSize(13f);
            tv.setTextColor(0xFF424242);
            tv.setLineSpacing(2f, 1f);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.textView.setText(items.get(position));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView textView;
            VH(@NonNull View itemView) { super(itemView); textView = (TextView) itemView; }
        }
    }
}
