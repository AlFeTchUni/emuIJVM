package ADT.ListDL;

import ADT.ListDL.*;

public class ListDLTest
{
    public static void main(String[] args)
    {
        //creo un oggetto ListDL
        ListDL myListDL = new ListDL();
        System.out.println("-------------\nLista creata!\n-------------");
        //inserisco un oggetto Integer utilizzando l'autoBoxing
        System.out.println("-------------\nInserisco una testa ed una coda\n-------------");
        myListDL.insertHead(2);
        myListDL.insertTail(11);
        System.out.println("-------------\nStampo la lista\n-------------");
        myListDL.printList();
        //inserisco un p� di nodi
        System.out.println("-------------\nInserisco un p� di nodi\n-------------");
        int total = (int) (Math.random() * 50);
        for (int i = 0; i < total; i++)
        {
            int myInt = (int) (Math.random() * 10);
            if (myInt % 2 == 0)
                myListDL.insertHead(myInt);
            else
                myListDL.insertTail(myInt);
        }
        System.out.println("-------------\nStampo la lista\n-------------");

        //stampo nuovamente la lista
        myListDL.printList();

        //cerco il numero 10
        System.out.println("-------------\nCerco tutti i 5 e li accerchio con 500\n-------------");
        //riavvolgo la lista
        myListDL.rewind();
        //faccio la ricerca di tutti i 5
        while (myListDL.locateNext(5))
        {
            myListDL.insertPrev(500);
            myListDL.insertNext(500);
            //System.out.println("Trovato");
        }
        System.out.println("-------------\nStampo la lista\n-------------");
        myListDL.printList();
        myListDL.insertTail(500);
        System.out.println("-------------\nRimuovo tutti i 500\n-------------");
        myListDL.delete(500);
        System.out.println("-------------\nStampo la lista\n-------------");
        myListDL.printList();
        System.out.println("-------------\nSvuto la lista con deleteHead\n-------------");
        //svuoto la lista
        while (!myListDL.isEmpty())
        {
            myListDL.deleteHead();
        }
        System.out.println("-------------\nStampo la lista\n-------------");
        myListDL.printList();
        System.out.println("-------------\nCauso un'eccezione\n-------------");
        myListDL.deleteHead();


    }
}