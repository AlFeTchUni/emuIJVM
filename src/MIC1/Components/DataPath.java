package MIC1.Components;

import Numbers.Binary32;
import Numbers.Binary8;

/*
	Mic-1 status, or the complements of registers and bus, that's why i called this class dataPath.
	Registers has the same mic-1 registers name, each register has a set function,that's non sense, but to permit initialization of a
	particular register when the program is not started, we can make this anomaly.
	The only register that as a get are MAR and PC
*/
public class DataPath {
    //registers
    private Binary32 MAR;
    private Binary32 MDR;
    private Binary32 PC;
    private Binary8 MBR;
    private Binary32 SP;
    private Binary32 LV;
    private Binary32 CPP;
    private Binary32 TOS;
    private Binary32 OPC;
    private Binary32 H;
    private Binary32 busB;
    private Binary32 busC;

    //constructor
    public DataPath() {
        MAR = new Binary32();
        MDR = new Binary32();
        PC = new Binary32();
        MBR = new Binary8();
        SP = new Binary32();
        LV = new Binary32();
        CPP = new Binary32();
        TOS = new Binary32();
        OPC = new Binary32();
        H = new Binary32();
        busB = new Binary32();
        busC = new Binary32();
    }

    //gives back A bus (or really H), makes request of this methods to the alu.
    public Binary32 getBusA() {
        return new Binary32(H.getValue());
    }

    //gives back B bus, yet initialized by readFrom.
    public Binary32 getBusB() {
        return busB;
    }

    //sets C bus(values sum by alu+shifter).
    public void setBusC(Binary32 _value) {
        busC = _value;
    }

    //assigns C bus to registers that has 1 in it's MIR bit.
    public void assign(String _code) {
        if (_code.length() != 9)
            throw new IllegalArgumentException("il codice assegnamento deve essere di 9 bit");
        for (int i = 0; i < 9; i++) {
            switch (i) {
                case 0:
                    if (_code.charAt(i) == '1')
                        H = new Binary32(busC.getValue());
                    break;
                case 1:
                    if (_code.charAt(i) == '1')
                        OPC = new Binary32(busC.getValue());
                    break;
                case 2:
                    if (_code.charAt(i) == '1')
                        TOS = new Binary32(busC.getValue());
                    break;
                case 3:
                    if (_code.charAt(i) == '1')
                        CPP = new Binary32(busC.getValue());
                    break;
                case 4:
                    if (_code.charAt(i) == '1')
                        LV = new Binary32(busC.getValue());
                    break;
                case 5:
                    if (_code.charAt(i) == '1')
                        SP = new Binary32(busC.getValue());
                    break;
                case 6:
                    if (_code.charAt(i) == '1')
                        PC = new Binary32(busC.getValue());
                    break;
                case 7:
                    if (_code.charAt(i) == '1')
                        MDR = new Binary32(busC.getValue());
                    break;
                case 8:
                    if (_code.charAt(i) == '1')
                        MAR = new Binary32(busC.getValue());
                    break;
            }
        }
    }

    //assigns a register to B bus, choosed with its value, implementation idea of the 4-to-16 decoder.
    public void readFrom(String _code) {
        if (_code.equals("0000"))
            busB = new Binary32(MDR.getValue());
        else if (_code.equals("0001"))
            busB = new Binary32(PC.getValue());
        else if (_code.equals("0010"))
            extendsMBR();
        else if (_code.equals("0011"))
            extendsMBRU();
        else if (_code.equals("0100"))
            busB = new Binary32(SP.getValue());
        else if (_code.equals("0101"))
            busB = new Binary32(LV.getValue());
        else if (_code.equals("0110"))
            busB = new Binary32(CPP.getValue());
        else if (_code.equals("0111"))
            busB = new Binary32(TOS.getValue());
        else if (_code.equals("1000"))
            busB = new Binary32(OPC.getValue());
    }

    //extends signed MBR in B bus.
    private void extendsMBR() {
        boolean[] mbr = MBR.getValue();
        boolean[] exMbr = new boolean[32];
        for (int i = 0; i < 8; i++)
            exMbr[31 - i] = mbr[7 - i];
        for (int i = 0; i < 24; i++)
            exMbr[i] = mbr[0];
        busB = new Binary32(exMbr);
    }

    //exteds no signed MBR in B bus
    private void extendsMBRU() {
        boolean[] mbr = MBR.getValue();
        boolean[] exMbr = new boolean[32];
        for (int i = 0; i < 8; i++)
            exMbr[31 - i] = mbr[7 - i];
        for (int i = 0; i < 24; i++)
            exMbr[i] = false;
        busB = new Binary32(exMbr);
    }

    //metodi get per i registri
    public Binary8 getMBR() {
        return MBR;
    }

    public void setMBR(Binary8 _value) {
        MBR = _value;
    }

    public void setMBR(int _value) {
        MBR = new Binary8(_value);
    }

    public Binary32 getPC() {
        return PC;
    }

    public void setPC(int _value) {
        PC = new Binary32(_value);
    }

    public Binary32 getMDR() {
        return MDR;
    }

    public void setMDR(int _value) {
        MDR = new Binary32(_value);
    }

    public void setMDR(Binary32 _value) {
        MDR = _value;
    }

    public Binary32 getMAR() {
        return MAR;
    }

    //set methods for all the registers
    public void setMAR(int _value) {
        MAR = new Binary32(_value);
    }

    public Binary32 getTOS() {
        return TOS;
    }

    public void setTOS(int _value) {
        TOS = new Binary32(_value);
    }

    public Binary32 getOPC() {
        return OPC;
    }

    public void setOPC(int _value) {
        OPC = new Binary32(_value);
    }

    public Binary32 getCPP() {
        return CPP;
    }

    public void setCPP(int _value) {
        CPP = new Binary32(_value);
    }

    public Binary32 getLV() {
        return LV;
    }

    public void setLV(int _value) {
        LV = new Binary32(_value);
    }

    public Binary32 getSP() {
        return SP;
    }

    public void setSP(int _value) {
        SP = new Binary32(_value);
    }

    public Binary32 getH() {
        return H;
    }

    public void setH(int _value) {
        H = new Binary32(_value);
    }

    //prints all the register contents in video.
    public void printDecimal() {
        System.out.println("MAR: " + MAR.getDecimal());
        System.out.println("MDR: " + MDR.getDecimal());
        System.out.println("PC: " + PC.getDecimal());
        System.out.println("MBR: " + MBR.getDecimal());
        System.out.println("SP: " + SP.getDecimal());
        System.out.println("LV: " + LV.getDecimal());
        System.out.println("CPP: " + CPP.getDecimal());
        System.out.println("TOS: " + TOS.getDecimal());
        System.out.println("OPC: " + OPC.getDecimal());
        System.out.println("H: " + H.getDecimal());
    }

    public void printBinary() {
        System.out.println("MAR: \t" + MAR);
        System.out.println("MDR: \t" + MDR);
        System.out.println("PC : \t" + PC);
        System.out.println("MBR: \t" + MBR);
        System.out.println("SP : \t" + SP);
        System.out.println("LV : \t" + LV);
        System.out.println("CPP: \t" + CPP);
        System.out.println("TOS: \t" + TOS);
        System.out.println("OPC: \t" + OPC);
        System.out.println("H  : \t" + H);
    }
}
