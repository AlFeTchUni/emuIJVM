package IJVM;

import ADT.ListDL.ListDL;
import Assembler.Constant;
import Emulator.Emulator;
import Emulator.SystemError;
import MAL.ErroreDiCompilazione;
import MAL.MAL;
import MIC1.CPU.MIC1;
import MIC1.Components.Memory32;
import MIC1.Components.controlStore;
import Numbers.Binary32;
import Numbers.Binary8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Machine {
    private MIC1 mic1;
    private int main1Index;
    private int mainLastByte;
    private String firstInstr;
    private ExecutorService runner;
    private Memory32 memory;
    private int lastByte;

    public Machine(Emulator theEmu) {
        mic1 = new MIC1();
        ListDL micInstr = new ListDL();
        //leggo dal file il microprogramm
        File theFile = new File("./IJVM/microprogram.mal");
        try (BufferedReader br = new BufferedReader(new FileReader(theFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                micInstr.insertTail(line);
            }
            String[] toTranslate = new String[micInstr.getSize()];
            micInstr.rewind();
            for (int i = 0; i < toTranslate.length; i++)
                toTranslate[i] = (String) micInstr.next();
            //traduco il microprogramma
            MAL theMal = new MAL();
            theMal.translate(toTranslate);
            mic1.setControlStore(theMal.writeRom());
            mic1.setEmulator(theEmu);
            main1Index = theMal.getMain1();
            firstInstr = theMal.getFirstInstruction();
        } catch (IOException e) {
            throw new SystemError("Microprogram's file not found: ./IJVM/microprogram.mal");
        } catch (ErroreDiCompilazione e) {
            throw new SystemError("The Microprogram has an error!\n" + e.getMessage() +
                    "\nThe file is ./IJVM/microprogram.mal");
        }
        //inizializzo i registri di mic1
        mic1.setRegister("CPP", 0);
        mic1.setRegister("PC", (int) Math.pow(2, 16) * 4);
        mic1.setRegister("LV", (int) Math.pow(2, 25));
        mic1.setRegister("SP", (int) Math.pow(2, 25));
        memory = mic1.getMemory();
    }

    public void print() {
        System.out.println(mic1.getRegister("CPP"));
        System.out.println(mic1.getRegister("PC"));
        System.out.println(mic1.getRegister("LV"));
        System.out.println(mic1.getRegister("MBR"));
    }

    //scrive il programma
    public void writeProgram(ListDL _toWrite) {
        _toWrite.rewind();
        for (int i = 0; i < _toWrite.getSize(); i++)
            memory.wr(new Binary32(i + (int) Math.pow(2, 16)), (Binary32) _toWrite.next());
        lastByte = (int) Math.pow(2, 16) + _toWrite.getSize();
        reset();
    }

    //scrive le costanti
    public void writeConstants(ListDL _toWrite) {
        _toWrite.rewind();
        for (int i = 0; i < _toWrite.getSize(); i++)
            memory.wr(new Binary32(i), ((Constant) _toWrite.next()).getValue());
    }

    public void setControlStore(controlStore _cStore) {
        mic1.setControlStore(_cStore);
    }

    //restituisce il valore di TOS
    public int getTOS() {
        return mic1.getRegister("TOS");
    }

    //restituisce il valore di SP
    public int getSP() {
        return mic1.getRegister("SP");
    }

    //restituisce il valore di LV
    public int getLV() {
        return mic1.getRegister("LV");
    }

    //restituisce il valore di PC
    public int getPC() {
        return mic1.getRegister("PC");
    }

    //restituisce un array con il contenuto di tutta la method area
    public String getMethodArea(boolean hex) {
        Binary32[][] content = memory.getMemoryBinary();
        String toReturn = "";
        for (int i = 0; i < content.length; i++) {
            if (content[i][0].getDecimal() >= (int) Math.pow(2, 25) - 1)
                break;
            //la versione binaria
            if (content[i][0].getDecimal() >= (int) Math.pow(2, 16) && !hex)
                toReturn += "0x" + Integer.toHexString(content[i][0].getDecimal() * 4) + "   " +
                        content[i][1].getByte(0) +
                        " " + content[i][1].getByte(1) +
                        " " + content[i][1].getByte(2) +
                        " " + content[i][1].getByte(3) +
                        "\n";
            //la versione esadecimale
            if (content[i][0].getDecimal() >= (int) Math.pow(2, 16) && hex) {
                toReturn += "0x" + Integer.toHexString(content[i][0].getDecimal() * 4) + "   ";
                for (int j = 0; j < 4; j++) {
                    String toAppend = Integer.toHexString(content[i][1].getByte(j).getDecimal());
                    if (toAppend.length() == 1)
                        toReturn += " 0x0" + toAppend;
                    else if (toAppend.length() == 2)
                        toReturn += " 0x" + toAppend;
                    else
                        toReturn += " 0x" + toAppend.substring(toAppend.length() - 2);
                }
                toReturn += "\n";
            }
        }
        return toReturn;
    }

    public String getConstantPool() {
        Binary32[][] content = mic1.getMemory().getMemoryBinary();
        String toReturn = "Addr |Content\n";
        for (int i = 0; i < content.length; i++) {
            if (content[i][0].getDecimal() < (int) Math.pow(2, 16)) {
                toReturn += "0x" + Integer.toHexString(content[i][0].getDecimal()) + "   "
                        + "Ox" + Integer.toHexString(content[i][1].getDecimal()) + "\n";
            } else
                break;
        }
        return toReturn;
    }

    public int getMainLastByte() {
        return mainLastByte;
    }

    public void setMainLastByte(int _mainLastByte) {
        mainLastByte = _mainLastByte;
    }

    //setta il tutto per l'avvio
    public void start() {
        mic1.start(firstInstr, mainLastByte);
        //la prima istruzione che sarà eseguita è un invokevirtual per il main
        boolean[] invokeB = {true, false, true, true, false, true, true, false};
        mic1.setMBR(new Binary8(invokeB));
    }

    public int[] getStackValues() {
        //controllo se lo stack presenta incongruenze
        if (mic1.getRegister("LV") >= (int) Math.pow(2, 25) && mic1.getRegister("SP") >= mic1.getRegister("LV"))
            return memory.getValuesFrom(mic1.getRegister("LV"), mic1.getRegister("SP"));
        else
            return new int[0];
    }


    //avvia la macchina, il mic1 è eseguito in un thread separato per evitare blocchi
    public void begin() {
        mic1.setStep(false);
        runner = Executors.newFixedThreadPool(1);
        runner.execute(mic1);
    }

    //arresta la macchina
    public void halt() {
        mic1.halt();
    }

    public boolean step() {
        mic1.setStep(true);
        runner = Executors.newFixedThreadPool(1);
        runner.execute(mic1);
        return mic1.getStop() == getPC() - 1;
    }

    public synchronized boolean stepNoThread() {
        mic1.setStep(true);
        mic1.execute();
        /*runner = Executors.newFixedThreadPool(1);
        runner.execute(mic1);*/
        return mic1.getStop() == getPC() - 1;
    }

    public void setStdin(int stdin) {
        mic1.getMemory().wr(new Binary32(-3), new Binary32(stdin));
    }

    //resetta la macchina
    public void reset() {
        mic1.setRegister("PC", ((int) Math.pow(2, 16)) * 4);
        mic1.setRegister("SP", ((int) Math.pow(2, 25)));
        mic1.setRegister("LV", ((int) Math.pow(2, 25)));
        mic1.setRegister("TOS", 0);
        boolean[] invokeB = {true, false, true, true, false, true, true, false};
        mic1.setMBR(new Binary8(invokeB));
        //effettua una pulizia della memoria
        memory.deleteFrom(lastByte);
        memory.wr(new Binary32(-3), new Binary32(0));
        mic1.reset(firstInstr, mainLastByte);
    }

    public void printMemory() {
        System.out.println(mic1.getMemory());
    }

    public Memory32 getMemory() {
        return mic1.getMemory();
    }

    public void setFirstInstrunction(String _instr) {
        firstInstr = _instr;
    }

}
