package MIC1.CPU;

import ADT.Queue.Queue;
import Emulator.Emulator;
import MIC1.Components.*;
import Numbers.Binary32;
import Numbers.Binary8;

/*
  Versione modificata per IJVM della cpu del MIC1
*/
public class MIC1 implements Runnable {
    final String[] registers = {"MAR", "MDR", "PC", "MBR", "SP", "LV", "CPP", "TOS", "OPC", "H"};
    int stop = 0;
    //a chi dire che la computazione è completata
    Emulator toAdvise;
    //esegue un solo step, ovvero si ferma quando la mpccontrol contiene l'indirzzodi Main1
    boolean step = false;
    //all the components
    private ALU alu;
    private DataPath dataPath;
    private controlStore cStore;
    private MIR mir;
    private MPCControl mpcControl;
    private Memory32 mem32;
    //manage memory reading
    private Queue readed;
    private Queue fetched;
    private Queue toWrite;
    //last algorithm instruction
    private String lastInstruction;
    //first instruction address
    private String mpcStart;
    //emulator to be advised if computation was successfull.
    //if was requested an halt operation.
    private boolean halt;
    //if was requested a no-end loop.
    private boolean infinity = false;

    //il costruttore
    public MIC1() {
        dataPath = new DataPath();
        alu = new ALU(dataPath);
        cStore = new controlStore();
        mir = new MIR("000000000000000000000000000000000000");
        mem32 = new Memory32();
        mpcControl = new MPCControl(mir, alu, dataPath);
        readed = new Queue();
        fetched = new Queue();
        toWrite = new Queue();
        halt = false;
    }

    //gives back memory content in matrix form.
    public int[][] getMemoryContent() {
        return mem32.getMemoryContent();
    }

    //methods requested by runnable interface,it calls execute.
    public Memory32 getMemory() {
        return mem32;
    }

    public void run() {
        execute();
    }

    //imposta il controlStore
    public void setControlStore(controlStore _toSet) {
        cStore = _toSet;
    }

    //called from the emulator, if user inserted a possible looping code
    public void setInfinity(boolean inf) {
        infinity = inf;
    }

    //gives back MIR register
    public String getMir() {
        return mir.getInstruction();
    }

    //setta l'emulatore a cui dire che la computazione è finita
    public void setEmulator(Emulator toSet) {
        toAdvise = toSet;
    }

    //execute MIR instruction
    public void execute() {
        boolean ultima = false;
        int cont = 0;
        boolean forHalt = false;
        //this cycle has its stop only when the algorhitm was executed or halt variable has true value.
        while (true) {
            //CHARGE MIR WITH NEW INSTRUCTION
            mir = new MIR(cStore.getMicInstruction(mpcControl.getMPC()));
            //pick from Mir the instruction to execute.
            String now = mir.getInstruction();
            //charge the B bus with the desidered register, the last 4 bit
            String readFrom = now.substring(32);
            dataPath.readFrom(readFrom);
            //send to alu and shifter the instruction to execute
            String toAlu = now.substring(12, 20);
            alu.execute(toAlu);
            //ASCENT CLOCK FRONT, the instruction selected registers receives results from alu
            dataPath.assign(now.substring(20, 29));
            //MDR and MBR receivers its data, if there was a memory read.
            //memory manage, if queue of read data is not empty it pops an element.
            if (!readed.isEmpty()) {
                Binary32 data = (Binary32) readed.dequeue();
                if (data != null)
                    dataPath.setMDR(data);
            }
            if (!fetched.isEmpty()) {
                Binary8 data = (Binary8) fetched.dequeue();
                if (data != null)
                    dataPath.setMBR(data);
            }
            if (!toWrite.isEmpty()) {
                toWriteData toWriteN = (toWriteData) toWrite.dequeue();
                if (toWriteN != null)
                    mem32.wr(toWriteN.getAddress(), toWriteN.getData());
            }

            //BEGIN OF THE READ/WRITE MEMORY PROCESS.
            if (!mir.getInstruction().substring(29, 32).equals("000")) {
                //delayed wr operation
                if (mir.getInstruction().charAt(29) == '1') {
                    toWrite.enqueue(new toWriteData(new Binary32(dataPath.getMAR().getValue()),
                            new Binary32(dataPath.getMDR().getValue())));
                    if (new Binary32(dataPath.getMAR().getValue()).getDecimal() == -3)//TODO Forse non è sempre -3
                    {
                        System.out.print((char) new Binary32(dataPath.getMDR().getValue()).getDecimal());
                    }
                }
                //delayed rd operation
                if (mir.getInstruction().charAt(30) == '1')
                    readed.enqueue(mem32.rd(dataPath.getMAR()));

                //delayed fetch operation
                if (mir.getInstruction().charAt(31) == '1')
                    fetched.enqueue(mem32.fetch(dataPath.getPC()));
            }
            //NOW IT CAN CHARGE MPC WITH THE ADDRESS OF THE NEXT INSTRUCTION
            mpcControl = new MPCControl(mir, alu, dataPath);
            //check if was requested an halt operation or if it arrived to the last algorithm instruction.
            if (halt || forHalt) {
                halt = false;
                //emulator need to be noticed that compilation was successfull.
                toAdvise.complete();
                return;
            }
            cont++;
            forHalt = (stop == getRegister("PC") && mpcControl.getMPC().equals(mpcStart)) ||
                    (mpcControl.getMPC().equals(mpcStart) && step || step && cont > 512);
        }
    }

    //restituisce il byte dopo il quale la computazione si deve fermare
    public int getStop() {
        return stop;
    }

    public boolean getAluZ() {
        return alu.getZ();
    }

    public boolean getAluN() {
        return alu.getN();
    }

    public void writeRom(int _address, String _instruction) {
        cStore.writeMicInstruction(_address, _instruction);
    }

    public controlStore getRom() {
        return cStore;
    }

    public void setRom(controlStore _cStore) {
        cStore = _cStore;
    }

    //last execute instruction
    public void setLast(String _lastIntruction) {
        lastInstruction = _lastIntruction;
    }

    public void setStep(boolean toSet) {
        step = toSet;
    }

    //machine starts, it takes first instruction address from MAL, ed il byte a cui si deve fermare la computazione
    public void start(String _instrNumber, int _stop) {
        mpcStart = _instrNumber;
        lastInstruction = _instrNumber;
        mpcControl.setMPC(_instrNumber);
        stop = _stop;
    }

    //reset the machine
    public void reset(String _instrNumber, int _stop) {
        mir = new MIR("000000000000000000000000000000000000");
        start(_instrNumber, _stop);
        //svuoto le code di leturra/scrittura
        while (!toWrite.isEmpty())
            toWrite.dequeue();
        while (!readed.isEmpty())
            readed.dequeue();
        while (!fetched.isEmpty())
            fetched.dequeue();
        toAdvise.complete();
    }

    //if we want to set a particular register
    public void setRegister(String _nameR, int _value) {
        if (_nameR.equals("MAR"))
            dataPath.setMAR(_value);
        else if (_nameR.equals("MDR"))
            dataPath.setMDR(_value);
        else if (_nameR.equals("PC"))
            dataPath.setPC(_value);
        else if (_nameR.equals("SP"))
            dataPath.setSP(_value);
        else if (_nameR.equals("LV"))
            dataPath.setLV(_value);
        else if (_nameR.equals("CPP"))
            dataPath.setCPP(_value);
        else if (_nameR.equals("TOS"))
            dataPath.setTOS(_value);
        else if (_nameR.equals("OPC"))
            dataPath.setOPC(_value);
        else if (_nameR.equals("H"))
            dataPath.setH(_value);
        else if (_nameR.equals("MBR")) {
            if (_value >= -128 && _value <= 127)
                dataPath.setMBR(_value);
            else
                throw new IllegalArgumentException("Invalid MBR value");
        } else
            throw new IllegalArgumentException("Invalid register name");
    }

    public void setMBR(Binary8 value) {
        dataPath.setMBR(value);
    }

    //apply halt, execute ends!
    public void halt() {
        halt = true;
    }

    public String getMPCStart() {
        return mpcStart;
    }

    public String getMPC() {
        return mpcControl.getMPC();
    }

    public void setMPC(String mpcValue) {
        mpcStart = mpcValue;
    }

    public int getRegister(String _name) {
        int toReturn = 0;
        if (_name.equals("H"))
            toReturn = dataPath.getH().getDecimal();
        else if (_name.equals("OPC"))
            toReturn = dataPath.getOPC().getDecimal();
        else if (_name.equals("TOS"))
            toReturn = dataPath.getTOS().getDecimal();
        else if (_name.equals("CPP"))
            toReturn = dataPath.getCPP().getDecimal();
        else if (_name.equals("LV"))
            toReturn = dataPath.getLV().getDecimal();
        else if (_name.equals("SP"))
            toReturn = dataPath.getSP().getDecimal();
        else if (_name.equals("MBR"))
            toReturn = dataPath.getMBR().getDecimal();
        else if (_name.equals("PC"))
            toReturn = dataPath.getPC().getDecimal();
        else if (_name.equals("MDR"))
            toReturn = dataPath.getMDR().getDecimal();
        else if (_name.equals("MAR"))
            toReturn = dataPath.getMAR().getDecimal();
        return toReturn;
    }

    //write to a specifiyng  address
    public void writeMemory(int _address, int _value) {
        mem32.wr(new Binary32(_address), new Binary32(_value));
    }

    //execution subdivision in subcycle
    public String runSubCycle(int _subCycle) {
        String toReturn = "";
        switch (_subCycle) {
            case 1:
                mir = new MIR(cStore.getMicInstruction(mpcControl.getMPC()));
                break;
            case 2:
                //charge B bus with desidered register from the instruction, last 4 bit.
                String readFrom = mir.getInstruction().substring(32);
                dataPath.readFrom(readFrom);
                break;
            case 3:
                String toAlu = mir.getInstruction().substring(12, 20);
                alu.execute(toAlu);
                break;
            case 4:
                //wait that alu and shifter output they're stable
                break;
            case 5:
                dataPath.assign(mir.getInstruction().substring(20, 29));
                //MDR and MBR receives its data from the last datapath reading operation
                //memory manage, if queue of readed data is not empty it pops an element
                if (!readed.isEmpty()) {
                    Binary32 data = (Binary32) readed.dequeue();
                    dataPath.setMDR(data);
                    if (readed.isEmpty())
                        toReturn += "rd0";
                    else
                        toReturn += "rdNOT0";
                }
                if (!fetched.isEmpty()) {
                    Binary8 data = (Binary8) fetched.dequeue();
                    dataPath.setMBR(data);
                    if (fetched.isEmpty())
                        toReturn += "fetch0";
                    else
                        toReturn += "fetchNOT0";
                }
                if (!toWrite.isEmpty()) {
                    toWriteData toWriteN = (toWriteData) toWrite.dequeue();
                    mem32.wr(toWriteN.getAddress(), toWriteN.getData());
                    if (toWrite.isEmpty())
                        toReturn += "wr0";
                    else
                        toReturn += "wrNOT0";
                }
                //starts eventual memory reading
                if (!mir.getInstruction().substring(29, 32).equals("000")) {
                    //delayed wr operation
                    if (mir.getInstruction().charAt(29) == '1') {
                        toWrite.enqueue(new toWriteData(dataPath.getMAR(), dataPath.getMDR()));
                        toReturn += "wrNOW0";
                    }
                    //delayed rd operation
                    if (mir.getInstruction().charAt(30) == '1') {
                        readed.enqueue(mem32.rd(dataPath.getMAR()));
                        toReturn += "rdNOW0";
                    }
                    //delayed fetch operation
                    if (mir.getInstruction().charAt(31) == '1') {
                        fetched.enqueue(mem32.fetch(dataPath.getPC()));
                        toReturn += "fetchNOW0";
                    }
                }
                break;
            case 6:
                mpcControl = new MPCControl(mir, alu, dataPath);
        }
        return toReturn;
    }
}

//a support class for delayed memory writing
class toWriteData {
    private Binary32 address;
    private Binary32 data;

    public toWriteData(Binary32 _address, Binary32 _data) {
        address = _address;
        data = _data;
    }

    public Binary32 getAddress() {
        return address;
    }

    public Binary32 getData() {
        return data;
    }
}
	