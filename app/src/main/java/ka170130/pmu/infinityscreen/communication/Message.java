package ka170130.pmu.infinityscreen.communication;

public class Message {

    public enum MessageType {
        HELLO
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
        content = new byte[input.length - 1];
        System.arraycopy(input, 1, content, 0, input.length - 1);
    }

    public static Message newHelloMessage(byte[] content) {
        return new Message(MessageType.HELLO, content);
    }

    public byte[] getBytes() {
        byte[] output = new byte[content.length + 1];
        output[0] = getCode();
        System.arraycopy(content, 0, output, 1, content.length);
        return output;

    }

    private byte getCode() {
        byte code = 0;
        while (code < MESSAGE_TYPES.length && !MESSAGE_TYPES[code].equals(messageType)) {
            code++;
        }
        return code;
    }
}
