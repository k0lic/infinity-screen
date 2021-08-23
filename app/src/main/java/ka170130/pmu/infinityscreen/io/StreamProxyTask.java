package ka170130.pmu.infinityscreen.io;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ka170130.pmu.infinityscreen.MainActivity;
import ka170130.pmu.infinityscreen.containers.Message;
import ka170130.pmu.infinityscreen.helpers.LogHelper;
import ka170130.pmu.infinityscreen.viewmodels.MediaViewModel;

public class StreamProxyTask implements Runnable {

    private static final int REQUEST_MAX_SIZE = 1024;

    private Socket socket;
    private String localPath;
    private long fileSize;
    private String mimeType;
    private int position;

    public StreamProxyTask(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        LogHelper.log("Stream Proxy Task is running...");

        try {
            readHeader();
            sendContent();
        } catch (IOException | InvalidRequestException | InterruptedException e) {
            LogHelper.error(e);
        }

        try {
            socket.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private void readHeader() throws IOException, InvalidRequestException {
        // Read HTTP headers
        String headers = "";
        InputStream inputStream = socket.getInputStream();
        byte[] buf = new byte[REQUEST_MAX_SIZE];
        int len = 0;
//        while (len != -1) {
            len = inputStream.read(buf, 0, buf.length);
//        }
        headers = new String(buf, 0, len, StandardCharsets.UTF_8);

        LogHelper.log("Stream Proxy Task read headers: ");
        LogHelper.log(headers);

        // Get the important bits from the headers
        String[] headerLines = headers.split("\n");
        String urlLine = headerLines[0];
        if (!urlLine.startsWith("GET ")) {
            LogHelper.log("Only GET is supported");
            throw new InvalidRequestException();
        }
        urlLine = urlLine.substring(4);
        int charPos = urlLine.indexOf(' ');
        if (charPos != -1) {
            urlLine = urlLine.substring(1, charPos);
        }
        String[] args = urlLine.split("\\?");
        localPath = args[0];
        for (int i = 1; i < args.length; i++) {
            // args[i] = <argument_name>"="<argument_value>
            int equalsIndex = args[i].indexOf("=");
            String argName = args[i].substring(0, equalsIndex);
            String argValue = args[i].substring(equalsIndex + 1);
            LogHelper.log("arg name: " + argName + "; arg value: " + argValue);
            switch (argName) {
                case "filesize":
                    fileSize = Long.parseLong(argValue);
                    break;
                case "mimetype":
                    mimeType = argValue;
                    break;
            }
        }

        position = 0;
        // See if there's a "Range:" header
        for (int i = 0; i < headerLines.length; i++) {
            String headerLine = headerLines[i];
            if (headerLine.startsWith("Range: bytes=")) {
                headerLine = headerLine.substring(13);
                charPos = headerLine.indexOf('-');
                if (charPos>0) {
                    headerLine = headerLine.substring(0, charPos);
                }
                position = Integer.parseInt(headerLine);
                LogHelper.log("position = " + position);
            }
        }
    }

    private void sendContent() throws IOException, InterruptedException {
        // Create HTTP header
        String headers = "";
        if (position == 0) {
            headers += "HTTP/1.1 200 OK\r\n";
        } else {
            headers += "HTTP/1.1 206 Partial Content\r\n";
        }
//        headers += "HTTP/1.0 200 OK\r\n";
        headers += "Content-Type: " + mimeType + "\r\n";
        headers += "Accept-Ranges: bytes\r\n";
        headers += "Content-Length: " + fileSize  + "\r\n";
//        headers += "Connection: Keep-Alive\r\n";
        headers += "Connection: close\r\n";
        headers += "\r\n";

        LogHelper.log("Response headers: ");
        LogHelper.log(headers);

        // Begin with HTTP header
        int fc = 0;
        long cbToSend = fileSize - position;
        OutputStream output = null;
        byte[] buff = new byte[Message.MESSAGE_MAX_SIZE];
//        byte[] buff = new byte[64 * 1024];  // TODO: adjust size
//        output = new BufferedOutputStream(socket.getOutputStream(), Message.MESSAGE_MAX_SIZE);
        output = new BufferedOutputStream(socket.getOutputStream(), 32*1024);  // TODO: adjust size
        output.write(headers.getBytes());

        // Loop as long as there's stuff to send
        while (cbToSend > 0 && !socket.isClosed()) {
            // See if there's more to send
            File file = new File(localPath);
            fc++;
            int cbSentThisBatch = 0;
            if (file.exists()) {
                FileInputStream input = new FileInputStream(file);
                input.skip(position);
                int cbToSendThisBatch = input.available();
                while (cbToSendThisBatch > 0) {
                    int cbToRead = Math.min(cbToSendThisBatch, buff.length);
                    int cbRead = input.read(buff, 0, cbToRead);
                    if (cbRead == -1) {
                        break;
                    }
                    cbToSendThisBatch -= cbRead;
                    cbToSend -= cbRead;
                    output.write(buff, 0, cbRead);
                    output.flush();
                    position += cbRead;
                    cbSentThisBatch += cbRead;

                    LogHelper.log("Stream Proxy Task sent " + cbRead + " bytes");
                }
                input.close();
            }

            // If we did nothing this batch, block for some time
            if (cbSentThisBatch == 0) {
                LogHelper.log("Blocking until more data appears");
                Thread.sleep(500);
            }
        }

        // Cleanup
        output.close();
        socket.close();
    }

    private class InvalidRequestException extends Exception {
        // TODO: anything?
    }
}
