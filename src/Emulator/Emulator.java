/*
  Questa classe, e tutte le sue classi innestate, rappresenta l'interfaccia tra la vera e propria macchina IJVM 
  la GUI
*/
package Emulator;

import Assembler.Assembler;
import Assembler.Opcode;
import Assembler.Parametro;
import Assembler.TranslationError;
import GUI.Controller;
import GUI.Dialogs.*;
import IJVM.Machine;
import MAL.ErroreDiCompilazione;
import MAL.MAL;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class Emulator {
    Controller theGUI;
    Machine theMachine;
    Assembler theAssembler;
    //true se la computazione è stata stoppata, false se invece è stata completata automaticamente
    boolean stopped = false;
    //il totale di elementi attualmente presenti sullo stack
    int totElementi = 0;
    //true se è stato premuto reset
    boolean reset;
    boolean step;
    Wait theWait = new Wait(null, false);
    Thread t;
    EventHandler<javafx.event.ActionEvent> compileHandler = ev -> {
        String programString = theGUI.getProgramString();
        //divido la strina in un array
        String[] toTranslate = programString.split("\n");
        try {
            theGUI.setOutput("Translating... please wait");
            theGUI.setStart(false);
            theGUI.setStop(false);
            theGUI.setStep(false);
            theGUI.setReset(false);
            theAssembler.translate(toTranslate);
            theMachine.writeProgram(theAssembler.getDecoded());
            theMachine.writeConstants(theAssembler.getConstants());
            theGUI.setMethodArea("Addr   | Content\n" + theMachine.getMethodArea(false));
            theGUI.setConstantPool(theMachine.getConstantPool());
            theGUI.setStdout("");
            theGUI.setOutput("Translation successfully completed!");
            theMachine.setMainLastByte(theAssembler.getMainLastByte());
            theMachine.reset();
            theGUI.setStart(true);
            theGUI.setStep(true);
            theGUI.setReset(false);
        } catch (TranslationError e) {
            theGUI.setOutput(e.getMessage());
        } catch (Exception e) {
            theGUI.setOutput("Unknown translation error\nThis is a bug, please notify!");
        }
    };
    EventHandler<javafx.event.ActionEvent> startHandler = ev -> {
        theGUI.setRunning(true);
        theGUI.setStart(false);
        theGUI.setStop(true);
        theGUI.setStep(false);
        theGUI.setReset(false);
        theMachine.begin();
    };
    EventHandler<javafx.event.ActionEvent> stopHandler = ev -> {
        stopped = true;
        theMachine.halt();
        theGUI.setStop(false);
    };
    EventHandler<javafx.event.ActionEvent> resetHandler = ev -> {
        int coefficienteStress = theMachine.getSP() - theMachine.getLV();
        if (!step && coefficienteStress > 750)
            theWait.setVisible(true);
        reset = true;
        theMachine.reset();
        theGUI.setStart(true);
        theGUI.setReset(false);
        theGUI.setStep(true);
        step = false;
        theGUI.setStdout("");
    };
    EventHandler<javafx.event.ActionEvent> hexBinHandler = ev -> {
        if (((RadioButton) ev.getSource()).getText().equals("Hex")) {
            theGUI.setBinary(false);
            theGUI.setHex(true);
            theGUI.setMethodArea("Addr    |Content\n" + theMachine.getMethodArea(true));
        } else {
            theGUI.setBinary(true);
            theGUI.setHex(false);
            theGUI.setMethodArea("Addr    |Content\n" + theMachine.getMethodArea(false));
        }

    };
    EventHandler<KeyEvent> stdinHandler = event -> {
        if (event.getText().length() > 0) {
            theMachine.setStdin((int) event.getText().charAt(0));
        }
        theGUI.clearStdin();
    };
    EventHandler<javafx.event.ActionEvent> stepHandler = ev -> {
        theMachine.step();
        theGUI.setStop(true);
        step = true;
        theGUI.setMemoryTest(theMachine.getMemory());
    };
    EventHandler<javafx.event.ActionEvent> slowRunHandler = ev -> {
        t = new Thread(() ->
        {
            while (true) {
                if (theMachine.step()) {
                    break;
                }
                theGUI.setStop(true);
                step = true;
                try {
                    Thread.sleep(Integer.parseInt(theGUI.getSleep()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    };
    EventHandler<javafx.event.ActionEvent> stopSlowRunHandler = ev -> {
        t.stop();
    };
    //l'icona del programma
    private javax.swing.ImageIcon icon = new javax.swing.ImageIcon("OtherStuff/icon.png");
    private MicroProgram microProgram;
    private OpcodeList opcodeList;
    EventHandler<javafx.event.ActionEvent> menuHandler = ev -> {
        MenuItem mItem = (MenuItem) ev.getSource();
        String name = mItem.getText();

        if (name.equals("MicroProgram")) {
            microProgram = new MicroProgram(new javax.swing.JFrame(), false);
            microProgram.setHandler(new microProgramHandler());
            //leggo dal file microprogram.mal il testo del micro programma
            try {
                microProgram.setMicroProgram(new String(Files.readAllBytes(Paths.get("./IJVM/microprogram.mal"))));
                microProgram.setSave(false);
                microProgram.setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "I/O Error",
                        "System Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (name.equals("Opcode List")) {
            opcodeList = new OpcodeList(new javax.swing.JFrame(), false);
            opcodeList.setList(theAssembler.getOpcodeList());
            opcodeList.setHandler(new opcodeListHandler(theAssembler.getOpcodeList()));
            opcodeList.setVisible(true);
        } else if (name.equals("Open Program")) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select JAS file");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JAS Files", "*.jas"));
            File selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                try {
                    theGUI.setProgram(new String(Files.readAllBytes(selectedFile.toPath())));
                    reset = true;
                    theMachine.reset();
                    complete();
                    theGUI.setMethodArea("");
                    theGUI.setConstantPool("");
                    theGUI.setOutput("");
                    theGUI.setStdout("");
                    theGUI.setStep(false);
                    theGUI.setStop(false);
                    theGUI.setStart(false);
                    theGUI.setReset(false);
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("System Error");
                    alert.setHeaderText("I/O Error:");
                    alert.setContentText(e.getMessage());

                    alert.showAndWait();
                }
            }

        } else if (name.equals("New Program")) {
            Optional<ButtonType> result = Optional.of(ButtonType.NO);
            if (!theGUI.getProgramString().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Warning");
                alert.setHeaderText("Warning");
                alert.setContentText("This action will erase your program.\nDo you want to proceed?");
                result = alert.showAndWait();
            }
            if (result.get() == ButtonType.OK || theGUI.getProgramString().isEmpty()) {
                reset = true;
                theMachine.reset();
                complete();
                theGUI.setMethodArea("");
                theGUI.setConstantPool("");
                theGUI.setOutput("");
                theGUI.setProgram("");
                theGUI.setStdout("");
                theGUI.setStep(false);
                theGUI.setStop(false);
                theGUI.setStart(false);
                theGUI.setReset(false);
            } else {
                // ... user chose CANCEL or closed the dialog
            }

        } else if (name.equals("Save Program")) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save file");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JAS Files", "*.jas"));
            File savedFile = fileChooser.showSaveDialog(null);

            if (savedFile != null) {
                File toSave = savedFile;
                try {
                    FileWriter output = new FileWriter(toSave);
                    output.write(theGUI.getProgramString());
                    output.close();
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("System Error ");
                    alert.setHeaderText("I/O Error:");
                    alert.setContentText(e.getMessage());

                    alert.showAndWait();
                }

            }

        } else if (name.equals("Exit"))
            System.exit(0);
        else if (name.equals("About")) {
            About theAbout = new About(new javax.swing.JFrame(), false);
            theAbout.setVisible(true);
        }
    };

    public Emulator(Controller _theGUI) {
        //avvio la macchina
        try {

            theMachine = new Machine(this);
            //inizializzo la GUI
            theGUI = _theGUI;
            //theGUI = Main.getController();
            //theGUI.setIconImage(icon.getImage());
            theAssembler = new Assembler();
        } catch (SystemError e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\nPlease fix this problem!",
                    "System Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        //setto tutto nella GUI
        theGUI.setPC(theMachine.getPC());
        theGUI.setLV(theMachine.getLV());
        theGUI.setSP(theMachine.getSP());
        theGUI.setTOS(theMachine.getTOS());
        theGUI.setRunning(false);
        theGUI.setCompileHandler(compileHandler);
        theGUI.setStartHandler(startHandler);
        theGUI.setStopHandler(stopHandler);
        theGUI.setResetHandler(resetHandler);
        theGUI.setHexBinaryHandler(hexBinHandler);
        theGUI.setStepHandler(stepHandler);
        theGUI.setSetStdinBtn(stdinHandler);
        theGUI.setMenuHandler(menuHandler);
        theGUI.setSlowRunBtnHandler(slowRunHandler);
        theGUI.setStopSlowRunBtnHandler(stopSlowRunHandler);
        theGUI.setStart(false);
        theGUI.setStop(false);
        theGUI.setStep(false);
        theGUI.setReset(false);
        theGUI.setBinary(true);
        theGUI.setOpCodeList(theAssembler.getOpcodeList());
    }

    public Controller getTheGUI() {
        return theGUI;
    }

    //chiamato dall'emumic quando la computazione è completa
    public void complete() {
        int coefficienteStress = theMachine.getSP() - theMachine.getLV();
        if (stopped && !step && coefficienteStress > 1000)
            theWait.setVisible(true);
        //aggiorno i registri che prelevo da emumic
        if (step)
            if (theMachine.getPC() - 1 == theMachine.getMainLastByte())
                theGUI.setStep(false);
        theGUI.setRunning(false);
        theGUI.setPC(theMachine.getPC());
        theGUI.setLV(theMachine.getLV());
        theGUI.setSP(theMachine.getSP());
        theGUI.setTOS(theMachine.getTOS());
        theGUI.setStart(stopped);
        theGUI.setReset(true);
        theGUI.setStop(false);
        refreshStack();

        //la computazione non è stata stoppata
        stopped = false;
        theWait.setVisible(false);
    }

    //gestisce lo stack
    public void refreshStack() {
        boolean error = false;
        //calcolo del totale di elementi presenti nello stack
        int nuovoTotale = theMachine.getSP() - (int) (Math.pow(2, 25));
        //controllo che non si sia verificato uno stack underflow
        if (theMachine.getSP() < theMachine.getLV()) {
            theGUI.stackError();
            return;
        }
        //incremento nuovoTotale poichè SP punta ad un'allocazione reale
        nuovoTotale++;
        //prelevo da IJVM i valori dello stack
        int[] values = theMachine.getStackValues();
        //se è stato premuto reset
        if (nuovoTotale == 1 && reset) {
            reset = false;
            values = new int[1];
        }
        //elimino dei registri nel caso in cui nuovoTotale<totElementi
        if (nuovoTotale < totElementi)
            theGUI.deleteRegisters(totElementi - nuovoTotale);
            //in caso contrario li aggiungo
        else
            theGUI.printRegisters(nuovoTotale - totElementi);
        //aggiorno i valori (potrebbe essere cambiato il Local Variable Frame)
        theGUI.setStackValues(values);
        //rinfresco lo stack (repaint)
        theGUI.refreshStack();
        //aggiorno totElementi
        totElementi = nuovoTotale >= 0 ? nuovoTotale : 0;
    }

    //classe per la gestione dei pulsanti del JDialog Microprogram
    private class microProgramHandler implements ActionListener {
        MAL mal;
        String program;

        public void actionPerformed(ActionEvent event) {
            //se l'evento riguarda il pulsante translate
            if (event.getActionCommand().equals("Translate")) {
                program = microProgram.getMicroProgram();
                String[] programArr = program.split("\n");
                mal = new MAL();
                try {
                    mal.translate(programArr);
                    microProgram.setSave(true);
                    microProgram.setOutput("Translation completed successfully!\n" +
                            "Now you can save the micro program!");
                } catch (ErroreDiCompilazione e) {
                    microProgram.setOutput(e.getMessage());
                    microProgram.setSave(false);
                }
            } else if (event.getActionCommand().equals("Save & Close")) {
                //aggiorno il control store
                theMachine.setControlStore(mal.writeRom());
                //salvo sul file il microprogramma
                try {
                    FileWriter output = new FileWriter("./IJVM/microprogram.mal");
                    output.write(program);
                    output.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "I/O Error",
                            "System Error", JOptionPane.ERROR_MESSAGE);
                }
                //resetto la macchina
                microProgram.dispose();
                reset = true;
                theMachine.setFirstInstrunction(mal.getFirstInstruction());
                theMachine.reset();
                complete();
                theGUI.setStep(false);
                theGUI.setStop(false);
                theGUI.setStart(false);
                theGUI.setReset(false);
            } else if (event.getActionCommand().equals("Restore Default")) {
                try {
                    FileInputStream inputF = new FileInputStream("./IJVM/original.mal");
                    FileOutputStream output = new FileOutputStream("./IJVM/microprogram.mal");
                    byte[] in = new byte[inputF.available()];
                    inputF.read(in);
                    output.write(in);
                    inputF.close();
                    output.close();
                    microProgram.setMicroProgram(new String(Files.readAllBytes(Paths.get("./IJVM/microprogram.mal"))));
                    program = microProgram.getMicroProgram();
                    String[] programArr = program.split("\n");
                    mal = new MAL();
                    try {
                        mal.translate(programArr);
                        microProgram.setSave(true);
                        JOptionPane.showMessageDialog(null, "Default microprogram restored!",
                                "System Error", JOptionPane.INFORMATION_MESSAGE);
                        microProgram.dispose();

                    } catch (ErroreDiCompilazione e) {
                        microProgram.setOutput(e.getMessage());
                        microProgram.setSave(false);
                    }

                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "I/O Error",
                            "System Error", JOptionPane.ERROR_MESSAGE);
                }

            }


        }
    }

    //gestisce i pulsanti di OpcodeList
    private class opcodeListHandler implements ActionListener {
        Opcode[] theOpcodes;
        OpcodeView theNew;

        public opcodeListHandler(Opcode[] _theOpcodes) {
            theOpcodes = _theOpcodes;
        }

        public void actionPerformed(ActionEvent event) {
            //se è stato premuto New
            if (event.getActionCommand().equals("New")) {
                theNew = new OpcodeView(new javax.swing.JFrame(), false);
                opcodeViewHandler handler = new opcodeViewHandler();
                handler.setModify(false);
                theNew.setList(new Parametro[0]);
                theNew.setLarge(false);
                theNew.setHandler(handler);
                theNew.setVisible(true);
            }
            //se è stato premuto edit
            else if (event.getActionCommand().equals("Edit")) {
                theNew = new OpcodeView(new javax.swing.JFrame(), false);
                opcodeViewHandler handler = new opcodeViewHandler();
                int selIndex = opcodeList.getSelectedIndex();
                handler.setModify(true);
                handler.setOpcodeIndex(selIndex);
                handler.setParametri(theOpcodes[selIndex].getParameters());
                theNew.setList(theOpcodes[selIndex].getParameters());
                theNew.setHandler(handler);
                theNew.setName(theOpcodes[selIndex].getOpcode());
                theNew.setValue1("0x" + Integer.toHexString(theOpcodes[selIndex].getValue()));
                theNew.setLarge(theOpcodes[selIndex].isLarge());
                if (theOpcodes[selIndex].isLarge())
                    theNew.setValue2("0x" + Integer.toHexString(theOpcodes[selIndex].getValue1()));
                theNew.setVisible(true);
            }
            //se è stato premuto Delete
            else if (event.getActionCommand().equals("Delete")) {
                int toDeleteIndex = opcodeList.getSelectedIndex();
                Opcode[] theOpcodesN = new Opcode[theOpcodes.length - 1];
                for (int i = 0, j = 0; i < theOpcodes.length; i++)
                    if (i != toDeleteIndex)
                        theOpcodesN[j++] = theOpcodes[i];
                theOpcodes = theOpcodesN;
                opcodeList.setList(theOpcodes);
            }
            //se è stato premuto Save & Close
            else if (event.getActionCommand().equals("Save & Close")) {
                theAssembler.setOpcodeList(theOpcodes);
                try {
                    theAssembler.saveOpcodeList();
                    opcodeList.dispose();
                } catch (SystemError e) {
                    JOptionPane.showMessageDialog(null, e.getMessage() + "\nPlease fix this problem!",
                            "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            //se è stato premuto restore default
            else if (event.getActionCommand().equals("Restore Default")) {
                //ripristino dal file original.lst
                try {
                    FileInputStream inputF = new FileInputStream("./Assembler/original.lst");
                    FileOutputStream output = new FileOutputStream("./Assembler/opcode.lst");
                    byte[] in = new byte[inputF.available()];
                    inputF.read(in);
                    output.write(in);
                    inputF.close();
                    output.close();
                    //ricostruisco l'assemblatore
                    theAssembler = new Assembler();
                    theOpcodes = theAssembler.getOpcodeList();
                    opcodeList.setList(theOpcodes);
                    JOptionPane.showMessageDialog(null, "Default OpcodeList restored!",
                            "Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage() + "\nPlease fix this problem!",
                            "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        //gestisce i pulsanti di OpcodeView
        private class opcodeViewHandler implements ActionListener {
            //la dialog parameter
            Parameter theNewP;
            //true se l'opcode presente nella finestra è da modificare, falso se è da inserire nuovo
            private boolean isToModify;
            //l'indice dell'opcode da modificare
            private int index;
            //i parametri dell'opcode
            private Parametro[] param = new Parametro[0];

            public void actionPerformed(ActionEvent event) {
                //se è stato premuto save
                if (event.getActionCommand().equals("Save & Close")) {
                    if (isToModify) {
                        Opcode nowOp = controllaOpcode();
                        if (nowOp != null) {
                            theOpcodes[index] = nowOp;
                            //aggiorno la lista
                            opcodeList.setList(theOpcodes);
                            theNew.dispose();
                        }
                    } else {
                        Opcode nowOp = controllaOpcode();
                        //aggiungo un nuovo Opcode
                        if (nowOp != null) {
                            Opcode[] theOpcodesN = new Opcode[theOpcodes.length + 1];
                            for (int i = 0; i < theOpcodes.length; i++)
                                theOpcodesN[i] = theOpcodes[i];
                            theOpcodes = theOpcodesN;
                            theOpcodes[theOpcodes.length - 1] = nowOp;
                            //aggiorno la lista
                            opcodeList.setList(theOpcodes);
                            theNew.dispose();
                        }
                    }
                }
                //se è stato premuto New
                else if (event.getActionCommand().equals("New")) {
                    theNewP = new Parameter(new javax.swing.JFrame(), false);
                    paramHandler handler = new paramHandler();
                    handler.setIndex(-1);
                    theNewP.select("Direct");
                    theNewP.setHandler(handler);
                    theNewP.setVisible(true);
                }
                //se è stato premuto Edit
                else if (event.getActionCommand().equals("Edit")) {
                    theNewP = new Parameter(new javax.swing.JFrame(), false);
                    paramHandler handler = new paramHandler();
                    handler.setIndex(theNew.getSelectedIndex());
                    theNewP.select(theOpcodes[index].getParametro(theNew.getSelectedIndex()).getTypeName());
                    theNewP.setBytes(
                            Integer.toString(
                                    theOpcodes[index].getParametro(theNew.getSelectedIndex()).getBytes())
                    );
                    theNewP.setHandler(handler);
                    theNewP.setVisible(true);
                }
                //se è stato premuto Delete
                else if (event.getActionCommand().equals("Delete")) {
                    int toDeleteIndex = theNew.getSelectedIndex();
                    Parametro[] paramN = new Parametro[param.length - 1];
                    for (int i = 0, j = 0; j < param.length; j++)
                        if (j != toDeleteIndex)
                            paramN[i++] = param[j];
                    param = paramN;
                    //aggiorno la lista
                    theNew.setList(param);
                }
            }

            public Opcode controllaOpcode() {
                String name = theNew.getName();
                String value1S = theNew.getValue1();
                String value2S = theNew.getValue2();
                int value1 = 0;
                int value2 = 0;
                //controllo il nome
                if (name.length() != 0) {
                    //controllo il nome
                    if (name.replaceAll("\\w", "").length() != 0) {
                        if (name.split(" ").length != 2) {
                            JOptionPane.showMessageDialog(null, "Opcode name not permitted " + name,
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }
                    //controllo l'unicità del nome

                    for (int i = 0; i < theOpcodes.length; i++) {
                        if (name.equals(theOpcodes[i].getOpcode())) {
                            if (isToModify)//se l'opcode è da modificare controllo l'indicie
                            {
                                if (i != index) {
                                    JOptionPane.showMessageDialog(null, "Opcode " + name + " already exist",
                                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                                    return null;
                                }
                            } else//altrimenti errore
                            {
                                JOptionPane.showMessageDialog(null, "Opcode " + name + " already exist",
                                        "Insert Error", JOptionPane.ERROR_MESSAGE);
                                return null;
                            }
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please insert an OPCODE name!",
                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                //controllo il primo valore
                if (value1S.length() != 0) {
                    try {
                        value1 = Integer.decode(value1S);
                        //controllo che non sia stato inserito un numero negativo
                        if (value1S.contains("-")) {
                            JOptionPane.showMessageDialog(null, "Negatives values are not permitted",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                        //controllo che il valore sia minore di 512
                        if (value1 > 512) {
                            JOptionPane.showMessageDialog(null, "Value1 are too large (Max 512)",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, value1S + " is NaN",
                                "Insert Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please insert Value 1",
                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                //controllo l'eventuale secondo valore
                if (theNew.isLarge()) {
                    try {
                        value2 = Integer.decode(value2S);
                        //controllo che non sia stato inserito un numero negativo
                        if (value2S.contains("-")) {
                            JOptionPane.showMessageDialog(null, "Negatives values are not permitted",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                        //controllo che il valore sia minore di 512
                        if (value2 > 512) {
                            JOptionPane.showMessageDialog(null, "Value2 are too large (Max 512)",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(null, value2S + " is NaN",
                                "Insert Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                }
                //ora posso controllare l'unicità del valore
                if (theNew.isLarge()) {
                    for (int i = 0; i < theOpcodes.length; i++) {
                        if (theOpcodes[i].isLarge()) {
                            if (theOpcodes[i].getValue() == value1 && theOpcodes[i].getValue1() == value2) {
                                if (isToModify && i != index) {
                                    JOptionPane.showMessageDialog(null, "Values: " + value1S + ", " + value2S +
                                                    " already assigned to: " + theOpcodes[i].getOpcode(),
                                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                                    return null;
                                } else if (!isToModify) {
                                    JOptionPane.showMessageDialog(null, "Values: " + value1S + ", " + value2S +
                                                    " already assigned to: " + theOpcodes[i].getOpcode(),
                                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                                    return null;
                                }
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < theOpcodes.length; i++) {
                        if (theOpcodes[i].getValue() == value1) {
                            if (isToModify) {
                                if (i != index) {
                                    JOptionPane.showMessageDialog(null, "Value: " + value1S +
                                                    " already assigned to: " + theOpcodes[i].getOpcode(),
                                            "Insert Error", JOptionPane.ERROR_MESSAGE);
                                    return null;
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "Value: " + value1S +
                                                " already assigned to: " + theOpcodes[i].getOpcode(),
                                        "Insert Error", JOptionPane.ERROR_MESSAGE);
                                return null;
                            }
                        }
                    }
                }
                Opcode toReturn;
                if (theNew.isLarge()) {
                    //costruisco l'opcode da restituire
                    toReturn = new Opcode(name, value1, value2, param.length);
                    for (int i = 0; i < param.length; i++)
                        toReturn.setParametro(i, param[i]);
                } else {
                    toReturn = new Opcode(name, value1, param.length);
                    for (int i = 0; i < param.length; i++)
                        toReturn.setParametro(i, param[i]);
                }
                return toReturn;
            }

            public void setModify(boolean toSet) {
                isToModify = toSet;
            }

            public void setOpcodeIndex(int toSet) {
                index = toSet;
            }

            public void setParametri(Parametro[] toSet) {
                param = toSet;
            }

            //gestisce i pulsanti di Parameter
            private class paramHandler implements ActionListener {
                //il numero di bytes
                int bytes;
                //l'indice del parametro
                private int paramIndex;

                public void actionPerformed(ActionEvent event) {
                    if (event.getActionCommand().equals("OK")) {
                        //controlla il numero di byte
                        if (theNewP.getBytes().length() != 0) {
                            try {
                                bytes = Integer.parseInt(theNewP.getBytes());
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(null, theNewP.getBytes() + " is NaN",
                                        "Insert Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Please insert the bytes number!",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        //un parametro più lungo di 4 bytes non è permesso
                        if (bytes > 4) {
                            JOptionPane.showMessageDialog(null, theNewP.getBytes() +
                                            "  is too large for a parameter (MAX 4)",
                                    "Insert Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        int tipoPar = theNewP.getSelected();
                        if (paramIndex == -1) {
                            Parametro[] paramN = new Parametro[param.length + 1];
                            for (int i = 0; i < param.length; i++)
                                paramN[i] = param[i];
                            param = paramN;
                            param[param.length - 1] = new Parametro(tipoPar, bytes);
                        } else {
                            param[paramIndex] = new Parametro(tipoPar, bytes);
                        }
                        //aggiorno la lista
                        theNew.setList(param);
                        theNewP.dispose();
                    }
                }

                public void setIndex(int toSet) {
                    paramIndex = toSet;
                }
            }
        }
    }//fine gestore menu
}
