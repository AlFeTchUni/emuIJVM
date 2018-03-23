package MIC1.Components;

/*
	MIR register, it always contains the current instruction
*/
public class MIR {
    private String instruction;

    public MIR(String _instruction) {
        if (_instruction.length() != 36)
            throw new IllegalArgumentException("Lunghezza istruzione non valido");
        instruction = _instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}