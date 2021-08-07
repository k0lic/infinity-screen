package ka170130.pmu.infinityscreen.containers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Message {

    public enum MessageType {
        HELLO,
        PEER_INFO,
        REQUEST_INFO,
        HOST_ACK,
        REQUEST_TRANSFORM,
        TRANSFORM,
        TRANSFORM_LIST_UPDATE
    }

    private static final MessageType[] MESSAGE_TYPES = MessageType.values();

    private MessageType messageType;
    private byte[] content;

    public Message(MessageType messageType, byte[] content) {
        this.messageType = messageType;
        this.content = content;
    }

    public Message(byte[] input) {
        messageType = MESSAGE_TYPES[input[0]];
        if (input.length == 1) {
            content = null;
        } else {
            content = new byte[input.length - 1];
            System.arraycopy(input, 1, content, 0, input.length - 1);
        }
    }

//    public static Message newHelloMessage(byte[] content) {
//        return new Message(MessageType.HELLO, content);
//    }

    public static Message newHelloMessage(PeerInfo peerInfo) throws IOException {
        return createMessageFromSerializable(MessageType.HELLO, peerInfo);
    }

    public static Message newPeerInfoMessage(PeerInetAddressInfo peerInfo) throws IOException {
        return createMessageFromSerializable(MessageType.PEER_INFO, peerInfo);
    }

    public static Message newRequestInfoMessage(PeerInfo peerInfo) throws IOException {
        return createMessageFromSerializable(MessageType.REQUEST_INFO, peerInfo);
    }

    public static Message newHostAckMessage(PeerInfo peerInfo) throws IOException {
        return createMessageFromSerializable(MessageType.HOST_ACK, peerInfo);
    }

    public static Message newRequestTransformMessage() {
        return new Message(MessageType.REQUEST_TRANSFORM, null);
    }

    public static Message newTransformMessage(TransformInfo transformInfo) throws IOException {
        return createMessageFromSerializable(MessageType.TRANSFORM, transformInfo);
    }

    public static Message newHelloMessage(ArrayList<TransformInfo> transformList) throws IOException {
        return createMessageFromSerializable(MessageType.TRANSFORM_LIST_UPDATE, transformList);
    }

    private static Message createMessageFromSerializable(MessageType type, Serializable serializable) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(serializable);
        Message message = new Message(type, byteOut.toByteArray());

        objectOut.close();
        byteOut.close();

        return message;
    }

    public Object extractObject() throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);
        Object extracted = objectIn.readObject();

        byteIn.close();
        objectIn.close();

        return extracted;
    }

    public byte[] getBytes() {
        int length = 1 + (content == null ? 0 : content.length);
        byte[] output = new byte[length];
        output[0] = getCode();
        if (content != null) {
            System.arraycopy(content, 0, output, 1, content.length);
        }
        return output;

    }

    public MessageType getMessageType() {
        return messageType;
    }

    public byte[] getContent() {
        return content;
    }

    private byte getCode() {
        byte code = 0;
        while (code < MESSAGE_TYPES.length && !MESSAGE_TYPES[code].equals(messageType)) {
            code++;
        }
        return code;
    }
}
