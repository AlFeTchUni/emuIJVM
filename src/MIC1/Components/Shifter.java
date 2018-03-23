package MIC1.Components;

import Numbers.Binary32;

/*
	Shifter component, only useful for the alu!
*/
public class Shifter {
    private DataPath dataPath;

    public Shifter(DataPath _dataPath) {
        dataPath = _dataPath;
    }

    public void execute(String _code, Binary32 toShift) {
        //8 positions left shift
        if (_code.equals("10")) {
            boolean[] newB = new boolean[32];
            boolean[] oldB = toShift.getValue();
            for (int i = 31; i > 23; i--)
                newB[i] = false;
            for (int i = 23; i >= 0; i--)
                newB[i] = oldB[i + 8];
            dataPath.setBusC(new Binary32(newB));
        }
        //1 position right shift
        else if (_code.equals("01")) {
            boolean[] newB = new boolean[32];
            boolean[] oldB = toShift.getValue();
            boolean oldVal = oldB[0];
            for (int i = 1; i < 32; i++)
                newB[i] = oldB[i - 1];
            newB[0] = oldVal;
            dataPath.setBusC(new Binary32(newB));
        } else
            dataPath.setBusC(toShift);
    }
}