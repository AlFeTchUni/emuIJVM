package MIC1.Components;

import MIC1.Components.Shifter;
import Numbers.*;

/*
	The Pulsating heart of MIC-1!
	This ALU was made in order to emulate mic-1 alu, it recognizes commands as 6 bit Strings, it takes status operands (if needed) and sends the results
	to the shifter(with the 2 control bit).
	Obviously contains 2 boolean variable, that are N and Z, useful for eventual jumps.
	It's constructor takes a status object, the shifter is automatically controlled by the Alu, it have to pass to the alu 8 bits.
*/
public class ALU
{
    private DataPath dataPath;
    //constants that can be passed to the C bus of the status
    final Binary32 uno = new Binary32(1);
    final Binary32 zero = new Binary32(0);
    final Binary32 menoUno = new Binary32(-1);
    //variables that contains informations about the last made operation.
    private boolean Z;
    private boolean N;
    private Shifter shifter;

    public ALU(DataPath _dataPath)
    {
        dataPath = _dataPath;
        shifter = new Shifter(dataPath);
        Z = N = false;
    }

    //it read the bit code and execute the respective operation
    public void execute(String _code)
    {
        if (_code.length() != 8)
            throw new IllegalArgumentException("I bit che gestiscono l'alu e lo shifter devono essere 8");
        String toShifter = _code.substring(0, 2);
        _code = _code.substring(2);
        if (_code.equals("011000"))
            passaA(toShifter);
        else if (_code.equals("010100"))
            passaB(toShifter);
        else if (_code.equals("011010"))
            negaA(toShifter);
        else if (_code.equals("101100"))
            negaB(toShifter);
        else if (_code.equals("111100"))
            sommaAB(toShifter);
        else if (_code.equals("111101"))
            sommaABed1(toShifter);
        else if (_code.equals("111001"))
            incA(toShifter);
        else if (_code.equals("110101"))
            incB(toShifter);
        else if (_code.equals("111111"))
            BmenoA(toShifter);
        else if (_code.equals("110111"))
            deincB(toShifter);
        else if (_code.equals("111011"))
            menoA(toShifter);
        else if (_code.equals("001100"))
            andLogico(toShifter);
        else if (_code.equals("011100"))
            orLogico(toShifter);
        else if (_code.equals("010000"))
            zero(toShifter);
        else if (_code.equals("010001"))
            uno(toShifter);
        else if (_code.equals("010010"))
            menoUno(toShifter);
        else if (_code.equals("000000"))
            zero("00");
    }

    //All the functions of the mic-1's Alu

    //make it pass, without change, A bus to the shifter
    private void passaA(String _toShifter)
    {
        shifter.execute(_toShifter, dataPath.getBusA());
        N = dataPath.getBusA().getValue()[0];
        Z = dataPath.getBusA().equals(zero);
    }

    //make it pass, without change, B bus to the shifter
    private void passaB(String _toShifter)
    {
        shifter.execute(_toShifter, dataPath.getBusB());
        N = dataPath.getBusB().getValue()[0];
        Z = dataPath.getBusB().equals(zero);
    }

    //denies A, bit to bit
    private void negaA(String _toShifter)
    {
        boolean[] original = dataPath.getBusA().getValue();
        boolean[] negato = new boolean[32];
        for (int i = 0; i < 32; i++)
            negato[i] = !original[i];
        Binary32 risultato = new Binary32(negato);
        shifter.execute(_toShifter, risultato);
        N = negato[0];
        Z = risultato.equals(zero);
    }

    //denies B, bit to bit
    private void negaB(String _toShifter)
    {
        boolean[] original = dataPath.getBusB().getValue();
        boolean[] negato = new boolean[32];
        for (int i = 0; i < 32; i++)
            negato[i] = !original[i];
        Binary32 risultato = new Binary32(negato);
        shifter.execute(_toShifter, risultato);
        N = negato[0];
        Z = risultato.equals(zero);
    }

    //sums A and B bus
    private void sommaAB(String _toShifter)
    {
        Binary32 risultato = sum(dataPath.getBusA(), dataPath.getBusB());
        Z = risultato.equals(zero);
        N = risultato.getValue()[0];
        shifter.execute(_toShifter, risultato);
    }

    private void sommaABed1(String _toShifter)
    {
        Binary32 result = sum(dataPath.getBusA(), dataPath.getBusB());
        Binary32 resultPiuUno = sum(result, uno);
        shifter.execute(_toShifter, resultPiuUno);
        Z = resultPiuUno.equals(zero);
        N = resultPiuUno.getValue()[0];
    }

    private void incA(String _toShifter)
    {
        Binary32 result = sum(dataPath.getBusA(), uno);
        shifter.execute(_toShifter, result);
        Z = result.equals(zero);
        N = result.getValue()[0];

    }

    private void incB(String _toShifter)
    {
        Binary32 result = sum(dataPath.getBusB(), uno);
        shifter.execute(_toShifter, result);
        Z = result.equals(zero);
        N = result.getValue()[0];
    }

    private void BmenoA(String _toShifter)
    {
        boolean[] A = dataPath.getBusA().getValue();
        boolean[] C = new boolean[32];
        //nego a
        for (int i = 0; i < 32; i++)
            C[i] = !A[i];
        //sommo
        Binary32 result = sum(sum(new Binary32(C), dataPath.getBusB()), uno);
        shifter.execute(_toShifter, result);
        Z = result.equals(zero);
        N = result.getValue()[0];
    }

    private void deincB(String _toShifter)
    {
        Binary32 result = sum(dataPath.getBusB(), menoUno);
        shifter.execute(_toShifter, result);
        N = result.getValue()[0];
        Z = result.equals(zero);
    }

    private void menoA(String _toShifter)
    {
        boolean[] A = dataPath.getBusA().getValue();
        boolean[] C = new boolean[32];
        for (int i = 0; i < 32; i++)
            C[i] = !A[i];
        Binary32 result = sum(new Binary32(C), uno);
        shifter.execute(_toShifter, result);
        N = result.getValue()[0];
        Z = result.equals(zero);
    }

    private void andLogico(String _toShifter)
    {
        boolean[] A = dataPath.getBusA().getValue();
        boolean[] B = dataPath.getBusB().getValue();
        boolean[] C = new boolean[32];
        for (int i = 0; i < 32; i++)
            C[i] = A[i] && B[i];
        Binary32 result = new Binary32(C);
        shifter.execute(_toShifter, result);
        Z = result.equals(zero);
        N = result.getValue()[0];
    }

    private void orLogico(String _toShifter)
    {
        boolean[] A = dataPath.getBusA().getValue();
        boolean[] B = dataPath.getBusB().getValue();
        boolean[] C = new boolean[32];
        for (int i = 0; i < 32; i++)
            C[i] = A[i] || B[i];
        Binary32 result = new Binary32(C);
        shifter.execute(_toShifter, result);
        Z = result.equals(zero);
        N = result.getValue()[0];
    }

    public void uno(String _toShifter)
    {
        shifter.execute(_toShifter, uno);
        Z = N = false;
    }

    public void zero(String _toShifter)
    {
        shifter.execute(_toShifter, zero);
        Z = true;
        N = false;
    }

    public void menoUno(String _toShifter)
    {
        shifter.execute(_toShifter, menoUno);
        Z = false;
        N = true;
    }

    //sums two binary numbers
    private Binary32 sum(Binary32 one, Binary32 two)
    {
        boolean[] A = one.getValue();
        boolean[] B = two.getValue();
        boolean[] C = new boolean[32];
        boolean riporto = false;
        for (int i = 31; i >= 0; i--)
        {
            //sums the actual 3 bit
            int somma = (A[i] ? 1 : 0) + (B[i] ? 1 : 0) + (riporto ? 1 : 0);
            if (somma == 3)
            {
                //put 1 to actual position and i've to report 1
                C[i] = true;
                riporto = true;
            }
            if (somma == 2)
            {
                //put 0 to actual position and i've to report 1
                C[i] = false;
                riporto = true;
            }
            if (somma == 1)
            {
                //put 1 to actual position and i've to report 0
                C[i] = true;
                riporto = false;
            }
            if (somma == 0)
            {
                //put 0 to actual position and i've to report 0
                C[i] = false;
                riporto = false;
            }
        }
        //gives back the obtained number
        return new Binary32(C);
    }

    //gives back the N and Z bits, needed for conditional branches.
    public boolean getZ()
    {
        return Z;
    }

    public boolean getN()
    {
        return N;
    }

    //an easy test
    public static void main(String[] args)
    {
        DataPath myData = new DataPath();
        myData.setH(10);
        myData.setPC(15);
        myData.setTOS(-20);
        System.out.println("**************");
        ALU myAlu = new ALU(myData);
        myData.readFrom("0001");
        myAlu.execute("011000");//funzione A
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("010100");//funzione B
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("011010");//funzione negaA
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("101100");//funzione negaB
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("111100");//funzione A+B
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("111101");//funzione A+B+1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("111001");//funzione A+1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("110101");//funzione B+1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("*****B-A*********");
        myAlu.execute("111111");//funzione B-A
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("110111");//funzione B-1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("111011");//funzione -A
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("001100");//funzione A AND B
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("011100");//funzione A OR B
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("010000");//funzione 0
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("010001");//funzione 1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
        myAlu.execute("010010");//funzione -1
        myData.assign("000010000");
        myData.printDecimal();

        System.out.println("**************");
    }
}