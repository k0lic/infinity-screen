package ka170130.pmu.infinityscreen.communication;

import android.net.wifi.p2p.WifiP2pInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SenderTask implements Runnable {

    private WifiP2pInfo info;
    private Message message;

    public SenderTask(WifiP2pInfo info, Message message) {
        this.info = info;
        this.message = message;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        OutputStream outputStream = null;

        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(info.groupOwnerAddress, 8888)), 500);

            outputStream = socket.getOutputStream();

            outputStream.write(message.getBytes());
        } catch (Exception e) {
            // catch logic
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // catch logic
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // catch logic
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
