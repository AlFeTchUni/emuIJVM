package Assembler;

import java.io.Serializable;

public class Parametro implements Serializable
{
    //il tipo è 0 = diretto, 1=costante, 2=variabile, 3=etichetta
    private int tipo;
    //il numero di bit del parametro
    private int bytes;

    public Parametro(int _tipo, int _bytes)
    {
        tipo = _tipo;
        bytes = _bytes;
    }

    public int getTipo()
    {
        return tipo;
    }

    public int getBytes()
    {
        return bytes;
    }

    public String getTypeName()
    {
        String toReturn = "";
        switch (tipo)
        {
            case 0:
                toReturn = "Direct";
                break;
            case 1:
                toReturn = "Constant";
                break;
            case 2:
                toReturn = "Variable";
                break;
            case 3:
                toReturn = "Label";
        }
        return toReturn;
    }

    public String toString()
    {
        return String.format("Type: %-10s Bytes: %d", getTypeName(), bytes);
    }

}