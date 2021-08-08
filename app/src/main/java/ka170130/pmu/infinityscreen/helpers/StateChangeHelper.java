package ka170130.pmu.infinityscreen.helpers;

import android.util.Log;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.StateViewModel;

public class StateChangeHelper {

    public static void requestStateChange(
            MainActivity mainActivity,
            ConnectionViewModel connectionViewModel,
            StateViewModel.AppState state
    ) {
        try {
            Boolean isHost = connectionViewModel.getIsHost().getValue();
            PeerInetAddressInfo host = connectionViewModel.getHostDevice().getValue();

            if (isHost) {
                mainActivity.getTaskManager().runBroadcastTask(
                        Message.newStateChangeMessage(state)
                );
            } else {
                mainActivity.getTaskManager().runSenderTask(
                        host.getInetAddress(),
                        Message.newStateChangeRequestMessage(state)
                );
            }
        } catch (Exception e) {
            Log.d(MainActivity.LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }
}
