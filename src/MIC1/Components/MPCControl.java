package MIC1.Components;

import Numbers.Binary8;

/*
	Class that englobes all the circuit regardings MPC and manage branches.
*/
public class MPCControl {
    private String MPC;
    private MIR mir;

    public MPCControl(MIR _mir, ALU _alu, DataPath _dataPath) {
        mir = _mir;
        MPC = mir.getInstruction().substring(0, 9);
        String nowInstrucion = mir.getInstruction();
        char[] MPCArr = MPC.toCharArray();
        if (nowInstrucion.substring(9, 12).equals("000"))
            return;
        else {
            boolean N = _alu.getN();
            boolean Z = _alu.getZ();
            boolean m1 = MPC.charAt(0) == 1;
            boolean F = (Z && nowInstrucion.charAt(11) == '1') || (N && nowInstrucion.charAt(10) == '1') || m1;
            MPCArr[0] = F ? '1' : '0';
        }
        if (mir.getInstruction().charAt(9) == '1') {
            Binary8 MBR = _dataPath.getMBR();
            boolean[] MBRarr = MBR.getValue();
            MPCArr = MPC.toCharArray();
            for (int i = 1; i < 9; i++)
                MPCArr[i] = (MBRarr[i - 1] || (MPCArr[i] == '1') ? '1' : '0');
        }
        MPC = new String(MPCArr);
    }

    public String getMPC() {
        return MPC;
    }

    public void setMPC(String mpcAddr) {
        if (mpcAddr.length() != 9)
            throw new IllegalArgumentException("MPC puo' contenere solo 9 bit");
        MPC = mpcAddr;
    }
}