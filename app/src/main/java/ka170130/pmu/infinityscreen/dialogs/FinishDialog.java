package ka170130.pmu.infinityscreen.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import ka170130.pmu.infinityscreen.R;

public class FinishDialog extends DialogFragment {

    private String message;

    public FinishDialog(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        String message =
                this.message + " " + getResources().getString(R.string.dialog_finish_closing);

        builder
                .setTitle(R.string.dialog_finish_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, id) -> {
                   finish();
                });

        return builder.create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        finish();
    }

    private void finish() {
        requireActivity().finish();
    }
}
