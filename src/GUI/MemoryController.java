package GUI;

import MIC1.Components.Memory32;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;

public class MemoryController
{
	@FXML
	private TableView<MemoryViewRow> memoryTableView;

	private TableColumn<MemoryViewRow, String> wordAddress;
	private TableColumn<MemoryViewRow, String> content;
	private TableColumn<MemoryViewRow, String> pointers;
	private ArrayList<TableColumn<MemoryViewRow, String>> columns = new ArrayList<>();
	private Memory32 memory;

	@FXML
	public void initialize()
	{
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
	}

	public void setMemory(Memory32 memory)
	{
		this.memory = memory;
		ArrayList<MemoryViewRow> daVisualizzare = new ArrayList<>();
		for (int[] i : memory.getMemoryContent())
		{
			MemoryViewRow memoryViewRow = new MemoryViewRow(Integer.toHexString(i[0]),Integer.toHexString(i[1]));
			daVisualizzare.add(memoryViewRow);
		}
		ObservableList<MemoryViewRow> daVisualizzareObs = FXCollections.observableArrayList(daVisualizzare);
		memoryTableView.setItems(daVisualizzareObs);
	}
}
