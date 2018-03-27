package GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MemoryViewRow {
    private StringProperty address;
    private StringProperty content;

    public MemoryViewRow(String address, String content) {
        this.address = new SimpleStringProperty(address);
        this.content = new SimpleStringProperty(content);
    }

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public StringProperty addressProperty() {
        return address;
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public StringProperty contentProperty() {
        return content;
    }
}
