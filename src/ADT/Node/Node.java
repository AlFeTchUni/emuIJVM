package ADT.Node;

/*
Un classe autoreferenziante � un particolare tipo di classe che ha tra le sue variabili d'istanza un riferimento ad un oggetto
della medesima classe. Questo tipo di classi sono molto utili per l'implementazione di liste concatenate. La prossima classe Node �
autoreferenziante e pu� contenere oltre al riferimento che la rende tale una qualsiasi sottoclasse di Object.

Cmq per il pagamento di queste lezioni private potete contattarmi a dariospyATtiscali.it (AT = @) ;).
*/
public class Node {
    //la prossima variabile permette la conservazione all'interno del nodo di una qualsiasi tipo di dato (anche int e compagnia bella, se non
    //ci credi prova a cercare Classi Wrapper Java su google!
    private Object object;

    //il riferimento al prossimo Node
    private Node next;

    //riferimento al Node precedente
    private Node prev;

    //costruttore a tre argomenti
    public Node(Object _object, Node _next, Node _prev) {
        object = _object;
        next = _next;
        prev = _prev;
    }

    //costruttore a due argomenti
    public Node(Object _object, Node _next) {
        this(_object, _next, null);
    }

    //costruttore ad un solo argomento
    public Node(Object _object) {
        //per convenzione un nodo che � candidato ad essere l'ultimo della lista punta a null
        this(_object, null, null);
    }

    //metodo che modifica il riferimento al nodo successivo
    public void setNext(Node _next) {
        next = _next;
    }

    //restituisce il valore di next
    public Node getNext() {
        return next;
    }

    //metodo che crea un nuovo nodo e successivamente assegna il suo indirizzo di memoria a next
    public void setNext(Object _object) {
        next = new Node(_object);
    }

    //restituisce prev
    public Node getPrev() {
        return prev;
    }

    //modifica prev
    public void setPrev(Node _newPrev) {
        prev = _newPrev;
    }

    //modifca prev creando un nuovo Node contenente _object
    public void setPrev(Object _newDataPrev) {
        this.setPrev(new Node(_newDataPrev));
    }

    //restituisce l'oggetto object
    public Object getData() {
        return object;
    }

    public void setData(Object _data) {
        object = _data;
    }
}