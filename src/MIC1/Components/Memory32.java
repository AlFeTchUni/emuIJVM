package MIC1.Components;

import Numbers.*;
import ADT.ListLS.*;
import ADT.ListDL.*;

/*
  Memory32 class represents the MIC-1 hardware memory.
  Is based on a simple linked list, in order to spend less memory and work quite good!
  Wr, rd and fetch methods are implemented with the help of MIC-1 memory.
  The list contains MemoryCell objects, defined by an address and a value.
*/
public class Memory32
{
    private ListLS data;

    //no arguments constructor
    public Memory32()
    {
        data = new ListLS();
    }

    //writes a value in a memory cell.
    public void wr(Binary32 _addr, Binary32 _value)
    {
        MemoryCell cella = new MemoryCell(_addr, _value);
        data.rewind();
        //if list is empty insert in the head of the list
        if (data.isEmpty())
            data.insertHead(cella);
        else
        {
            //if memory cell is littler than the head inserts the cell in list head.
            if (((MemoryCell) data.getHead()).compareTo(cella) == 1)
            {
                data.insertHead(cella);
                return;
            }
            //go on as long as it doesn't find a cell with bigger address.
            while (data.hasNext() && ((MemoryCell) data.next()).compareTo(cella) == -1)
                data.saveNow();
            //if cell in which cycle has become to stop is the
            //same(in order to the address)to the toinsert cell it substitutes the cell.
            if (((MemoryCell) data.getNow()).equals(cella))
            {
                data.setNow(cella);
                return;
            }
            //else it inserts in the immediately first position.
            data.restoreNow();
            data.insertNext(cella);
        }
    }

    public Binary32 rd(Binary32 _addr)
    {
        //create a cell with the same address as the desidered one with phony content value.
        MemoryCell cella = new MemoryCell(_addr, new Binary32(0));
        //if list is empty gives back null.
        if (data.isEmpty())
            return new Binary32(0);
        //else it search memory cell
        data.rewind();
        MemoryCell now = (MemoryCell) data.next();
        while (data.hasNext() && now.compareTo(cella) == -1)
            now = (MemoryCell) data.next();
        //if the cell where the list has stopped its cycle is the one requested it gives back the inner value.
        if (now.getAddress().equals(_addr))
            return now.getData();
        //else gives back null
        return new Binary32(0);
    }

    //gives back the byte of _addr position in memory, as the mic-1 do!
    public Binary8 fetch(Binary32 _addr)
    {
        //word where is found the byte
        int parola = _addr.getDecimal() / 4;
        try
        {
            //if it founds the desidered byte gives back it.
            return (rd(new Binary32(parola))).getByte(_addr.getDecimal() % 4);
        } catch (NullPointerException e)
        {
            //else it does a beloved nothing and...
        } catch (IllegalArgumentException e)
        {
        }
        //gives back a byte of 0.
        return new Binary8(0);
    }

    //prints the memory contents.
    public String toString()
    {
        String output = "";
        if (data.isEmpty())
            return output;
        data.rewind();
        while (data.hasNext())
            output += ((MemoryCell) data.next()).toString() + "\n";
        return output;
    }

    public int[][] getMemoryContent()
    {
        int[][] toReturn = new int[data.getSize()][2];
        data.rewind();
        int i = 0;
        while (data.hasNext())
        {
            MemoryCell now = (MemoryCell) data.next();
            toReturn[i][0] = now.getAddress().getDecimal();
            toReturn[i++][1] = now.getData().getDecimal();
        }
        return toReturn;
    }

    public Binary32[][] getMemoryBinary()
    {
        Binary32[][] toReturn = new Binary32[data.getSize()][2];
        data.rewind();
        int i = 0;
        while (data.hasNext())
        {
            MemoryCell now = (MemoryCell) data.next();
            toReturn[i][0] = now.getAddress();
            toReturn[i++][1] = now.getData();
        }
        return toReturn;
    }

    //elimina tutte le celle di memoria dall'indirizzo _start in su
    public void deleteFrom(int _start)
    {
        data.rewind();
        while (data.hasNext())
        {
            data.saveNow();
            MemoryCell next = (MemoryCell) data.next();
            if (next.getAddress().getDecimal() >= _start)
            {
                data.restoreNow();
                data.deleteNext();
            }
        }
    }

    //restituisce un array contenente i valori da un indirizzo _start ad un indirizzo _stop
    public int[] getValuesFrom(int _start, int _stop)
    {
        if (_stop < _start)
            return new int[0];
        data.rewind();
        ListDL toReturnL = new ListDL();
        int nowAddr = _start;
        while (data.hasNext())
        {
            data.saveNow();
            MemoryCell now = (MemoryCell) data.next();
            if (now.getAddress().getDecimal() >= _start)
            {
                //è possibile che l'indirizzo nowAddr non esiste, il valore da tornare è dunque 0
                if (now.getAddress().getDecimal() == nowAddr)
                {
                    toReturnL.insertTail(now.getData().getDecimal());
                    nowAddr++;
                } else
                {
                    toReturnL.insertTail(0);
                    nowAddr++;
                    data.restoreNow();
                }
            }
            if (nowAddr > _stop)
                break;
        }
        int[] toReturn = new int[toReturnL.getSize()];
        toReturnL.rewind();
        for (int i = 0; i < toReturn.length; i++)
            toReturn[i] = (Integer) toReturnL.next();
        return toReturn;
    }

    //a simple test
    public static void main(String[] args)
    {
        Memory32 mem = new Memory32();
        mem.wr(new Binary32(0), new Binary32(-607095434));
        mem.wr(new Binary32(2), new Binary32(10));
        mem.wr(new Binary32(2), new Binary32(-20));
        mem.wr(new Binary32(6), new Binary32(7));
        mem.wr(new Binary32(1), new Binary32(1));
        mem.wr(new Binary32(0), new Binary32(1));
        mem.wr(new Binary32(-1), new Binary32(1));
        System.out.println(mem);
        System.out.println(mem.fetch(new Binary32(7)));
        System.out.println(mem.rd(new Binary32(6)));
    }
}

//MemoryCell class, the object to be insert in the linked list.
class MemoryCell implements Comparable<MemoryCell>
{
    private Binary32 address;
    private Binary32 data;

    public MemoryCell(Binary32 _address, Binary32 _data)
    {
        address = _address;
        data = _data;
    }

    public Binary32 getAddress()
    {
        return address;
    }

    public Binary32 getData()
    {
        return data;
    }

    public void setData(Binary32 _data)
    {
        data = _data;
    }

    //compares address in unsigned mode, this way it obtains that 00 is minor than 11 (binary obviuosly!)
    public int compareTo(MemoryCell _toCompare)
    {
        return this.address.compareToUnsigned(_toCompare.getAddress());
    }

    //gives back true if memory cells has the same address.
    public boolean equals(MemoryCell _toCompare)
    {
        return address.equals(_toCompare.getAddress());
    }

    //gives back address+value
    public String toString()
    {
        return address.getDecimal() + " " + (address.getDecimal() >= 65536 ? data.toString() : data.getDecimal());
    }
}