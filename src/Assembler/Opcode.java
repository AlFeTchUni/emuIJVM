package Assembler;

import java.io.Serializable;

public class Opcode implements Serializable {
    private String opcode;
    private int value1;
    private int value2;
    private Parametro[] parameters;
    private boolean isLarge;

    //il costruttore nel caso di OPCODE normale
    public Opcode(String _opcode, int _value, int _parametersNumber) {
        opcode = _opcode;
        value1 = _value;
        parameters = new Parametro[_parametersNumber];
        isLarge = false;
    }

    //il costruttore nel caso di OPCODE esteso (ad es: WIDE ILOAD)
    public Opcode(String _opcode, int _value1, int _value2, int _parametersNumber) {
        opcode = _opcode;
        value1 = _value1;
        value2 = _value2;
        isLarge = true;
        parameters = new Parametro[_parametersNumber];
    }


    public String getOpcode() {
        return opcode;
    }

    public int getValue() {
        return value1;
    }

    public int getValue1() {
        return value2;
    }

    public boolean isLarge() {
        return isLarge;
    }

    public int getParametersNumber() {
        return parameters.length;
    }

    public Parametro getParametro(int num) {
        if (num >= parameters.length || num < 0)
            return null;
        return parameters[num];
    }

    public Parametro[] getParameters() {
        return parameters;
    }

    public boolean setParametro(int num, Parametro toInsert) {
        if (num >= parameters.length || num < 0)
            return false;
        parameters[num] = toInsert;
        return true;
    }

    public String toString() {
        return String.format("Name: %-20s  Type: %-7s \t Parameters: %d ", opcode, isLarge ? "Large" : "Normal",
                getParametersNumber());
    }

    public boolean equals(Opcode toCompare) {
        return opcode.equals(toCompare.getOpcode());
    }


}