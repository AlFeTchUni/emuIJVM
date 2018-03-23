package Assembler;

import Numbers.*;

public class Constant
{
    private String name;
    private Binary32 value;

    public Constant(String _name, int _value)
    {
        name = _name;
        value = new Binary32(_value);
    }

    public String getName()
    {
        return name;
    }

    public Binary32 getValue()
    {
        return value;
    }

    public void setValue(int _t)
    {
        value = new Binary32(_t);
    }
}