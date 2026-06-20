package com.example.vvgreenhouse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.vvgreenhouse.R;

/**
 * 占位 Fragment —— 用于尚未实现的模块
 * 显示模块名称 + "功能开发中"
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    public static PlaceholderFragment newInstance(String title) {
        PlaceholderFragment f = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        Bundle args = getArguments();
        if (args != null) {
            tvTitle.setText(args.getString(ARG_TITLE, "") + "模块");
        }
        return view;
    }
}
