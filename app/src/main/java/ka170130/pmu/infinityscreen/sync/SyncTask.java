package ka170130.pmu.infinityscreen.sync;

import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.containers.PeerInetAddressInfo;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.ConnectionViewModel;
import ka170130.pmu.infinityscreen.viewmodels.SyncViewModel;

public class SyncTask implements Runnable {

    private static final long[] SLEEP_PERIODS = {
            250,
            500,
            1000,
            2000
    };
    private static final double PADDING = 0.1;

    private MainActivity mainActivity;
    private SyncViewModel syncViewModel;
    private ConnectionViewModel connectionViewModel;

    private int counter;
    private int sleepCounter;

    public SyncTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        syncViewModel = new ViewModelProvider(mainActivity).get(SyncViewModel.class);
        connectionViewModel = new ViewModelProvider(mainActivity).get(ConnectionViewModel.class);

        counter = 0;
        sleepCounter = 0;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Adjust sleep period if messages are taking a longer time to be processed
                long sleepPeriod = getSleepPeriod();
                Long averageRoundTripTime = syncViewModel.getAverageRoundTripTime();
                if (averageRoundTripTime * 2 < sleepPeriod * (1 - PADDING)) {
                    decreaseSleep();
                } else if (averageRoundTripTime > sleepPeriod * (1 - PADDING)) {
                    increaseSleep();
                }

                // Sleep
                Thread.sleep(getSleepPeriod());

                ArrayList<SyncInfo> syncInfoList = syncViewModel.getSyncInfoList();
                // Skip if counter is out of range (includes case when list is empty)
                if (syncInfoList.size() <= counter) {
                    counter = 0;
                    continue;
                }

                SyncInfo syncInfo = syncInfoList.get(counter);

                Collection<PeerInetAddressInfo> groupList =
                        connectionViewModel.getGroupList().getValue();
                // Skip if invalid Group List value
                if (groupList == null) {
                    continue;
                }

                // Find InetAddress of device which selected Sync Info belongs to
                Iterator<PeerInetAddressInfo> iterator = groupList.iterator();
                InetAddress inetAddress = null;
                while (iterator.hasNext() && inetAddress == null) {
                    PeerInetAddressInfo next = iterator.next();
                    if (next.getDeviceAddress().equals(syncInfo.getDeviceAddress())) {
                        inetAddress = next.getInetAddress();
                    }
                }

                // Skip if InetAddress was not found
                if (inetAddress == null) {
                    counter = (counter + 1) % syncInfoList.size();
                    continue;
                }

                // Send CLOCK_REQUEST message in order to update SyncInfo with the CLOCK_RESPONSE message
                Message message = Message.newClockRequestMessage(System.currentTimeMillis());
                mainActivity.getTaskManager().runSenderTask(inetAddress, message);

                // Increment counter
                counter = (counter + 1) % syncInfoList.size();
            } catch (InterruptedException | IOException e) {
                LogHelper.error(e);
            }
        }
    }

    public void increaseSleep() {
        if (sleepCounter + 1 < SLEEP_PERIODS.length) {
            sleepCounter++;
        }
    }

    public void decreaseSleep() {
        if (sleepCounter - 1 >= 0) {
            sleepCounter--;
        }
    }

    public long getSleepPeriod() {
        return SLEEP_PERIODS[sleepCounter];
    }
}
