package GUI;

import Assembler.Opcode;
import Emulator.Emulator;
import MIC1.Components.Memory32;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller
{
	private static final String[] KEYWORDS = new String[]{
			"BIPUSH", "DUP", "GOTO", "IADD", "IAND", "IFEQ", "IFLT", "IF_ICMPEQ", "IINC", "ILOAD", "INVOKEVIRTUAL", "IOR", "IRETURN", "ISTORE", "ISUB", "LDC_W", "NOP", "POP", "SWAP", "WIDE ILOAD", "WIDE ISTORE", "OUT", "HALT", "IN"
	};
	private String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private String COMMENT_PATTERN = "[/]{2}.*";
	private String LABEL_PATTERN = "(?m)^((?![/.]).)*:";
	private String DECLARATION_PATTERN = "(?m)^[^\\S\\n]*?[.].+?((?=[/:])|$)";
	private Pattern PATTERN = Pattern.compile(
			"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
					+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					+ "|(?<LABEL>" + LABEL_PATTERN + ")"
					+ "|(?<DECLARATION>" + DECLARATION_PATTERN + ")"
	);
	@FXML
	public Slider sleepSlider;
	@FXML
	public TextField sleepTxt;
	@FXML
	public Button slowRunBtn;
	@FXML
	public Button stopSlowRunBtn;
	@FXML
	private MyCodeArea programTxt;
	@FXML
	private Canvas stackCanvas;
	@FXML
	private Button startBtn;
	@FXML
	private Button translateBtn;
	@FXML
	private Button stopBtn;
	@FXML
	private Button stepBtn;
	@FXML
	private Button resetBtn;
	@FXML
	private TextArea stdinTxt;
	@FXML
	private TextArea assemblerOutputTxt;
	@FXML
	private TextArea methodAreaTxt;
	@FXML
	private TextArea constantAreaTxt;
	@FXML
	private TextArea stdoutTxt;
	@FXML
	private TextField isRunningTxt;
	@FXML
	private TextField TOSTxt;
	@FXML
	private TextField SPTxt;
	@FXML
	private TextField LVTxt;
	@FXML
	private TextField PCTxt;
	@FXML
	private RadioButton methodBinRadio;
	@FXML
	private RadioButton methodHexRadio;
	@FXML
	public Menu recentPrograms; //TODO da fare
	@FXML
	private MenuItem newProgramMenu;
	@FXML
	private MenuItem openProgramMenu;
	@FXML
	private MenuItem saveProgramMenu;
	@FXML
	private MenuItem exitMenu;
	@FXML
	private MenuItem microProgramMenu;
	@FXML
	private MenuItem opcodeListMenu;
	@FXML
	private MenuItem showMemoryViewMenu;
	@FXML
	private MenuItem aboutMenu;
	@FXML
	private RadioButton globalDecRadio;
	@FXML
	private RadioButton globalHexRadio;

	private int registerBuffer = 0;
	private Stack stack;
	private MemoryController memoryController;
	private Parent memoryRoot;
	private Stage memoryStage;

	private boolean hex = true;

	private StyleSpans<Collection<String>> computeHighlighting(String text)
	{
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder
				= new StyleSpansBuilder<>();
		while (matcher.find())
		{
			String styleClass =
					matcher.group("KEYWORD") != null ? "keyword" :
							matcher.group("COMMENT") != null ? "comment" :
									matcher.group("LABEL") != null ? "label" :
											matcher.group("DECLARATION") != null ? "declaration" :
											/*		matcher.group("SEMICOLON") != null ? "semicolon" :
															matcher.group("STRING") != null ? "string" :*/
													null; /* never happens */
			assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	@FXML
	public void initialize()
	{
		programTxt.setParagraphGraphicFactory(LineNumberFactory.get(programTxt));
		translateBtn.setDisable(true);
		programTxt.richChanges()
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
				.subscribe(change ->
				{
					programTxt.setStyleSpans(0, computeHighlighting(programTxt.getText()));
					if (programTxt.getText().isEmpty())
						translateBtn.setDisable(true);
					else
						translateBtn.setDisable(false);
				});
		programTxt.setId("programArea");

		programTxt.getEntries().addAll(Arrays.asList(KEYWORDS));//TODO da aggiornare in esecuzione
		programTxt.getEntries().addAll(Arrays.asList(".var", ".constant", ".main"));

		stack = new Stack();

		sleepSlider.valueProperty().addListener(ev ->
		{
			sleepTxt.setText(String.valueOf((int) sleepSlider.getValue()));
		});

		sleepTxt.textProperty().addListener(ev ->
		{
			try
			{
				sleepSlider.setValue(Integer.parseInt(sleepTxt.getText()));
			} catch (NumberFormatException e)
			{
				sleepSlider.setValue(0);
				sleepTxt.setText("0");
			}

		});

		memoryStage = new Stage();
		try
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("../memory.fxml"));
			memoryRoot = loader.load();
			memoryController = loader.getController();
			memoryStage.setTitle("Memory View");
			memoryStage.setScene(new Scene(memoryRoot));
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		Emulator myEm = new Emulator(this);
	}

	public void setMemoryTest(Memory32 memory)
	{
		memoryController.setMemory(memory);
	}

	public void setSlowRunBtnHandler(EventHandler<ActionEvent> _toSet)
	{
		slowRunBtn.setOnAction(_toSet);
	}

	public void setStopSlowRunBtnHandler(EventHandler<ActionEvent> _toSet)
	{
		stopSlowRunBtn.setOnAction(_toSet);
	}

	public void setStartHandler(EventHandler<ActionEvent> _toSet)
	{
		startBtn.setOnAction(_toSet);
	}

	public void setStopHandler(EventHandler<ActionEvent> _toSet)
	{
		stopBtn.setOnAction(_toSet);
	}

	public void setResetHandler(EventHandler<ActionEvent> _toSet)
	{
		resetBtn.setOnAction(_toSet);
	}

	public void setStepHandler(EventHandler<ActionEvent> _toSet)
	{
		stepBtn.setOnAction(_toSet);
	}

	public void setCompileHandler(EventHandler<ActionEvent> _toSet)
	{
		translateBtn.setOnAction(_toSet);
	}

	public void setHexBinaryHandler(EventHandler<ActionEvent> _toSet)
	{
		methodHexRadio.setOnAction(_toSet);
		methodBinRadio.setOnAction(_toSet);
	}

	public void setMenuHandler(EventHandler<ActionEvent> _toSet)
	{
		newProgramMenu.setOnAction(_toSet);
		openProgramMenu.setOnAction(_toSet);
		saveProgramMenu.setOnAction(_toSet);
		exitMenu.setOnAction(_toSet);
		microProgramMenu.setOnAction(_toSet);
		opcodeListMenu.setOnAction(_toSet);
		aboutMenu.setOnAction(_toSet);
	}

	//abilita/disabilita il pulsante start
	public void setStart(boolean toSet)
	{
		startBtn.setDisable(!toSet);
	}

	public void setStop(boolean toSet)
	{
		stopBtn.setDisable(!toSet);
	}

	public void setReset(boolean toSet)
	{
		resetBtn.setDisable(!toSet);
	}

	public void setStep(boolean toSet)
	{
		stepBtn.setDisable(!toSet);
	}

	public void setTranslate(boolean toSet)
	{
		translateBtn.setDisable(!toSet);
	}

	public void setProgram(String toSet)
	{
		programTxt.replaceText(toSet);
	}

	public void setStdout(String toSet)
	{
		stdoutTxt.setText(toSet);
	}

	public void setStdin(String toSet)
	{
		stdinTxt.setText(toSet);
	}

	public void clearStdin()
	{
		Platform.runLater(() ->
		{
			stdinTxt.clear();
		});
	}

	public void appendStdout(String toSet)
	{
		Platform.runLater(() ->
		{
			stdoutTxt.appendText(toSet);
		});
	}

	public void setOutput(String toOut)
	{
		assemblerOutputTxt.setText(toOut);
	}

	public void setHex(boolean toSet)
	{
		methodHexRadio.setSelected(toSet);
	}

	public void setBinary(boolean toSet)
	{
		methodBinRadio.setSelected(toSet);
	}

	public String getProgramString()
	{
		return programTxt.getText();
	}

	public String getSleep()
	{
		return sleepTxt.getText();
	}

	public String getStdoutString()
	{
		return stdoutTxt.getText();
	}

	public String getStdinString()
	{
		return stdinTxt.getText();
	}

	public void setMethodArea(String toSet)
	{
		methodAreaTxt.setText(toSet);
	}

	public void setConstantPool(String toSet)
	{
		constantAreaTxt.setText(toSet);
	}

	public void setSP(int toSet)
	{
		Platform.runLater(() ->
		{
			String string;
			if(hex)
				string = Integer.toHexString(toSet);
			else
				string = String.valueOf(toSet);
			SPTxt.setText(string);
		});
	}

	public void setLV(int toSet)
	{
		Platform.runLater(() ->
		{
			String string;
			if(hex)
				string = Integer.toHexString(toSet);
			else
				string = String.valueOf(toSet);
			LVTxt.setText(string);
		});
	}

	public void setPC(int toSet)
	{
		Platform.runLater(() ->
		{
			String string;
			if(hex)
				string = Integer.toHexString(toSet);
			else
				string = String.valueOf(toSet);
			PCTxt.setText(string);
		});
	}

	public void setRunning(boolean toSet)
	{
		Platform.runLater(() ->
		{
			isRunningTxt.setText(toSet ? "true" : "false");
		});
	}

	public void setTOS(int toSet)
	{
		Platform.runLater(() ->
		{
			String string;
			if(hex)
				string = Integer.toHexString(toSet);
			else
				string = String.valueOf(toSet);
			TOSTxt.setText(string);
		});
	}

	public void printRegisters(int _numRegistri)
	{
		Platform.runLater(() ->
		{
			registerBuffer = registerBuffer + _numRegistri;
			if (_numRegistri > 0)
				for (int i = 0; i < _numRegistri; i++)
					stack.insertRegister();
		});

	}

	public void deleteRegisters(int _numRegistri)
	{
		Platform.runLater(() ->
		{
			registerBuffer = registerBuffer - _numRegistri;
			for (int i = 0; i < _numRegistri; i++)
				stack.deleteRegister();
		});

	}

	public void stackError()
	{
		Platform.runLater(() ->
		{
			stack.error();
			stack.paintComponent(stackCanvas.getGraphicsContext2D(), hex);
		});

	}

	public void setStackValues(int[] toSet)
	{
		Platform.runLater(() ->
		{
			stackCanvas.getGraphicsContext2D().clearRect(0, 0, stackCanvas.getWidth(), stackCanvas.getHeight());
			stack.setStackValues(toSet);
		});

	}

	public void refreshStack()
	{
		Platform.runLater(() ->
		{
			stackCanvas.getGraphicsContext2D().clearRect(0, 0, stackCanvas.getWidth(), stackCanvas.getHeight());
			stack.paintComponent(stackCanvas.getGraphicsContext2D(), hex);
		});

	}

	public void setOpCodeList(Opcode[] opCodeList)
	{
		ArrayList<String> keywords = new ArrayList<>();
		for (Opcode o : opCodeList)
		{
			keywords.add(o.getOpcode());
		}
		KEYWORD_PATTERN = "\\b(" + String.join("|", keywords.toArray(new String[keywords.size()])) + ")\\b";
		PATTERN = Pattern.compile(
				"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
						+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
						+ "|(?<LABEL>" + LABEL_PATTERN + ")"
						+ "|(?<DECLARATION>" + DECLARATION_PATTERN + ")"
		);
	}

	public void getRecentProgram(ActionEvent actionEvent)
	{
		//TODO
	}

	public void setSetStdinBtn(EventHandler<KeyEvent> stdinHandler)
	{
		stdinTxt.setOnKeyPressed(stdinHandler);
	}

	public void showMemoryView(ActionEvent actionEvent)
	{
		if (!memoryStage.isShowing())
		{
			memoryStage.show();
		} else
		{
			memoryStage.requestFocus();
		}
	}

	public void onGlobalDecRadio(ActionEvent actionEvent)
	{
		globalHexRadio.setSelected(false);
		hex = false;
		refreshStack();

		SPTxt.setText(String.valueOf(Integer.parseInt(SPTxt.getText().trim(),16)));
		LVTxt.setText(String.valueOf(Integer.parseInt(LVTxt.getText().trim(),16)));
		PCTxt.setText(String.valueOf(Integer.parseInt(PCTxt.getText().trim(),16)));
		TOSTxt.setText(String.valueOf(Integer.parseInt(TOSTxt.getText().trim(),16)));
	}

	public void onGlobalHexRadio(ActionEvent actionEvent)
	{
		globalDecRadio.setSelected(false);
		hex = true;
		refreshStack();
		SPTxt.setText(Integer.toHexString(Integer.parseInt(SPTxt.getText().trim())));
		LVTxt.setText(Integer.toHexString(Integer.parseInt(LVTxt.getText().trim())));
		PCTxt.setText(Integer.toHexString(Integer.parseInt(PCTxt.getText().trim())));
		TOSTxt.setText(Integer.toHexString(Integer.parseInt(TOSTxt.getText().trim())));
	}
}
