package ADT.Queue;

import ADT.ListDL.*;

//semplice coda
public class Queue
{
    private ListDL data;

    public Queue()
    {
        data = new ListDL();
    }

    public void enqueue(Object _toInsert)
    {
        data.insertTail(_toInsert);
    }

    public Object dequeue()
    {
        if (!data.isEmpty())
            return data.deleteHead();
        else
            throw new EmptyQueueException("Empty Queue, unable to dequeue");
    }

    public boolean isEmpty()
    {
        return data.isEmpty();
    }

    public void makeEmpty()
    {
        data = new ListDL();
    }
}