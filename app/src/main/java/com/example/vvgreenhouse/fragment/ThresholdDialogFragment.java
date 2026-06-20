package com.example.vvgreenhouse.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.vvgreenhouse.R;
import com.example.vvgreenhouse.model.ThresholdConfig;

/**
 * 阈值设置对话框
 */
public class ThresholdDialogFragment extends DialogFragment {

    private ThresholdConfig originalConfig;
    private OnThresholdSavedListener listener;

    public interface OnThresholdSavedListener {
        void onThresholdSaved(ThresholdConfig config);
    }

    public static ThresholdDialogFragment newInstance(ThresholdConfig config) {
        ThresholdDialogFragment f = new ThresholdDialogFragment();
        Bundle args = new Bundle();
        f.originalConfig = config;
        return f;
    }

    public void setOnThresholdSavedListener(OnThresholdSavedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_threshold, null);

        // 获取所有输入框引用
        EditText etTempMin = view.findViewById(R.id.et_temp_min);
        EditText etTempMax = view.findViewById(R.id.et_temp_max);
        EditText etHumidityMin = view.findViewById(R.id.et_humidity_min);
        EditText etHumidityMax = view.findViewById(R.id.et_humidity_max);
        EditText etCo2Min = view.findViewById(R.id.et_co2_min);
        EditText etCo2Max = view.findViewById(R.id.et_co2_max);
        EditText etLightMin = view.findViewById(R.id.et_light_min);
        EditText etLightMax = view.findViewById(R.id.et_light_max);
        EditText etSoilTempMin = view.findViewById(R.id.et_soil_temp_min);
        EditText etSoilTempMax = view.findViewById(R.id.et_soil_temp_max);
        EditText etSoilHumiMin = view.findViewById(R.id.et_soil_humidity_min);
        EditText etSoilHumiMax = view.findViewById(R.id.et_soil_humidity_max);
        EditText etPhMin = view.findViewById(R.id.et_ph_min);
        EditText etPhMax = view.findViewById(R.id.et_ph_max);
        EditText etEcMin = view.findViewById(R.id.et_ec_min);
        EditText etEcMax = view.findViewById(R.id.et_ec_max);

        // 填充当前值
        loadConfigToFields(originalConfig == null ? ThresholdConfig.defaults() : originalConfig,
                etTempMin, etTempMax, etHumidityMin, etHumidityMax,
                etCo2Min, etCo2Max, etLightMin, etLightMax,
                etSoilTempMin, etSoilTempMax, etSoilHumiMin, etSoilHumiMax,
                etPhMin, etPhMax, etEcMin, etEcMax);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        view.findViewById(R.id.btn_save_threshold).setOnClickListener(v -> {
            ThresholdConfig cfg = readConfigFromFields(
                    etTempMin, etTempMax, etHumidityMin, etHumidityMax,
                    etCo2Min, etCo2Max, etLightMin, etLightMax,
                    etSoilTempMin, etSoilTempMax, etSoilHumiMin, etSoilHumiMax,
                    etPhMin, etPhMax, etEcMin, etEcMax);

            Context ctx = getContext();
            if (ctx != null) {
                cfg.save(ctx);
                Toast.makeText(ctx, "阈值已保存", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onThresholdSaved(cfg);
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_reset_defaults).setOnClickListener(v -> {
            ThresholdConfig def = ThresholdConfig.defaults();
            loadConfigToFields(def,
                    etTempMin, etTempMax, etHumidityMin, etHumidityMax,
                    etCo2Min, etCo2Max, etLightMin, etLightMax,
                    etSoilTempMin, etSoilTempMax, etSoilHumiMin, etSoilHumiMax,
                    etPhMin, etPhMax, etEcMin, etEcMax);
        });

        return dialog;
    }

    // ========== 辅助方法 ==========

    private void loadConfigToFields(ThresholdConfig c,
                                    EditText etTempMin, EditText etTempMax,
                                    EditText etHumidityMin, EditText etHumidityMax,
                                    EditText etCo2Min, EditText etCo2Max,
                                    EditText etLightMin, EditText etLightMax,
                                    EditText etSoilTempMin, EditText etSoilTempMax,
                                    EditText etSoilHumiMin, EditText etSoilHumiMax,
                                    EditText etPhMin, EditText etPhMax,
                                    EditText etEcMin, EditText etEcMax) {
        etTempMin.setText(String.valueOf(c.tempMin));
        etTempMax.setText(String.valueOf(c.tempMax));
        etHumidityMin.setText(String.valueOf(c.humidityMin));
        etHumidityMax.setText(String.valueOf(c.humidityMax));
        etCo2Min.setText(String.valueOf(c.co2Min));
        etCo2Max.setText(String.valueOf(c.co2Max));
        etLightMin.setText(String.valueOf(c.lightMin));
        etLightMax.setText(String.valueOf(c.lightMax));
        etSoilTempMin.setText(String.valueOf(c.soilTempMin));
        etSoilTempMax.setText(String.valueOf(c.soilTempMax));
        etSoilHumiMin.setText(String.valueOf(c.soilHumidityMin));
        etSoilHumiMax.setText(String.valueOf(c.soilHumidityMax));
        etPhMin.setText(String.valueOf(c.phMin));
        etPhMax.setText(String.valueOf(c.phMax));
        etEcMin.setText(String.valueOf(c.ecMin));
        etEcMax.setText(String.valueOf(c.ecMax));
    }

    private ThresholdConfig readConfigFromFields(
            EditText etTempMin, EditText etTempMax,
            EditText etHumidityMin, EditText etHumidityMax,
            EditText etCo2Min, EditText etCo2Max,
            EditText etLightMin, EditText etLightMax,
            EditText etSoilTempMin, EditText etSoilTempMax,
            EditText etSoilHumiMin, EditText etSoilHumiMax,
            EditText etPhMin, EditText etPhMax,
            EditText etEcMin, EditText etEcMax) {
        ThresholdConfig c = new ThresholdConfig();
        c.tempMin = parseFloat(etTempMin, c.tempMin);
        c.tempMax = parseFloat(etTempMax, c.tempMax);
        c.humidityMin = parseFloat(etHumidityMin, c.humidityMin);
        c.humidityMax = parseFloat(etHumidityMax, c.humidityMax);
        c.co2Min = parseFloat(etCo2Min, c.co2Min);
        c.co2Max = parseFloat(etCo2Max, c.co2Max);
        c.lightMin = parseFloat(etLightMin, c.lightMin);
        c.lightMax = parseFloat(etLightMax, c.lightMax);
        c.soilTempMin = parseFloat(etSoilTempMin, c.soilTempMin);
        c.soilTempMax = parseFloat(etSoilTempMax, c.soilTempMax);
        c.soilHumidityMin = parseFloat(etSoilHumiMin, c.soilHumidityMin);
        c.soilHumidityMax = parseFloat(etSoilHumiMax, c.soilHumidityMax);
        c.phMin = parseFloat(etPhMin, c.phMin);
        c.phMax = parseFloat(etPhMax, c.phMax);
        c.ecMin = parseFloat(etEcMin, c.ecMin);
        c.ecMax = parseFloat(etEcMax, c.ecMax);
        return c;
    }

    private float parseFloat(EditText et, float fallback) {
        try {
            return Float.parseFloat(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
