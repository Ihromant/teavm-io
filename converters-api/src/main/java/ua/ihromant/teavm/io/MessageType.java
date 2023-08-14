package ua.ihromant.teavm.io;

public interface MessageType {
    Class<? extends Message> getCls();
}
