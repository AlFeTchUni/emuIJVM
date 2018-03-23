/*
	Dario Spitaleri
	Venerd� 18/4/2008 18:14
	Liste Linkate Semplici
	Un'implementazione delle liste linkate semplici con metodi accessori
	Spero possa esservi utile.
	Ogni critica, suggerimento, complimento a dariospyATtiscali.it (AT=@)
	Have a lot of fun!
*/
package ADT.ListLS;

public class ListLS {
    //il nodo che � la testa della nostra lista
    private Node head;
    //questo nodo ci serve per la navigazione della lista,
    private Node now;
    //permette di conservare now in una data posizione
    private Node savedNow;
    //il costruttore senza argomenti
    private int size;

    public ListLS() {
        head = now = savedNow = null;
        size = 0;
    }

    //il costruttore con un oggetto Object
    public ListLS(Object _head) {
        head = new Node(_head);
        now = savedNow = null;
        size = 1;
    }

    //metodi GET

    //restituisce il dato della testa
    public Object getHead() {
        if (isEmpty())
            throw new EmptyListException("Lista vuota");
        return head.getData();
    }

    //metodi SET
    //modifica il dato contenuto in testa
    public void setHead(Object _head) {
        if (isEmpty()) {
            insertHead(_head);
            return;
        }
        head.setData(_head);
    }

    //restituisce il contenuto di now
    public Object getNow() {
        if (isEmpty())
            throw new EmptyListException("Empty List");
        if (now == null)
            return null;
        return now.getData();
    }

    public void setNow(Object _toInsert) {
        if (isEmpty())
            throw new EmptyListException("Lista vuota");
        if (now == null) {
            setHead(_toInsert);
            return;
        }
        now.setData(_toInsert);
    }

    //restituisce il contenuto del successivo di now
    public Object getNext() {
        if (isEmpty())
            throw new EmptyListException("Empty List");
        if (now == null)
            return head.getData();
        if (hasNext())
            return now.getNext().getData();
        throw new NoNextException("There is no next for this Node");
    }

    public void setNext(Object _toInsert) {
        if (isEmpty())
            throw new EmptyListException("Empty List");
        //se now � null aggiorno la testa
        if (now == null) {
            setHead(_toInsert);
            return;
        }
        //se esiste un successivo, lo aggiorno, altrimenti lancio un eccezione
        if (hasNext())
            now.getNext().setData(_toInsert);
        else
            throw new NoNextException("There is no next for this node");
    }

    //metodi insert
    //inserisce un elemento in testa
    public void insertHead(Object _nHead) {
        size++;
        Node newHead = new Node(_nHead);
        newHead.setNext(head);
        head = newHead;
        if (now == head.getNext())
            now = head;
    }

    //inserisce un elemento in coda, metodo dispendioso per le liste linkate semplici
    public void insertTail(Object _tail) {
        size++;
        Node tail = new Node(_tail);
        //se � vuota
        if (isEmpty()) {
            head = tail;
            now = null;
            return;
        }
        //se non  � vuota scorro fino in fondo
        Node ora = head;
        while (ora.getNext() != null) {
            ora = ora.getNext();
        }
        ora.setNext(tail);
    }

    //inserisce un elemento subito dopo now
    public void insertNext(Object _toInsert) {
        size++;
        if (isEmpty()) {
            head = now = new Node(_toInsert);
            return;
        }
        if (now == null) {
            insertHead(_toInsert);
            return;
        }
        Node toInsert = new Node(_toInsert);
        toInsert.setNext(now.getNext());
        now.setNext(toInsert);
        now = toInsert;
    }

    //inserisce un oggetto in modo ordinato descrescente, gli oggetti dentro i Node devono
    //essere in questo caso Comparable
    public void insertOrdered(Object _toInsert) {
        try {
            size++;
            Node toInsert = new Node(_toInsert);
            //se la lista � vuota inserisco l'oggetto in testa
            if (isEmpty()) {
                head = toInsert;
                return;
            }
            //faccio il casting degli oggetti da comparare, qui potrebbe essere lanciata ClassCastException
            Comparable headC = (Comparable) head.getData();
            Comparable toInsertC = (Comparable) toInsert.getData();
            //Se l'oggetto del nodo � uguale a quello della testa, lo inserisco al secondo posto
            if (toInsertC.compareTo(headC) == 0) {
                toInsert.setNext(head.getNext());
                head.setNext(toInsert);
                return;
            }
            //se l'oggetto � invece pi� piccolo lo inserisco prima
            else if (toInsertC.compareTo(headC) == -1) {
                toInsert.setNext(head);
                head = toInsert;
                return;
            }
            //altrimenti faccio una ricerca della posizione
            Node aux = head.getNext();
            if (aux != null) {
                Comparable toCompare = (Comparable) aux.getData();
                Node prev = head;
                while (aux != null && toInsertC.compareTo(toCompare) == 1) {
                    prev = aux;
                    aux = aux.getNext();
                    if (aux != null)
                        toCompare = (Comparable) aux.getData();
                }
                toInsert.setNext(aux);
                prev.setNext(toInsert);
            } else
                head.setNext(new Node(_toInsert));

        }
        //se gli oggetti contenuto dentro i nodi non implementano Comparable viene creata questa eccezione
        catch (ClassCastException e) {
            System.out.println("Impossible to Compare, the Object in the Node do not implements the Comparable interface");
            return;
        }
    }

    //metodi DELETE

    //rimuove la testa e restituisce il dato contenuto
    public Object deleteHead() {
        if (isEmpty())
            throw new EmptyListException("The list is empty, is not possible to delete");
        size--;
        Object toReturn = head.getData();
        //se now � head, lo aggiorno
        if (now == head)
            now = head.getNext();
        head = head.getNext();
        return toReturn;
    }

    //rimuove la coda e restituisce il dato contenuto
    public Object deleteTail() {
        if (head == null)
            throw new EmptyListException("The list is empty, is not possible to delete");
        size--;
        Node ora = head.getNext();
        Node prev = head;
        if (ora == null) {
            Object toReturn = head.getData();
            head = null;
            return toReturn;
        }
        while (ora.getNext() != null) {
            ora = ora.getNext();
            prev = prev.getNext();
        }
        if (now == prev.getNext())
            now = prev;
        prev.setNext(null);
        return ora.getData();
    }

    //rimuove il successivo di now
    public Object deleteNext() {
        if (isEmpty())
            throw new EmptyListException("Empty List");
        if (now == null)
            return deleteHead();
        if (hasNext()) {
            Object toReturn = now.getNext().getData();
            now.setNext(now.getNext().getNext());
            size--;
            return toReturn;
        }
        throw new NoNextException("There is no next for this Node");
    }

    //rimuove tutte le occorrenze di _toDelete
    public void delete(Object _toDelete) {
        //se la lista � vuota lancio un eccezione
        if (isEmpty())
            throw new EmptyListException("The list is empty, is not possible to delete");
        //controllo se il nodo � la testa
        if (head.getData().equals(_toDelete)) {
            deleteHead();
            size--;
            return;
        }
        Node prev = head;
        Node ora = head.getNext();
        //altrimenti cerco il nodo
        while (ora != null) {
            if (ora.getData().equals(_toDelete)) {
                prev.setNext(ora.getNext());
                size--;
            }
            ora = ora.getNext();
            prev = prev.getNext();
        }
        //se il nodo rimosso � now, aggiorno now in modo tale che punti al nodo successivo
        if (now == ora)
            now = ora.getNext();
        prev.setNext(ora.getNext());
    }


    //metodi per la NAVIGAZIONE
    //restituisce il dato conenuto nel successivo di now, se non esiste lancio un eccezione, se la lista
    //� vuota lancio un eccezione
    public Object next() {
        if (isEmpty())
            throw new EmptyListException("The list is Empty!");
        //se now � null allora � prima della testa, restituisco la testa
        if (now == null) {
            now = head;
            return now.getData();
        }
        //se non esiste un prossimo
        if (!hasNext())
            throw new NoNextException("There is no next for this Node");

        //aggiorno now
        now = now.getNext();
        return now.getData();
    }

    //restituisce true se next ha un successivo
    public boolean hasNext() {
        if (isEmpty())
            return false;
        if (now == null)
            return true;
        if (now.getNext() != null)
            return true;
        else
            return false;
    }

    //elimina il nodo now
    //"riavvolge" la lista
    public void rewind() //a tribute to Vasco
    {
        now = null;
    }

    //cerca di trovare un Object nella lista, se lo trova restituisce true e sposta now nel nodo trovato
    //cos� alla prossima chiamata di next viene restituito il dato contenuto dal nodo successivo a quello trovato
    public boolean locate(Object _toLocate) {
        //se � vuota non faccio nulla
        if (isEmpty())
            return false;
        Node myNode = head;
        //cerco il nodo
        while (myNode != null && !myNode.getData().equals(_toLocate)) {
            myNode = myNode.getNext();
        }
        //se � stato trovato aggiorno now e torno true
        if (myNode != null) {
            now = myNode;
            return true;
        }
        //altrimenti torno false e non modifico now
        else
            return false;
    }

    //conserva now
    public void saveNow() {
        savedNow = now;
    }

    //riporta now alla posizione conservata
    public void restoreNow() {
        now = savedNow;
    }

    //altri metodi

    //svuota la lista
    public void makeEmpty() {
        head = null;
        now = null;
        size = 0;
    }

    //restiusce true se la lista � vuota, false altrimenti
    public boolean isEmpty() {
        return head == null;
    }

    //stampa la lista
    public void printList() {
        if (isEmpty())
            System.out.println("No such elements");
        Node myNode = head;
        int i = 0;
        while (myNode != null) {
            System.out.printf("Node: %-5s  content: %-5s address %-22s next %-22s\n", i, myNode.getData(), myNode, myNode.getNext());
            myNode = myNode.getNext();
            i++;
        }
    }

    public int getSize() {
        return size;
    }
}