package ka170130.pmu.infinityscreen.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.R;

public class SettingsPanelDialog extends DialogFragment {

    private String message;
    private String action;

    private final ActivityResultLauncher<Intent> settingsPanelLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            intent -> {
                Log.d(MainActivity.LOG_TAG, "SettingsPanel activity returned");
            }
    );

    public SettingsPanelDialog(String message, String action) {
        this.message = message;
        this.action = action;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        builder
                .setTitle(R.string.dialog_settings_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
                    settingsPanelLauncher.launch(new Intent(action));
                });

        return builder.create();
    }
}
