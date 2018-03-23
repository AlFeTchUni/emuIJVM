package MIC1.Components;

/*
	MIC-1 controlStore, or the ROM where microprograms are written by MAL.
	It's a simple 512 String array.
	Methods writeMicInstruction, needs to Mal(i thougth it was not properly to make this memory read-only)
*/
public class controlStore
{
    private String[] data;

    public controlStore()
    {
        data = new String[512];
    }

    //gives back the _addr position instruction
    public String getMicInstruction(String _addr)
    {
        if (_addr.length() != 9)
            throw new IllegalArgumentException("L'indirizzo istruzione da estrarre deve essere di 9 bit");
        //calculate the address, or converts binary 9 bit value in an integer value.
        int address = 0;
        for (int i = 0; i < 9; i++)
            address = (_addr.charAt(i) == '1' ? 1 : 0) + 2 * address;
        //if address matches an instructions gives back it.
        if (data[address] != null)
            return data[address];
            //else gives back 0 position
        else
            return "000000000000000000000000000000000000";
    }

    //writes _instr to (integer) address
    public void writeMicInstruction(int addr, String _instr)
    {
        if (addr > 512 || addr < 0 || _instr.length() != 36)
            throw new IllegalArgumentException("Indirizzo e/o istruzione non validi");
        data[addr] = _instr;
    }

    public void newMemory()
    {
        data = new String[512];
    }

    public String[] getArray()
    {
        return data;
    }
}

			