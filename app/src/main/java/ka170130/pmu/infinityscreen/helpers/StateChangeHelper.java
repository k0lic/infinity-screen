package ka170130.pmu.infinityscreen.helpers;

import android.util.Log;

import java.net.InetAddress;

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

            if (isHost) {
                mainActivity.getTaskManager()
                        .sendToAllInGroup(Message.newStateChangeMessage(state), true);
//                mainActivity.getTaskManager().runBroadcastTask(
//                        Message.newStateChangeMessage(state)
//                );
            } else {
                InetAddress hostAddress =
                        connectionViewModel.getHostDevice().getValue().getInetAddress();
                mainActivity.getTaskManager().runSenderTask(
                        hostAddress,
                        Message.newStateChangeRequestMessage(state)
                );
            }
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }
}
