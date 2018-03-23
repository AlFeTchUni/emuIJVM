package ADT.ListLS;

import ADT.ListLS.ListLS;

public class ListTest
{
    public static void main(String[] args)
    {
        //utilizzo la classe wrapper Integer per rappresentare dei valori interi nella lista
        Integer[] myInt = new Integer[100];

        //riempio l'array con valori casuali
        for (int i = 0; i < myInt.length; i++)
            myInt[i] = new Integer((int) (Math.random() * 100));

        //creo la lista passando  myInt[0];)
        ListLS myList = new ListLS(myInt[0]);

        System.out.println("*******************************Stampo la lista*******************************");
        //stampo la lista
        myList.printList();

        System.out.println("*******************************Svuoto la lista, usando makeEmpty()*******************************");
        //svuoto la lista
        myList.makeEmpty();

        //stampo nuovamente la lista
        System.out.println("*******************************Stampo nuovamente la lista (vuota)*******************************");
        myList.printList();

        //riempio la lista usando i metodi di ListLS
        System.out.println("*******************************Riempio la lista usando i metodi di ListLS*******************************");
        for (int i = 0; i < 100; i++)
        {
            //lo inserisco in coda
            myList.insertTail((int) (Math.random() * 100));
            myList.insertHead((int) (Math.random() * 10));
        }

        //stampo ancora una volta la lista, piena questa volta
        System.out.println("*******************************Stampo ancora una volta la lista*******************************");
        myList.printList();

        //cerco il Node con valore 5
        System.out.println("*******************************Cerco il Node con valore 5*******************************");
        boolean myFinded = myList.locate(5);
        System.out.printf("Nodo %s\n", myFinded ? "trovato" : "non trovato");

        //inserisco dopo il Node con valore 6 il valore 500, se � stato trovato
        System.out.println("*******************************Utilizzo insertAfter()*******************************");
        if (myFinded)
        {
            myList.insertNext(500);
            System.out.print("Inserito Node con valore 500 dopo quello con valore 5, ora ristampo la lista\n");
            myList.printList();
        }

        //svuoto la lista senza usare makeEmpty
        System.out.println("*******************************Svuoto la lista senza usare makeEmpty()*******************************");
        while (!myList.isEmpty())
        {
            System.out.printf("Eliminato %s\n", myList.deleteHead());
        }

        //stampo la lista (vuota)
        System.out.println("*******************************Stampo ancora una volta la lista*******************************");
        myList.printList();

        //inserisco degli elementi ordinati
        System.out.println("*******************************Inserisco degli elementi in modo ordinato*******************************");
        for (int i = 0; i < 100; i++)
        {
            myList.insertOrdered((int) (Math.random() * 100));
        }
        System.out.println("*******************************Stampo la lista ordinata*******************************");
        myList.printList();
    }
}