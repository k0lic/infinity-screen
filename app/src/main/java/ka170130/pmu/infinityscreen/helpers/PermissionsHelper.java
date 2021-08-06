package ka170130.pmu.infinityscreen.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;

public class PermissionsHelper {

    private static MainActivity mainActivity;
    private static ActivityResultLauncher<String[]> launcher;
    private static Callback<String> callback;

    public static void init(MainActivity mainActivity) {
        PermissionsHelper.mainActivity = mainActivity;

        launcher = mainActivity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                grantedMap -> {
                    Iterator<String> iterator = grantedMap.keySet().iterator();
                    boolean allGranted = true;

                    while (allGranted && iterator.hasNext()) {
                        String key = iterator.next();
                        allGranted = grantedMap.get(key);
                    }

                    if (allGranted) {
                        if (PermissionsHelper.callback != null) {
                            PermissionsHelper.callback.invoke(null);
                        }
                    }
                }
        );
    }

    public static void request(String[] permissions, Callback<String> callback) {
        boolean launch = false;

        for (int i = 0; i < permissions.length && !launch; i++) {
            String perm = permissions[i];
            launch = ActivityCompat.checkSelfPermission(mainActivity, perm)
                    != PackageManager.PERMISSION_GRANTED;
        }

        PermissionsHelper.callback = callback;
        if (launch) {
            launcher.launch(permissions);
        } else {
            if (PermissionsHelper.callback != null) {
                PermissionsHelper.callback.invoke(null);
            }
        }
    }

}
