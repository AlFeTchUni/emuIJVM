package GUI;

import MIC1.Components.Memory32;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;

public class MemoryController {
    @FXML
    private RadioButton hexRadio;
    @FXML
    private RadioButton binRadio;
    @FXML
    private RadioButton decRadio;
    @FXML
    private TextField gotoAddressTxt;
    @FXML
    private Button goBtn;
    @FXML
    private TableView<MemoryViewRow> memoryTableView;

    private int numberBase = 16;

    private TableColumn<MemoryViewRow, String> wordAddress;
    private TableColumn<MemoryViewRow, String> content;
    private TableColumn<MemoryViewRow, String> pointers;
    private ArrayList<TableColumn<MemoryViewRow, String>> columns = new ArrayList<>();

    private Memory32 memory;

    @FXML
    public void initialize() {
        wordAddress = new TableColumn<>("Word Address");
        content = new TableColumn<>("Content");
        pointers = new TableColumn<>("Pointers");

        wordAddress.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
        content.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        columns.add(wordAddress);
        columns.add(content);
        columns.add(pointers);

        memoryTableView.getColumns().clear();
        memoryTableView.getColumns().addAll(wordAddress, content, pointers);

        setNumberFormatHandler();
    }

    private void setNumberFormatHandler() {
        hexRadio.setOnAction(event -> {
            binRadio.setSelected(false);
            decRadio.setSelected(false);
            numberBase = 16;
            setMemory(memory);
        });
        binRadio.setOnAction(event -> {
            hexRadio.setSelected(false);
            decRadio.setSelected(false);
            numberBase = 2;
            setMemory(memory);
        });
        decRadio.setOnAction(event -> {
            binRadio.setSelected(false);
            hexRadio.setSelected(false);
            numberBase = 10;
            setMemory(memory);
        });
    }

    public void setMemory(Memory32 memory) {
        this.memory = memory;
        ArrayList<MemoryViewRow> daVisualizzare = new ArrayList<>();
        for (int[] i : memory.getMemoryContent()) {
            MemoryViewRow memoryViewRow;
            if (numberBase == 16)
                memoryViewRow = new MemoryViewRow(Integer.toHexString(i[0]), Integer.toHexString(i[1]));
            else if (numberBase == 10)
                memoryViewRow = new MemoryViewRow(String.valueOf(i[0]), String.valueOf(i[1]));
            else if (numberBase == 2)
                memoryViewRow = new MemoryViewRow(Integer.toHexString(i[0]), Integer.toHexString(i[1]));//TODO da mettere in binario
            else
                memoryViewRow = new MemoryViewRow(Integer.toHexString(i[0]), Integer.toHexString(i[1]));
            daVisualizzare.add(memoryViewRow);
        }
        ObservableList<MemoryViewRow> daVisualizzareObs = FXCollections.observableArrayList(daVisualizzare);
        memoryTableView.setItems(daVisualizzareObs);
    }
}
