package ADT.ListDL;

//una nuova estensione della classe RuntimeException, l'eccezione pu� essere lanciata dal metodo
//delete della classe ListLS
public class EmptyListException extends RuntimeException
{
    public EmptyListException(String msg)
    {
        super(msg);
    }
}