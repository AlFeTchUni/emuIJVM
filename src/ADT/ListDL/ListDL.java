package ADT.ListDL;
/*
  Dario Spitaleri 
  5/04/2008 19:12
  "Le liste linkate doppie"
  Implementazione di una lista linkata doppia con annessi nodi e metodi accessori.
  Spero possa esservi utile!
  Per ogni domanda, consiglio, critica dariospyATtiscali.it (AT=@).
  Have a lot of fun!
*/

import ADT.Node.Node;

public class ListDL {
    private Node head;
    private Node tail;
    //la prossima variabile d'istanza server per poter navigare all'interno della lista
    private Node now;
    private Node savedNow;
    private int size;

    //il costruttore senza argomenti, semplicissimo
    public ListDL() {
        head = tail = now = null;
        size = 0;
    }

    //il costruttore con solo l'object che costituir� la testa e la coda
    public ListDL(Object _head) {
        head = tail = now = new Node(_head);
        size = 1;
    }

    //il costruttore con due Object
    public ListDL(Object _head, Object _tail) {
        head = new Node(_head);
        tail = new Node(_tail);
        tail.setPrev(head);
        head.setNext(tail);
        now = head;
        size = 2;
    }

    //metodi GET

    //restituisce la coda
    public Object getTail() {
        return tail.getData();
    }

    //modifca tail
    public void setTail(Object _toSet) {
        tail.setData(_toSet);
    }

    //restituisce il nodo coda
    public Node getTailNode() {
        return tail;
    }

    //restituisce la testa
    public Object getHead() {
        return head.getData();
    }

    //modifica head
    public void setHead(Object _toSet) {
        head.setData(_toSet);
    }

    //metodi SET

    //restituisce il Node testa
    public Node getHeadNode() {
        return head;
    }

    //restituisce l'oggetto a cui punta now
    public Object getNow() {
        return now.getData();
    }

    //modifca now
    public void setNow(Object _toSet) {
        now.setData(_toSet);
    }

    //modifca il successivo di now
    public void setNext(Object _toSet) {
        if (now.getNext() != null)
            now.getNext().setData(_toSet);
        else
            throw new NoNextException("There is no next for this Node");
    }

    //modifica il precedente di now
    public void setPrev(Object _toSet) {
        if (now.getPrev() != null)
            now.getPrev().setData(_toSet);
        else
            throw new NoNextException("There is no prev for this Node");
    }

    //metodi utili per lo scorrimento della lista

    //restituisce l'oggetto contenuto nel successivo di now, e aggiorna now
    public Object next() {
        if (now == null) {
            if (isEmpty())
                return new EmptyListException("Empty list, no next!");
            now = head;
            return now.getData();
        }
        if (now.getNext() != null)
            now = now.getNext();
        else
            throw new NoNextException("There is no next for this Node");
        return now.getData();
    }

    //restituisce l'oggetto contenuto nel precedente di now, e aggiorna now
    public Object prev() {
        if (now.getPrev() != null)
            now = now.getPrev();
        else
            throw new NoNextException("There is no prev for this Node");
        return now.getData();
    }

    //riavvolge la lista in modo tale che now sia uguale ad head
    public void rewind() {
        now = null;
    }

    //riavvolge la lista in modo tale che now sia uguale a tail
    public void fforward() {
        now = tail;
    }

    //restituisce true se now ha un successivo
    public boolean hasNext() {
        if (isEmpty())
            return false;
        if (now == null)
            return true;
        if (now.getNext() != null)
            return true;
        return false;
    }

    //restituisce true se now ha un precedente
    public boolean hasPrev() {
        if (isEmpty())
            return false;
        if (now.getPrev() != null)
            return true;
        return false;
    }

    //posiziona now alla prima occorrenza di _toLocate, parte dalla testa
    public boolean locateFromHead(Object _toLocate) {
        Node old = now;
        rewind();
        while (hasNext())
            if (_toLocate.equals(next()))
                return true;
        if (tail.getData().equals(_toLocate)) {
            now = tail;
            return true;
        }
        now = old;
        return false;
    }

    //posiziona now alla prima occorrenza di _toLocate, parte dalla coda
    public boolean locateFromTail(Object _toLocate) {
        Node old = now;
        fforward();
        if (now.getData().equals(_toLocate))
            return true;
        while (hasPrev())
            if (_toLocate.equals(prev()))
                return true;
        if (head.getData().equals(_toLocate)) {
            now = head;
            return true;
        }
        now = old;
        return false;
    }

    //posiziona now alla prima occorrenza di _toLocate, parte dalla posizione di now in poi
    public boolean locateNext(Object _toLocate) {
        Node old = now;
        while (hasNext())
            if (_toLocate.equals(next()))
                return true;
        if (tail.getData().equals(_toLocate)) {
            now = tail;
            return true;
        }
        now = old;
        return false;
    }

    //posiziona now alla prima occorrenza di _toLocate, parte dalla posizione di now a ritroso
    public boolean locatePrev(Object _toLocate) {
        Node old = now;
        while (hasPrev())
            if (_toLocate.equals(prev()))
                return true;
        if (head.getData().equals(_toLocate)) {
            now = head;
            return true;
        }
        now = old;
        return false;
    }
    //metodi insert

    //inserisce una nuova coda, parametro Object
    public void insertTail(Object _newTail) {
        if (isEmpty()) {
            head = tail = new Node(_newTail);
            size++;
            return;
        }
        Node newTail = new Node(_newTail);
        tail.setNext(newTail);
        newTail.setPrev(tail);
        tail = newTail;
        if (now == tail.getPrev())
            now = tail;
        size++;
    }

    //inserisce una nuova testa
    public void insertHead(Object _newHead) {
        if (isEmpty()) {
            head = tail = new Node(_newHead);
            size = 1;
            return;
        }
        Node newHead = new Node(_newHead);
        head.setPrev(newHead);
        newHead.setNext(head);
        head = newHead;
        if (now == head.getNext())
            now = head;
        size++;
    }

    //inserisce prima di now
    public void insertPrev(Object _toInsert) {

        //controllo che il prossimo elemento esista, se non esiste effettuo un inserimento in testa
        if (hasPrev()) {
            Node toInsert = new Node(_toInsert);
            toInsert.setNext(now);
            toInsert.setPrev(now.getPrev());
            now.getPrev().setNext(toInsert);
            now.setPrev(toInsert);
            size++;
            return;
        }
        insertHead(_toInsert);
        size++;
    }

    //inserisce dopo di now
    public void insertNext(Object _toInsert) {
        if (now == null) {
            insertHead(_toInsert);
            return;
        }
        //controllo che il prossimo elemento esista, se non esiste effettuo un inserimento in coda
        if (hasNext()) {
            Node toInsert = new Node(_toInsert);
            toInsert.setPrev(now);
            toInsert.setNext(now.getNext());
            now.getNext().setPrev(toInsert);
            now.setNext(toInsert);
            size++;
            return;
        }
        insertTail(_toInsert);
        size++;
    }

    //metodi delete

    //elimina head
    public Object deleteHead() {
        if (isEmpty())
            throw new EmptyListException("Empty List, no delete actions are permitted");
        Node oldHead = head;
        head = head.getNext();
        if (!isEmpty()) {
            head.setPrev(null);
            if (now == oldHead)
                fforward();
        }
        size--;
        return oldHead.getData();
    }

    //tenta di rimuovere la coda
    public Object deleteTail() {
        if (isEmpty())
            throw new EmptyListException("Empty List, no delete actions are permitted");
        Node oldTail = tail;
        tail = tail.getPrev();
        if (tail != null) {
            tail.setNext(null);
            if (now == oldTail)
                rewind();
        }

        size--;
        return oldTail.getData();
    }

    //rimuove now e sposta now in avanti
    public Object deleteNow() {
        if (isEmpty())
            throw new EmptyListException("Empty List, no delete actions are permitted");
        if (now == null) {
            return deleteHead();

        }
        if (now != head)
            now.getPrev().setNext(now.getNext());
        if (now != tail)
            now.getNext().setPrev(now.getPrev());
        if (now == head && now == tail)
            makeEmpty();
        Object toReturn = now.getData();
        now = now.getNext();
        size--;
        return toReturn;
    }

    //rimuove dopo di now
    public Object deleteNext() {
        if (isEmpty())
            throw new EmptyListException("Empty List, no delete actions are permitted");
        Object toReturn = now.getNext().getData();
        now.setNext(now.getNext().getNext());
        now.getNext().setPrev(now);
        size--;
        return toReturn;
    }

    //rimuove prima di now
    public Object deletePrev() {
        if (isEmpty())
            throw new EmptyListException("Empty List, no delete actions are permitted");
        Object toReturn = now.getPrev().getData();
        now.setPrev(now.getPrev().getPrev());
        now.getPrev().setNext(now);
        size--;
        return toReturn;
    }

    //rimuove tutte le occorrenze di _toDelete
    public void delete(Object _toDelete) {
        if (isEmpty())
            throw new EmptyListException("Lista vuota");
        if (_toDelete.equals(head.getData()))
            deleteHead();
        Node fromHead = head;
        Node fromHeadN = fromHead;
        //faccio una ricerca che parte dalla fine e dall'inizio fino ad arrivare al centro lista
        while (fromHead != null) {
            //se trovo un'occorrenza aggiorno opportunamente
            if (fromHead.getData().equals(_toDelete)) {
                fromHead.getPrev().setNext(fromHead.getNext());
                if (fromHead.getNext() != null)
                    fromHead.getNext().setPrev(fromHead.getPrev());
                //il seguente giochetto serve per controllare che now non punti ad un nodo eliminato
                fromHeadN = fromHead.getNext();
                fromHead.setNext(null);
                fromHead.setPrev(null);
                fromHead = fromHeadN;
                size--;
            } else
                fromHead = fromHead.getNext();
        }
        //se now non ha ne prev ne next, allora si tratta di un nodo eliminato
        if (now.getNext() == null && now.getNext() == null)
            if (now != tail)
                now = now.getNext();
            else if (now != head)
                now = now.getPrev();
    }

    //salva now
    public void saveNow() {
        savedNow = now;
    }

    public void restoreNow() {
        now = savedNow;
    }


    //stampa la lista
    public void printList() {
        if (isEmpty()) {
            System.out.println("No such elements");
            return;
        }
        Node current = head;
        int i = 0;
        while (current != null) {
            System.out.printf("Node:  %-3s valore: %-3s indirizzo: %-22s precedente: %-22s successivo: %s\n", i++, current.getData(), current, current.getPrev(), current.getNext());
            current = current.getNext();
        }
    }

    //controlla se la lista � vuota
    public boolean isEmpty() {
        return head == null;
    }

    //svuota la lista
    public void makeEmpty() {
        tail = head = now = null;
    }

    public int getSize() {
        return size;
    }
}
	