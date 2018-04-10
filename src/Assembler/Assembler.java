/*
  Inizio oggi, mercoledì 9/7/2008 ad implementare emuIJVM completamente solo!
  La seguente classe si occupa dell'assemblaggio del codice assembly scritto dall'utente generandone una versione 
  binaria (o comunque esadecimale) che sarà poi caricata nella memoria di emuMIC che eseguirà il tutto!
*/
package Assembler;

import ADT.ListDL.ListDL;
import Emulator.SystemError;
import Numbers.Binary32;
import Numbers.Binary8;

import java.io.*;
import java.math.BigInteger;

public class Assembler {
    //l'ultimo byte del metodo main, serve per fermare la computazione
    int mainLastByte = 0;
    //conterrà le istruzioni tradotte
    private ListDL decoded;
    //contiene tutti gli opcode riconosciuti dall'assemblatore
    private ListDL referencesTable;
    //la tabella delle costanti
    private ListDL constantsTable;
    //la tabella dei simboli
    private ListDL signsTable;
    //il modulo del programma, contiene un insieme di liste di istruzioni, una per ogni metodo
    private ListDL programModule;

    //il costruttore inizializza semplicemente tutti i campi
    public Assembler() {
        referencesTable = new ListDL();

        //carico gli opcode dal file opcode
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream("./Assembler/opcode.lst"));
            while (true)
                referencesTable.insertTail(input.readObject());
        } catch (EOFException e) {
        } catch (IOException e) {
            throw new SystemError("I/O error, the file Assembler/opcode.lst may be corrupt\n" + e);
        } catch (ClassNotFoundException e) {
            throw new SystemError("The file Assembler/opcode.lst is corrupt");
        }

    }

    public static void main(String[] args) {

        short ciao = 5000;

    }

    //traduce una sequenza di istruzioni
    public void translate(String[] _toTranslate) {
        if (_toTranslate.length == 0)
            throw new TranslationError("Program not found!");
        //rimuove i commenti e le istruzioni vuote
        for (int i = 0; i < _toTranslate.length; i++) {
            if (_toTranslate[i].contains("//"))
                _toTranslate[i] = _toTranslate[i].substring(0, _toTranslate[i].indexOf("/"));
            if (_toTranslate[i].replaceAll(" ", "").replaceAll("\t", "").length() == 0) {
                String[] toTrN = new String[_toTranslate.length - 1];
                for (int j = 0, k = 0; j < _toTranslate.length; j++)
                    if (j != i) {
                        System.out.println(_toTranslate[j] + j);
                        toTrN[k++] = _toTranslate[j];
                    }
                /*i--;
                _toTranslate = toTrN;*/
            }
        }
        System.out.println("-----ASSEMBLER STANDARD OUTPUT-----");
        constantsTable = new ListDL();
        signsTable = new ListDL();
        programModule = new ListDL();
        decoded = new ListDL();
        //inserisco nella constantsTable una costante fittizia, objref, serve per le chiamate di metodi
        //secondo le specifiche Tanmbaumniane
        constantsTable.insertTail(new Constant("objref", 0));
        //pulisco tutte le righe di istruzioni dagli spazi in eccesso
        int first_nempty_line = -1;
        for (int i = 0; i < _toTranslate.length; i++) {
            _toTranslate[i] = _toTranslate[i].replaceAll("\\s+", " ");
            if (first_nempty_line == -1 && !_toTranslate[i].isEmpty())
                first_nempty_line = i;
        }
        //cerco le costanti
        if (_toTranslate[first_nempty_line].contains(".constant")) {
            if (_toTranslate[first_nempty_line].replace(" ", "").equals(".constant")) {
                _toTranslate[first_nempty_line] = null;
                int j = first_nempty_line+1;
                while (!_toTranslate[j].contains(".end-constant")) {
                    String[] constant = _toTranslate[j].trim().split(" ");
                    if (constant.length != 2)
                        throw new TranslationError("Malformed constant declaration: \n"
                                + _toTranslate[j] + "\non line: " + (j + 1));
                    //pulisco il nome della costante
                    String name = purge(constant[0]);
                    //controllo la correttezza del nome
                    String nameP = name.replaceAll("\\w", "");
                    int value = 0;
                    if (nameP.length() != 0)
                        throw new TranslationError("Constant's name not permitted: \n" + name + "\non line: " + (j + 1));
                    try {
                        if (constant[1].startsWith("0x")) {
                            String hex = constant[1].split("0x")[1];
                            BigInteger bi = new BigInteger(hex, 16);
                            value = bi.intValue();
                        } else {
                            value = Integer.parseInt(constant[1]);
                        }
                    } catch (NumberFormatException e) {
                        throw new TranslationError(constant[1] + " is NaN on line: " + (j + 1));
                    } catch (IndexOutOfBoundsException e) {
                        throw new TranslationError("Invalid Hex number on line: " + (j + 1));
                    }
                    constantsTable.insertTail(new Constant(name, value));
                    _toTranslate[j] = null;
                    j++;
                    if (j > _toTranslate.length)
                        throw new TranslationError(".end-constant expected");
                }
                if (!_toTranslate[j].replace(" ", "").equals(".end-constant"))
                    throw new TranslationError("Unknown instrunction:\n" + _toTranslate[j] + "\non line: " + (j + 1));
                _toTranslate[j] = null;
            }
        }
        //cerco il main
        String[] main = new String[2];
        boolean mainF = false;
        int mainInit = 0;
        for (int i = 0; i < _toTranslate.length; i++) {
            if (_toTranslate[i] != null && _toTranslate[i].trim().equals(".main")) {
                _toTranslate[i] = ".method main()";
                if (mainF)
                    throw new TranslationError("main method already defined \non line " + (i + 1));
                mainF = true;
                mainInit = i + 1;
                //costruisco la firma del metodo
                //main[0] = _toTranslate[i];
                int index = 0;
                while (!_toTranslate[i].contains(".end-main")) {
                    //inserisco nell'array del main la nuova riga di codice
                    main[index++] = _toTranslate[i];
                    i++;
                    if (i >= _toTranslate.length)
                        throw new TranslationError(".end-main expected\n");
                    if (_toTranslate[i].contains("."))
                        if (!_toTranslate[i].trim().equals(".var") && !_toTranslate[i].trim().equals(".end-main") &&
                                !_toTranslate[i].trim().equals(".end-var"))
                            throw new TranslationError("Unexpected: " + _toTranslate[i] + " in method main");
                    if (index >= main.length) {
                        String[] mainN = new String[main.length + 1];
                        for (int k = 0; k < main.length; k++)
                            mainN[k] = main[k];
                        main = mainN;
                    }
                    //elimino l'istruzione appena inserita nel main
                    _toTranslate[i - 1] = null;
                }
                if (_toTranslate[i].replace(" ", "").equals(".end-main"))
                    main[index] = ".end-method";
                else
                    throw new TranslationError("Unknown instrunction:\n" + _toTranslate[i] +
                            "\nin method: main\non line: " + (i + 1));
                _toTranslate[i] = null;
            }
        }
        //se non esiste il main
        if (!mainF)
            throw new TranslationError("main method not found");
        //traduco il main
        Method created = firstStep(main, mainInit);
        System.out.println("----Translating: " + created.getName() + "----");
        secondStep(created, mainInit);
        mainLastByte = (created.getSize() + (int) Math.pow(2, 16) * 4) + 3;
        System.out.println(mainLastByte);
        int methodInit = 0;
        //cerco e divido gli altri metodi
        for (int i = 0; i < _toTranslate.length; i++) {
            if (_toTranslate[i] != null) {
                String[] nowMethod = new String[2];
                if (_toTranslate[i].contains(".method")) {
                    int index = 0;
                    methodInit = i + 1;
                    while (!_toTranslate[i].contains(".end-method")) {
                        nowMethod[index++] = _toTranslate[i++];
                        if (index >= nowMethod.length) {
                            String[] nowMethodN = new String[nowMethod.length + 1];
                            for (int j = 0; j < nowMethod.length; j++)
                                nowMethodN[j] = nowMethod[j];
                            nowMethod = nowMethodN;
                            if (i >= _toTranslate.length)
                                throw new TranslationError(".end-method expected on line " + i);
                        }
                        _toTranslate[i - 1] = null;
                    }
                    nowMethod[index] = _toTranslate[i];
                    _toTranslate[i] = null;
                } else if (_toTranslate[i].replaceAll("\\s+", "").trim().isEmpty())
                    continue;
                else
                    throw new TranslationError("Unknown instrunction:\n" + _toTranslate[i] + "\non line: " + i);
                //traduco il metodo
                Method now = firstStep(nowMethod, methodInit);
                System.out.println("----Translating: " + now.getName() + "----");
                secondStep(now, methodInit);
            }
        }

        //procedo col likcaggio
        linkage();
        System.out.println("\n-----END OF ASSEMBLER STANDARD OUTPUT-----");
    }

    //effettua il primo step di un metodo
    public Method firstStep(String[] _instrunctions, int methodInit) {
        //controllo la dichiarazione del metodo
        String direttiva = _instrunctions[0];
        String nomeMetodo = "";
        String[] parameters = new String[0];
        if (direttiva.substring(0, 8).equals(".method ")) {
	    /*
	      DECODIFICA DELLA FIRMA DEL METODO
	    */
            //estraggo il nome del metodo
            if (direttiva.contains("("))
                nomeMetodo = direttiva.substring(8, direttiva.indexOf('('));
            else
                throw new TranslationError("Unknown instrution: \n " + direttiva +
                        " \n on line: " + methodInit);
            //elimino gli spazi dal nome del metodo
            nomeMetodo = purge(nomeMetodo);
            //controllo la correttezza del nome del metodo
            String nomeMetodoP = nomeMetodo.replaceAll("\\w", "");
            if (nomeMetodoP.length() != 0)
                throw new TranslationError("Method name not permitted: \n" + nomeMetodo + " \n on line: " + methodInit);
            //estraggo i parametri
            String parametri = direttiva.substring(direttiva.indexOf('('));
            //controllo che la stringa sia ben parentizzata
            parametri = purge(parametri);
            if (parametri.charAt(parametri.length() - 1) == ')') {
                parametri = (parametri.substring(1, parametri.length() - 1));
                String[] parametriArr;
                if (parametri.length() == 0)
                    parametriArr = new String[0];
                else
                    parametriArr = parametri.split(",");
                parameters = new String[parametriArr.length];
                for (int i = 0; i < parametriArr.length; i++) {
                    //elimino gli spazi iniziali e finali
                    parametriArr[i] = purge(parametriArr[i]);
                    //controllo che i nomi dei parametri contengano solo lettere numeri o underscore
                    String nowP = parametriArr[i].replaceAll("\\w", "");
                    if (nowP.length() != 0)
                        throw new TranslationError("Parameter name not permitted: \n" + parametriArr[i] +
                                " \n method: " + nomeMetodo + "\non line: " + methodInit);
                    //controllo l'unicità del nome
                    for (int j = 0; j < i; j++)
                        if (parameters[j].equals(parametriArr[i]))
                            throw new TranslationError("Parameter name already used :\n" +
                                    parameters[j] + "\nmethod :" + nomeMetodo + "\non line" + methodInit);

                    parameters[i] = parametriArr[i];
                }
            } else
                throw new TranslationError("Unknown instrunction:\n " + direttiva + "\non line: " + methodInit);
        }
	/*
	  FINE DECODIFICA FIRMA DEL METODO
	*/
	/*
	  DECODIFICA VARIABILI LOCALI, SE ESISTENTI
	*/
	    int var_line = 1;
        String direttivaVar = _instrunctions[var_line];
        if (direttivaVar.trim().isEmpty()) {
            for (var_line = 2; var_line < _instrunctions.length; var_line++) {
                if (!_instrunctions[var_line].trim().isEmpty()) {
                    direttivaVar = _instrunctions[var_line];
                    break;
                }
            }
        }
        int endVarIndex = 1;
        ListDL localVars = new ListDL();
        if (direttivaVar.contains(".var")) {
            //controllo che la direttiva trovata sia corretta
            if (!direttivaVar.replace(" ", "").equals(".var"))
                throw new TranslationError("Unknown instrution:\n" + direttiva +
                        "\nmethod: " + nomeMetodo + "\non line: " + (methodInit + 1));
            //esamino tutte le variabili finchè non trovo .end-var
            int i = var_line+1;
            try {
                while (!_instrunctions[i].contains(".end-var")) {
                    //elimino spazi iniziali e spazi finali
                    _instrunctions[i] = purge(_instrunctions[i]);
                    //controllo che la variabile abbia un nome corretto
                    String nowP = _instrunctions[i].replaceAll("\\w", "");
                    if (nowP.length() != 0)
                        throw new TranslationError("Variable name not permitted:\n" +
                                _instrunctions[i] +
                                "\nmethod: " + nomeMetodo +
                                "\non line: " + (methodInit + i));
                    //controllo tra variabili e parametri l'unicità del nome
                    for (int j = 0; j < parameters.length; j++)
                        if (parameters[j].equals(_instrunctions[i]))
                            throw new TranslationError("Variable name already used for a parameter:\n " +
                                    parameters[j] + " \n method: " + nomeMetodo +
                                    "\n on line " + (methodInit + i));
                    localVars.rewind();
                    while (localVars.hasNext())
                        if (localVars.next().equals(_instrunctions[i]))
                            throw new TranslationError("Variable already defined:\n" +
                                    _instrunctions[i] + " \nmethod: " + nomeMetodo +
                                    "\non line " + (methodInit + i));
                    if (!_instrunctions[i].isEmpty())
                        localVars.insertTail(_instrunctions[i]);
                    endVarIndex = i++;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new TranslationError(".end-var expected in method " + nomeMetodo);
            }
            if (!_instrunctions[++endVarIndex].replace(" ", "").equals(".end-var"))
                throw new TranslationError("Unknown instrution " + _instrunctions[endVarIndex] +
                        "\nmethod: " + nomeMetodo +
                        "\non line " + (methodInit + endVarIndex));
        } else
            endVarIndex = 0;
        Method theMethod = new Method(nomeMetodo, parameters.length + 1, localVars.getSize(), 0);
        constantsTable.insertTail(new Constant(theMethod.getName(), -1));
        //inserisco tutti i simboli finora trovati
        int nowOffset = 1;
        // i parametri
        for (int i = 0; i < parameters.length; i++)
            theMethod.insertSimbolo(new Simbolo(parameters[i], nowOffset++));
        //le variabili locali
        localVars.rewind();
        while (localVars.hasNext())
            theMethod.insertSimbolo(new Simbolo((String) localVars.next(), nowOffset++));
        int i = endVarIndex + 1;
        int lastByte = 4;
        //calcolo tutti gli indirizzi del metodo
        try {
            while (!_instrunctions[i].contains(".end-method")) {
                _instrunctions[i] = purge(_instrunctions[i]);
                if (_instrunctions[i].contains(".var"))
                    throw new TranslationError("Unexpected .var \n" +
                            "method: " + nomeMetodo + "\non line: " + (methodInit + i));
                //controllo la presenza di un etichetta
                if (_instrunctions[i].contains(":")) {
                    String label = _instrunctions[i].split(":")[0];
                    //elimino gli spazi iniziali e finali
                    label = purge(label);
                    //controllo che l'etichetta non contenga caratteri non permessi
                    String labelPars = label.replaceAll("\\w", "");
                    if (labelPars.length() != 0)
                        throw new TranslationError("Label name not permitted:\n" + label +
                                "\nmethod: " + nomeMetodo +
                                "\non line: " + (methodInit + i));
                    _instrunctions[i] = _instrunctions[i].substring(_instrunctions[i].indexOf(':') + 1);
                    _instrunctions[i] = purge(_instrunctions[i]);
                    //cerco un'etichetta con lo stesso nome
                    ListDL labelList = theMethod.getLabels();
                    labelList.rewind();
                    while (labelList.hasNext())
                        if (((Label) labelList.next()).getName().equals(label))
                            throw new TranslationError("Label already defined:\n" + label +
                                    "\nmethod: " +
                                    "\non line " + (methodInit + i));
                    theMethod.insertLabel(new Label(label, lastByte));
                }
                //cerco l'opcode tra quelli che sono presenti nella lista
                String[] opcodeLine = _instrunctions[i].split(" ");
                String opcodeName = opcodeLine[0];
                if (opcodeName.isEmpty()) {
                    i++;
                    continue;
                }
                Opcode now = getOpcode(opcodeName);
                if (now == null) {
                    //provo a cercare l'opcode formato da due parole
                    if (opcodeLine.length == 1)
                        throw new TranslationError("Opcode not found:\n" + opcodeName +
                                "\nmethod: " + nomeMetodo +
                                "\non line: " + (methodInit + i));
                    else {
                        purge(opcodeName);
                        String opcodeNS = opcodeLine[1];
                        now = getOpcode(opcodeName + " " + opcodeNS);
                        if (now == null)
                            throw new TranslationError("Opcode not found:\n" + opcodeName +
                                    "\nmethod: " + nomeMetodo +
                                    "\non line " + (methodInit + i));
                    }
                }

                int length = calculateLength(now);
                //controllo il numero di operandi presenti nella riga
                if (!now.isLarge() && opcodeLine.length - 1 != now.getParametersNumber())
                    throw new TranslationError("The opcode " + opcodeName + " has " +
                            (now.getParametersNumber()) + " operands \n" +
                            "method: " + theMethod.getName() + "\non line " + (methodInit + i));
                if (now.isLarge() && opcodeLine.length - 2 != now.getParametersNumber())
                    throw new TranslationError("The opcode " + now.getOpcode() + " has " +
                            (now.getParametersNumber()) + " operands \n" +
                            "method: " + theMethod.getName() + "\non line " + (methodInit + i));
                ListDL parametersInstr = new ListDL();
                //giro tutti gli operandi alla ricerca di simboli da inserire nella tabella dei simboli
                for (int j = 0; j < now.getParametersNumber(); j++) {
                    Parametro nowP = now.getParametro(j);
                    ListDL simboli = theMethod.getSimboli();
                    boolean isHex = false;
                    switch (nowP.getTipo()) {

                        //caso parametro diretto
                        case 0:
                            try {
                                String param = opcodeLine[j + (now.isLarge() ? 2 : 1)];
                                if (param.startsWith("0x")) {
                                    String hex = param.split("0x")[1];
                                    BigInteger bi = new BigInteger(hex, 16);
                                    isHex = true;
                                } else {
                                    Integer.parseInt(param);
                                    isHex = false;
                                }
                            } catch (NumberFormatException e) {
                                throw new TranslationError(opcodeLine[j + (now.isLarge() ? 2 : 1)] +
                                        " is NaN\nmethod: " + nomeMetodo + "\non line: " + (methodInit + i));
                            } catch (IndexOutOfBoundsException e) {
                                throw new TranslationError("Invalid Hex number on line: " + (methodInit + i));
                            }
                            break;
                        //caso costante inesistente
                        case 2:
                            //caso variabile
                            simboli = theMethod.getSimboli();
                            boolean trovato = false;
                            simboli.rewind();
                            while (simboli.hasNext())
                                if (((Simbolo) simboli.next()).getName().equals(opcodeLine[j + (now.isLarge() ? 2 : 1)]))
                                    trovato = true;
                            if (!trovato)
                                throw new TranslationError("Variable not found " + opcodeLine[j + (now.isLarge() ? 2 : 1)] +
                                        "\nmethod: " + nomeMetodo +
                                        "\non line: " + (methodInit + i));
                            break;
                        case 3:
                            //caso etichetta
                            simboli = theMethod.getSimboli();
                            trovato = false;
                            simboli.rewind();
                            while (simboli.hasNext())
                                if (((Simbolo) simboli.next()).getName().equals(opcodeLine[j + (now.isLarge() ? 2 : 1)]))
                                    trovato = true;
                            if (!trovato)
                                theMethod.insertSimbolo(new Simbolo(opcodeLine[j + (now.isLarge() ? 2 : 1)], -1));
                            //-1 sta per non risolto
                            break;
                    }
                    if (isHex) {
                        String hex = (opcodeLine[j + (now.isLarge() ? 2 : 1)]).split("0x")[1];
                        BigInteger bi = new BigInteger(hex, 16);
                        parametersInstr.insertTail(bi.toString());
                    } else {
                        parametersInstr.insertTail(opcodeLine[j + (now.isLarge() ? 2 : 1)]);
                    }
                }
                //inserisco l'istruzione con gli indirizzi risolti nel method
                theMethod.insertInstruction(new FirstStepInstr(lastByte, length, now, parametersInstr));
                lastByte += length;
                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TranslationError(".end-method expected in method " + nomeMetodo);
        }
        return theMethod;
    }//fine del primo passo

    //elimina gli spazi iniziali e finali dalla string in input
    public String purge(String _toPurge) {
        int j = 0;
        if (_toPurge.length() == 0)
            return "";
        while (j < _toPurge.length() && _toPurge.charAt(j++) == ' ')
            _toPurge = _toPurge.substring(1);
        j = _toPurge.length() - 1;
        while (j >= 0 && _toPurge.charAt(j--) == ' ')
            _toPurge = _toPurge.substring(0, _toPurge.length() - 1);
        return _toPurge;
    }

    public void secondStep(Method _toDecode, int methodInit) {
        ListDL instrunctions = _toDecode.getInstrunctions();
        instrunctions.rewind();
        //estraggo la firma del metodo
        //creo l'head del metodo
        //i parametri
        Binary8[] parametersArr = toBytes(_toDecode.getParameters(), 2);
        //le variabili locali
        Binary8[] variableArr = toBytes(_toDecode.getLocalVars(), 2);
        Binary8[] first4Bytes =
                {
                        parametersArr[1], parametersArr[0], variableArr[1], variableArr[0]
                };
        _toDecode.setFirst4Bytes(first4Bytes);
        while (instrunctions.hasNext()) {
            FirstStepInstr now = (FirstStepInstr) instrunctions.next();
            Opcode nowOp = now.getOpcode();
            int opcodeVal = nowOp.getValue();
            //se l'opcode è largo
            int opcodeVal1 = 0;
            if (nowOp.isLarge())
                opcodeVal1 = nowOp.getValue1();
            ListDL parameters = now.getParameters();
            parameters.rewind();
            BinIstr nowB = new BinIstr();
            nowB.insertByte(toBytes(nowOp.getValue(), 1)[0]);
            if (nowOp.isLarge())
                nowB.insertByte(toBytes(nowOp.getValue1(), 1)[0]);
            ListDL translatedPar = new ListDL();
            ListDL sygns = _toDecode.getSimboli();
            boolean solved = true;
            String[] toSolve = new String[1];
            int[] positions = new int[1];
            int[] unSBytes = new int[1];
            int index = 0;
            for (int i = 0; i < nowOp.getParametersNumber(); i++) {

                String parameterName = (String) parameters.next();
                int bytes = nowOp.getParametro(i).getBytes();
                if (nowOp.getParametro(i).getTipo() != 1) {
                    switch (nowOp.getParametro(i).getTipo()) {
                        case 0:
                            //controllo il valore passato e successivamente lo traduco in binario
                            int maxValue = (int) Math.pow(2, (bytes * 8) - 1) - 1;
                            int minValue = -(int) Math.pow(2, (bytes * 8) - 1);
                            int value = Integer.parseInt(parameterName);
                            if (value > maxValue || value < minValue)
                                throw new TranslationError("Parameter " + value + " too large\n" +
                                        "method: " + _toDecode.getName() +
                                        "\non line: " + (methodInit + i));
                            Binary8[] converted = toBytes(value, bytes);
                            for (int j = 0; j < converted.length; j++)
                                nowB.insertByte(converted[j]);
                            break;
                        case 2:
                            //elaboro tutti i simboli
                            sygns.rewind();
                            int symbolValue = -1;
                            while (sygns.hasNext()) {
                                Simbolo nowS = (Simbolo) sygns.next();
                                if (nowS.getName().equals(parameterName))
                                    symbolValue = nowS.getOffset();
                            }
                            if (symbolValue == -1)
                                throw new TranslationError("Unknown symbol:\n" + symbolValue +
                                        "\nmethod: " + _toDecode.getName() +
                                        "\non line: " + (methodInit + i));
                            converted = toBytes(symbolValue, bytes);
                            for (int j = converted.length - 1; j >= 0; j--)
                                nowB.insertByte(converted[j]);
                            break;
                        case 3:
                            //elaboro le etichette
                            ListDL labels = _toDecode.getLabels();
                            labels.rewind();
                            boolean trovato = false;
                            while (labels.hasNext()) {
                                Label nowL = (Label) labels.next();
                                if (nowL.getName().equals(parameterName)) {
                                    trovato = true;
                                    int offset = nowL.getByte() - now.getFirstByte();
                                    maxValue = (int) Math.pow(2, (bytes * 8) - 1) - 1;
                                    minValue = -(int) Math.pow(2, (bytes * 8) - 1);
                                    //controllo che l'offset non sia troppo grande
                                    if (offset < minValue || offset > maxValue)
                                        throw new TranslationError("Offset too large: " + offset +
                                                "\nmethod: " + _toDecode.getName() +
                                                "\non line " + (methodInit + i));
                                    Binary8[] offsetArr = toBytes(offset, bytes);
                                    for (int j = offsetArr.length - 1; j >= 0; j--)
                                        nowB.insertByte(offsetArr[j]);
                                }
                            }
                            if (!trovato)
                                throw new TranslationError("Label not found: \n" + parameterName +
                                        "\nmethod: " + _toDecode.getName() +
                                        "\non line " + (methodInit + i));
                    }
                }
                //il caso della costante non è risolvibile in questo momento
                else {
                    solved = false;
                    if (index >= toSolve.length) {
                        String[] toSolveN = new String[toSolve.length + 1];
                        int[] positionsN = new int[toSolve.length + 1];
                        int[] unSBytesN = new int[unSBytes.length + 1];
                        for (int j = 0; j < toSolve.length; j++) {
                            toSolveN[j] = toSolve[j];
                            positionsN[j] = positions[j];
                            unSBytesN[j] = unSBytes[j];
                        }
                        toSolve = toSolveN;
                        positions = positionsN;
                        unSBytes = unSBytesN;
                    }
                    toSolve[index] = parameterName;
                    positions[index] = i;
                    unSBytes[index++] = bytes;
                }
            }
            SecondStepInstr toInsertInstr = new SecondStepInstr();
            if (!solved) {
                toInsertInstr.setSolved(false);
                toInsertInstr.setNotSolved(toSolve);
                toInsertInstr.setPositions(positions);
                toInsertInstr.setUnsolvedBytes(unSBytes);
            } else
                toInsertInstr.setSolved(true);
            toInsertInstr.setBinInstr(nowB);
            _toDecode.insertInstruction(toInsertInstr);
        }
        System.out.println("Method's head");
        Binary8[] head = _toDecode.getFirst4Bytes();
        for (int i = 0; i < head.length; i++)
            System.out.print(head[i] + " ");
        ListDL finalInstructions = _toDecode.getInstrunctionsSeconds();
        finalInstructions.rewind();
        System.out.println("\nInstrunctions:");
        System.out.println("Solved - Instrunction");
        while (finalInstructions.hasNext()) {
            SecondStepInstr now = (SecondStepInstr) finalInstructions.next();
            System.out.printf("%6.1s -%s\n", now.isSolved(), now.getBinIstr());
            if (!now.isSolved()) {
                String[] toSolve = now.getUnsolved();
                int[] positions = now.getUnsolvedPositions();
                int[] unsolvedBytes = now.getUnsolvedBytes();
                System.out.println("\tArguments unsolved: \n\tName, position, bytes");
                for (int i = 0; i < toSolve.length; i++)
                    System.out.printf("\t%s, %d, %d\n", toSolve[i], positions[i], unsolvedBytes[i]);
            }

        }
        if (exist(_toDecode))
            throw new TranslationError("Method already defined " + _toDecode.getName());
        programModule.insertTail(_toDecode);
    }

    //effettua il linkaggio di tutti i metodi
    public void linkage() {
        programModule.rewind();
        decoded.rewind();
        //effettuo il calcolo dei primi byte dei metodi
        int now = 3;
        //controllo che tutte le costanti siano uniche
        constantsTable.rewind();
        while (constantsTable.hasNext()) {
            Constant nowC = (Constant) constantsTable.next();
            constantsTable.saveNow();
            while (constantsTable.hasNext()) {
                Constant succ = (Constant) constantsTable.next();
                if (nowC.getName().equals(succ.getName()))
                    throw new TranslationError("Constant " + nowC.getName() + " already defined ");
            }
            constantsTable.restoreNow();
        }

        while (programModule.hasNext()) {
            Method nowM = (Method) programModule.next();
            //aggiorno la costante
            constantsTable.rewind();
            while (constantsTable.hasNext()) {
                Constant nowC = (Constant) constantsTable.next();
                if (nowC.getName().equals(nowM.getName())) {
                    //la method area di ijvm, almeno per la mia implementazione comincia da 2^18
                    nowC.setValue(now + ((int) Math.pow(2, 16) * 4));
                }
            }
            now += nowM.getSize();
        }
        //risolvo tutte le istruzioni non risolte presenti nei metodi
        programModule.rewind();
        System.out.println("----Solving all remaining instrunctions----");
        while (programModule.hasNext()) {
            Method nowM = (Method) programModule.next();
            //aggiorno la costante
            constantsTable.rewind();
            ListDL secondStep = nowM.getInstrunctionsSeconds();
            secondStep.rewind();
            while (secondStep.hasNext()) {
                //controllo che l'istruzione sia risolta
                SecondStepInstr nowStep = (SecondStepInstr) secondStep.next();
                if (!nowStep.isSolved()) {
                    //estraggo gli argomenti non risolti
                    String[] notSolved = nowStep.getUnsolved();
                    int[] positions = nowStep.getUnsolvedPositions();
                    int[] bytes = nowStep.getUnsolvedBytes();
                    BinIstr nowBin = nowStep.getBinIstr();
                    for (int i = 0; i < notSolved.length; i++) {
                        //cerco tra le costanti
                        constantsTable.rewind();
                        boolean trovato = false;
                        int index = 0;
                        while (constantsTable.hasNext()) {
                            Constant nowC = (Constant) constantsTable.next();
                            if (nowC.getName().equals(notSolved[i])) {
                                trovato = true;
                                break;
                            }
                            index++;
                        }
                        if (!trovato)
                            throw new TranslationError("Constant not found: " + notSolved[i] + " in method " + nowM.getName());
                        Binary8[] result = toBytes(index, bytes[i]);
                        for (int j = result.length - 1; j >= 0; j--)
                            nowBin.insertByte(result[j], positions[i]++);
                    }
                    System.out.println(nowBin + " method " + nowM.getName());
                }
            }
        }
        System.out.println("---Final object code---");
        //scrivo in decoded le istruzioni in linguaggio macchina trovate
        programModule.rewind();
        while (programModule.hasNext()) {
            Method nowM = (Method) programModule.next();
            Binary8[] head = nowM.getFirst4Bytes();
            for (int i = 0; i < head.length; i++)
                decoded.insertTail(head[i]);
            ListDL secondStep = nowM.getInstrunctionsSeconds();
            secondStep.rewind();
            while (secondStep.hasNext()) {
                SecondStepInstr nowS = (SecondStepInstr) secondStep.next();
                ListDL nowBin = nowS.getBinIstr().getBytes();
                nowBin.rewind();
                while (nowBin.hasNext())
                    decoded.insertTail(nowBin.next());
            }
        }
        decoded.rewind();
        int index = 1;
        while (decoded.hasNext())
            System.out.printf("%s%s", decoded.next(), (index++ % 4) == 0 ? "\n" : " ");
    }

    //restituisce true se e solo se il method passata come parametro è presente nella lista dei metodi
    public boolean exist(Method _toSearch) {
        programModule.rewind();
        while (programModule.hasNext())
            if (((Method) programModule.next()).equals(_toSearch))
                return true;
        return false;
    }

    //restituisce un array di stringhe da otto caratteri
    public Binary8[] toBytes(int toConvert, int totBytes) {
        Binary32 converted = new Binary32(toConvert);
        Binary8[] toReturn = new Binary8[totBytes];
        //il totale dei bit è 32
        for (int i = toReturn.length - 1; i >= 0; i--) {
            toReturn[i] = converted.getByte(3 - i);
        }
        return toReturn;
    }

    //restitusce la lunghezza dell'opcode
    private int calculateLength(Opcode _toCalc) {
        int length = 1;
        if (_toCalc.isLarge())
            length = 2;
        //calcolo lunghezza in byte dell'istruzione
        for (int i = 0; i < _toCalc.getParametersNumber(); i++)
            length += _toCalc.getParametro(i).getBytes();
        return length;
    }

    //restituisce l'opcode desiderato, null in caso di opcode non trovato
    public Opcode getOpcode(String name) {
        referencesTable.rewind();
        while (referencesTable.hasNext()) {
            Opcode now = (Opcode) referencesTable.next();
            if (now.getOpcode().equals(name))
                return now;
        }
        return null;
    }

    //questo metodo permette di inserire in tableReference un nuovo OPCODE con il corrispondente valore esadecimale e
    //numero di parametri
    public Opcode insertOPCODE(String opcode, int value, int parameters) {
        //scorro tutta la lista alla ricerca di opcode e/o value
        referencesTable.rewind();
        while (referencesTable.hasNext()) {
            Opcode now = (Opcode) referencesTable.next();
            if (now.getOpcode().equals(opcode))
                throw new InsertException("Opcode: " + opcode + " already exist");
            if (now.getValue() == value)
                throw new InsertException("Valore: " + value + " alredy assigned to " + now.getOpcode());
        }
        //inserisco il nuovo opcode nella lista
        Opcode toReturn = new Opcode(opcode, value, parameters);
        referencesTable.insertTail(toReturn);
        return toReturn;
    }

    public void setOpcode(Opcode toSet) {
        referencesTable.rewind();
        while (referencesTable.hasNext()) {
            Opcode now = (Opcode) referencesTable.next();
            if (now.equals(toSet))
                referencesTable.setNow(toSet);
            return;
        }
    }

    //restituisce una lista contentente le istruzioni tradotte suddivise in parole da 4 byte
    public ListDL getDecoded() {
        ListDL toReturn = new ListDL();
        //la prima istruzione che deve eseguire la macchina sarà INVOKEVIRTUAL main
        constantsTable.rewind();
        int index = 0;
        while (constantsTable.hasNext()) {
            Constant now = (Constant) constantsTable.next();
            if (now.getName().equals("main")) {
                Binary8[] mainAddr = toBytes(index, 2);
                for (int i = 0; i < mainAddr.length; i++)
                    decoded.insertHead(mainAddr[i]);
                break;
            }
            index++;
        }
        boolean[] invokeB = {true, false, true, true, false, true, true, false};
        decoded.insertHead(new Binary8(invokeB));
        decoded.rewind();
        while (decoded.hasNext()) {
            boolean[] bin32 = new boolean[32];
            for (int i = 0; i < 4 && decoded.hasNext(); i++) {
                boolean[] bin8 = ((Binary8) decoded.next()).getValue();
                for (int j = 0; j < bin8.length; j++)
                    bin32[(i * 8) + j] = bin8[j];
            }
            toReturn.insertTail(new Binary32(bin32));
        }
        return toReturn;
    }

    //restituisce la tabella delle costanti
    public ListDL getConstants() {
        return constantsTable;
    }

    public int getMainLastByte() {
        return mainLastByte;
    }

    public void saveOpcodeList() {
        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream("./Assembler/opcode.lst"));
            referencesTable.rewind();
            while (referencesTable.hasNext())
                output.writeObject(referencesTable.next());
            output.close();
        } catch (IOException e) {
            throw new SystemError("I/O error, the file may be corrupt");
        }
    }

    //restituisce un array contenente gli opcode riconosciuti
    public Opcode[] getOpcodeList() {
        Opcode[] toReturn = new Opcode[referencesTable.getSize()];
        referencesTable.rewind();
        int i = 0;
        while (referencesTable.hasNext())
            toReturn[i++] = (Opcode) referencesTable.next();
        return toReturn;
    }

    public void setOpcodeList(Opcode[] toSet) {
        referencesTable = new ListDL();
        for (int i = 0; i < toSet.length; i++)
            referencesTable.insertTail(toSet[i]);
    }

}

//il risultato del primo step
class FirstStepInstr {
    private int firstByte;
    private int length;
    private Opcode opcode;
    private ListDL parameters;

    public FirstStepInstr(int _firstByte, int _length, Opcode _opcode, ListDL _parameters) {
        firstByte = _firstByte;
        length = _length;
        opcode = _opcode;
        parameters = _parameters;
    }

    public int getFirstByte() {
        return firstByte;
    }

    public int getLength() {
        return length;
    }

    public int getNextByte() {
        return firstByte + length;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public void insertParameter(String parameterName) {
        parameters.insertTail(parameterName);
    }

    public ListDL getParameters() {
        return parameters;
    }
}

//il risultato del secondo step
class SecondStepInstr {
    private boolean solved;
    private BinIstr instrunction;
    private String[] notSolved;
    private int[] positions;
    private int[] bytes;

    public void setNotSolved(String[] _notSolved) {
        notSolved = _notSolved;
    }

    public void setPositions(int[] _positions) {
        positions = _positions;
    }

    public void setBinInstr(BinIstr _binInstr) {
        instrunction = _binInstr;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean _solved) {
        solved = _solved;
    }

    public BinIstr getBinIstr() {
        return instrunction;
    }

    public String[] getUnsolved() {
        return notSolved;
    }

    public int[] getUnsolvedPositions() {
        return positions;
    }

    public int[] getUnsolvedBytes() {
        return bytes;
    }

    public void setUnsolvedBytes(int[] unsBytes) {
        bytes = unsBytes;
    }


}


class Method {
    private String name;
    private int parameters;
    private int localVars;
    private ListDL instrunctions;
    private ListDL instrunctionsSecond;
    private ListDL labels;
    private ListDL simboli;
    private int firstByte;
    private String[] istruzioni;
    private Binary8[] first4Bytes;
    private ListDL binIstr;
    private int size;

    public Method(String _name, int _parameters, int _localVars, int _firstByte) {
        name = _name;
        parameters = _parameters;
        localVars = _localVars;
        instrunctions = new ListDL();
        labels = new ListDL();
        simboli = new ListDL();
        binIstr = new ListDL();
        firstByte = _firstByte;
        instrunctionsSecond = new ListDL();
        size = 4;
    }

    public String getName() {
        return name;
    }

    public int getParameters() {
        return parameters;
    }

    public int getLocalVars() {
        return localVars;
    }

    public ListDL getInstrunctions() {
        return instrunctions;
    }

    public ListDL getInstrunctionsSeconds() {
        return instrunctionsSecond;
    }

    public ListDL getSimboli() {
        return simboli;
    }

    public ListDL getLabels() {
        return labels;
    }

    public Binary8[] getFirst4Bytes() {
        return first4Bytes;
    }

    public void setFirst4Bytes(Binary8[] bytes) {
        first4Bytes = bytes;
    }

    public void insertSimbolo(Simbolo simbolo) {
        simboli.insertTail(simbolo);
    }

    public void insertInstruction(FirstStepInstr instrunction) {
        instrunctions.insertTail(instrunction);
        size += instrunction.getLength();
    }

    public void insertInstruction(SecondStepInstr instrunction) {
        instrunctionsSecond.insertTail(instrunction);
    }

    public void insertLabel(Label toInsert) {
        labels.insertTail(toInsert);
    }

    public boolean equals(Method toCompare) {
        return name.equals(toCompare.getName());
    }

    public int getSize() {
        return size;
    }

}

class BinIstr {
    ListDL bytes;

    public BinIstr() {
        bytes = new ListDL();
    }

    public void insertByte(Binary8 _byte) {
        bytes.insertTail(_byte);
    }

    public void insertByte(Binary8 _byte, int index) {
        bytes.rewind();
        int i = 0;
        while (bytes.hasNext() && i++ <= index)
            bytes.next();
        bytes.insertNext(_byte);
    }

    public ListDL getBytes() {
        return bytes;
    }

    public String toString() {
        String toReturn = "";
        bytes.rewind();
        while (bytes.hasNext())
            toReturn += " " + bytes.next();
        return toReturn;
    }

}

class Simbolo {
    String name;
    int offset;

    public Simbolo(String _name, int _offset) {
        name = _name;
        offset = _offset;
    }

    public String getName() {
        return name;
    }

    public int getOffset() {
        return offset;
    }
}

class Label {
    String name;
    int byteN;

    public Label(String _name, int _byteN) {
        name = _name;
        byteN = _byteN;
    }

    public int getByte() {
        return byteN;
    }

    public String getName() {
        return name;
    }
}