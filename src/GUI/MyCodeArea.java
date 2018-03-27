package GUI;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;

import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyCodeArea extends CodeArea {
    /**
     * The existing autocomplete entries.
     */
    private final SortedSet<String> entries;
    /**
     * The popup used to select an entry.
     */
    private final Pattern lastWordPattern = Pattern.compile("([^ \\t\\n][a-zA-Z0-9_:]+)$");
    private final Pattern firstWordPattern = Pattern.compile("([^ \t]\\w+)");
    private LinkedList<String> searchResult = new LinkedList<>();

    public MyCodeArea() {
        super();
        entries = new TreeSet<>();
        final Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox suggestionBox = new VBox();
        suggestionBox.setStyle("-fx-background-colorr: white;");
        popup.getContent().add(suggestionBox);
        setEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                int paragraph = getCurrentParagraph();
                if (event.getCode() == KeyCode.ENTER)
                    paragraph--;
                Matcher m = lastWordPattern.matcher(getText(paragraph));
                String lastWord = "";
                if (m.find())
                    lastWord = m.group();
                m = firstWordPattern.matcher(getText(paragraph));
                String first = "";
                if (m.find())
                    first = m.group();
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {

                    if (event.getCode() == KeyCode.ENTER) {
                        if (searchResult.size() > 0 && searchResult.getFirst().length() != lastWord.length()) {
                            insertText(getCaretPosition() - 1, searchResult.getFirst().substring(lastWord.length()));
                            deleteNextChar();
                            popup.hide();
                            return;
                        }

                        if (!first.startsWith(".") || first.startsWith(".end"))
                            return;
                        //insertText(getCaretPosition(), "\t");
                        int pos = getCaretPosition();
                        insertText(pos, "\n.end-" + first.replace(".", ""));
                        moveTo(pos);
                    }

                    if (event.getCode() == KeyCode.SPACE) {
                        popup.hide();
                        searchResult.clear();
                    }

					/*
					if (!entries.contains(lastWord))
						entries.add(lastWord.replaceAll("[^0-9a-zA-Z]+", ""));
						*/

                }


            }
        });
        textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
                Matcher m = lastWordPattern.matcher(getText(getCurrentParagraph(), 0, getCurrentParagraph(), getCaretColumn()));
                String lastWord = "";
                if (m.find())
                    lastWord = m.group();
                searchResult.clear();
                suggestionBox.getChildren().clear();
                if (lastWord.length() > 1)
                    searchResult.addAll(entries.subSet(lastWord, lastWord + Character.MAX_VALUE));
                searchResult.forEach(x -> suggestionBox.getChildren().add(new Label(x)));
                if (lastWord.length() > 1) {
                    Bounds bounds = getCaretBounds().orElse(null);
                    if (bounds != null)
                        popup.show(MyCodeArea.this, bounds.getMaxX() + 20, bounds.getMinY());
                } else {
                    popup.hide();
                }

            }
        });


        focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                //	popup.hide();
            }
        });

    }

    /**
     * Get the existing set of autocomplete entries.
     *
     * @return The existing autocomplete entries.
     */
    public SortedSet<String> getEntries() {
        return entries;
    }


}
