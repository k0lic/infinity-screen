package ka170130.pmu.infinityscreen.helpers;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ka170130.pmu.infinityscreen.MainActivity;

public class FileSelectionHelper {

    private static MainActivity mainActivity;
    private static ActivityResultLauncher<String[]> launcher;
    private static Callback<ArrayList<String>> callback;

    public static void init(MainActivity mainActivity) {
        FileSelectionHelper.mainActivity = mainActivity;

        launcher = mainActivity.registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                result -> {
                    if (callback != null) {
                        ContentResolver contentResolver = mainActivity.getContentResolver();
                        ArrayList<String> stringList = new ArrayList<>();

                        Iterator<Uri> iterator = result.iterator();
                        while (iterator.hasNext()) {
                            Uri next = iterator.next();

                            contentResolver.takePersistableUriPermission(next, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            stringList.add(next.toString());
                        }

                        callback.invoke(stringList);
                    }
                }
        );
    }

    public static void request(String[] mimeTypes, Callback<ArrayList<String>> callback) {
        FileSelectionHelper.callback = callback;
        launcher.launch(mimeTypes);
    }
}
