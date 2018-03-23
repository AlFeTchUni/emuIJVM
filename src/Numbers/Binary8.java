package Numbers;

//8 bit numbers, they'll be the values that MBR can manage and moreover are the fetch gave back value.
public class Binary8 {
    private boolean[] value;

    public Binary8(boolean[] _value) {
        if (_value.length != 8)
            throw new IllegalArgumentException("I bit di questo tipo dati devono essere 8");
        value = _value;
    }

    public Binary8() {
        value = new boolean[8];
    }

    public Binary8(int _value) {
        value = new boolean[8];
        boolean negativo = false;
        int conv = _value;
        if (conv < 0) {
            value[0] = true;
            conv = 0 - conv;
            negativo = true;
        } else
            value[0] = false;
        for (int i = 0; i < 7; i++) {
            value[7 - i] = conv % 2 == 1;
            conv = conv / 2;
        }
        if (negativo) {
            for (int i = 0; i < 7; i++)
                value[7 - i] = !value[7 - i];
            int i = 7;
            while (i >= 0 && value[i])
                value[i--] = false;
            if (i != -1)
                value[i] = true;
        }
    }

    public static void main(String[] args) {
        Binary8 number = new Binary8((byte) -10);
        System.out.println(number);
    }

    public String toString() {
        String toReturn = "";
        for (int i = 0; i < value.length; i++) {
            if (value[i])
                toReturn += "1";
            else
                toReturn += "0";
        }
        return toReturn;
    }

    public boolean[] getValue() {
        return value;
    }

    public boolean equals(Binary8 _compare) {
        for (int i = 0; i < _compare.getValue().length; i++) {
            if (_compare.getValue()[i] != value[i])
                return false;
        }
        return true;
    }

    public int getDecimal() {
        //if number is negative
        if (value[0]) {
            //converts it in positive
            boolean[] conv = new boolean[8];
            for (int i = 0; i < 8; i++)
                conv[i] = !value[i];
            int c = 7;
            //sum a 1 (to-two complement)
            while (c >= 0 && conv[c])
                conv[c--] = false;
            if (c != -1)
                conv[c] = true;
            //the integer to give back
            int toReturn = 0;
            //decimal conversion
            for (int i = 0; i < 8; i++)
                if (conv[i])
                    toReturn = 1 + 2 * toReturn;
                else
                    toReturn = 0 + 2 * toReturn;
            //gives back the contrary
            return 0 - toReturn;
        }
        //it's positive
        int toReturn = 0;
        for (int i = 0; i < 8; i++)
            if (value[i])
                toReturn = 1 + 2 * toReturn;
            else
                toReturn = 0 + 2 * toReturn;
        return toReturn;
    }
}
		