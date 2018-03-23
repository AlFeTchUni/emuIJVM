package Numbers;

/*
	The 32bit binary number, base of all the Mic-1 architecture.
	This class provides comparation of its objects, has a constructor family to be easier to use.
*/
public class Binary32 implements Comparable<Binary32>
{
    //The real and proper number is represented by a boolean array, each element of the array represents 1 bit.
    private boolean[] value;

    //with-array constructor
    public Binary32(boolean[] _value)
    {
        //se la lughezza dell'array non e di 32 elementi
        if (_value.length != 32)
            throw new IllegalArgumentException("I bit di questo tipo di dati devono essere 32");
        else
            value = _value;
    }

    //no-args constructor
    public Binary32()
    {
        value = new boolean[32];
    }

    //with-integer constructor, execute a conversion in binary number.
    public Binary32(int _value)
    {
        value = new boolean[32];
        boolean negativo = false;
        //first, if it's negative, converts it into positive
        if (_value < 0)
        {
            //assigns at the first bit true value.
            value[0] = true;
            //converts the arguments in positive.
            _value = 0 - _value;
            negativo = true;
        } else
            value[0] = false;
        //converts the number in binary.
        for (int i = 0; i < 31; i++)
        {
            value[31 - i] = _value % 2 == 1;
            _value = _value / 2;
        }
        //if argument is negative, makes a to-two complement conversion.
        if (negativo)
        {
            for (int i = 0; i < 31; i++)
                value[31 - i] = !value[31 - i];
            int i = 31;
            while (i >= 0 && value[i])
                value[i--] = false;
            if (i != -1)
                value[i] = true;
        }
    }

    //gives back the boolean array
    public boolean[] getValue()
    {
        return value;
    }

    //the super-normal equals!
    public boolean equals(Binary32 _compare)
    {
        for (int i = 0; i < 32; i++)
            if (_compare.getValue()[i] != value[i])
                return false;
        return true;
    }

    //gives back the byte starting from _num position.
    public Binary8 getByte(int _num)
    {
        if (_num > 23 || _num < 0)
            throw new IllegalArgumentException("Byte inesistente");
        //builds the array to give back.
        boolean[] myByte = new boolean[8];
        int c = 0;
        //fills the array.
        for (int i = _num * 8; i < _num * 8 + 8; i++)
            myByte[c++] = value[i];
        //gives back.
        return new Binary8(myByte);
    }

    //gibes back the decimal number.
    public int getDecimal()
    {
        //if number is negative
        if (value[0])
        {
            //convert number in positive
            boolean[] conv = new boolean[32];
            for (int i = 0; i < 32; i++)
                conv[i] = !value[i];
            int c = 31;
            //sum a 1 (read to-two complements specifics)
            while (c >= 0 && conv[c])
                conv[c--] = false;
            if (c != -1)
                conv[c] = true;
            //the integer to give back
            int toReturn = 0;
            //decimal conversion
            for (int i = 0; i < 32; i++)
                if (conv[i])
                    toReturn = 1 + 2 * toReturn;
                else
                    toReturn = 0 + 2 * toReturn;
            //gives back the contrary
            return 0 - toReturn;
        }
        //it's positive
        int toReturn = 0;
        for (int i = 0; i < 32; i++)
            if (value[i])
                toReturn = 1 + 2 * toReturn;
            else
                toReturn = 0 + 2 * toReturn;
        return toReturn;
    }

    //the super-normal compareTo
    public int compareTo(Binary32 _toCompare)
    {
        int toCompareDecimal = _toCompare.getDecimal();
        int thisDecimal = this.getDecimal();
        if (thisDecimal > toCompareDecimal)
            return 1;
        if (thisDecimal < toCompareDecimal)
            return -1;
        return 0;
    }

    //compareTo unsigned, useful for memory
    public int compareToUnsigned(Binary32 _toCompare)
    {
        long thisValue = 0;
        long compareValue = 0;
        //extracts the unsigned value of this objectestraggo il valore senza segno di questo oggetto
        for (int i = 0; i < 32; i++)
            if (value[i])
                thisValue = 1 + 2 * thisValue;
            else
                thisValue = 0 + 2 * thisValue;
        //extract the unsigned value of the toCompare object.
        for (int i = 0; i < 32; i++)
            if (_toCompare.getValue()[i])
                compareValue = 1 + 2 * compareValue;
            else
                compareValue = 0 + 2 * compareValue;
        //gives back the appropriate value
        if (thisValue > compareValue)
            return 1;
        if (thisValue < compareValue)
            return -1;
        return 0;
    }

    //gives back the bit string that represents the number.
    public String toString()
    {
        String toReturn = "";
        for (int i = 0; i < value.length; i++)
        {
            if (value[i])
                toReturn += "1";
            else
                toReturn += "0";
        }
        return toReturn;
    }

    //a simple test.
    public static void main(String[] args)
    {
        Binary32 number = new Binary32(0xFF);
        System.out.println(number);
    }

}

			