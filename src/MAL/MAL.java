package MAL;

import ADT.ListLS.EmptyListException;
import ADT.ListLS.ListLS;
import ADT.ListLS.NoNextException;
import ADT.Queue.Queue;
import MIC1.Components.controlStore;

public class MAL {
    //VARIABLES DECLARATION

    //mic-1 control store
    //private controlStore myCStore;
    //all the program blocks,a block is made by a label and an instruction sequence
    private ListLS blocchi;
    //here it stores the first instruction address of each block.
    private ListLS prime;
    //here it stores the last instructioin address of each block.
    private Queue ultime;
    //here inserts address of instructions of which it can't know address of the next instruction a priori.
    private ListLS unResolved;
    //here inserts all the ifs blocks and the corresponding elses ones.
    private ListLS ifsAndElses;
    //here inserts all the instruction that will be write in the control store
    private ListLS finalInstructions;
    //the last instruction, where computation maybe halt.
    private Istruzione lastInstruction;
    //boolean variable that becomes true if the traduced algorithm may create an infinite loop.
    private boolean warning;
    //conserva le istruzioni con indirizzo riservato
    private ListLS reserved;

    public MAL()//controlStore _myCStore)
    {
        //myCStore = _myCStore;
        blocchi = new ListLS();
        prime = new ListLS();
        ultime = new Queue();
        ifsAndElses = new ListLS();
        finalInstructions = new ListLS();
        unResolved = new ListLS();
        warning = false;
        reserved = new ListLS();
    }

    public static void main(String[] args) {
        String[] istruzioni =
                {
                        "TOS=1;if(N)goto et1; else goto et2",
                        "et1:TOS=1",
                        "(0x50)TOS=0",
                        "et2: OPC=1",
                        "(0x51) OPC=OPC+1",
                        "(0x51) OPC=OPC+2",
                        "TOS=TOS+H"
                };
        MAL myMal = new MAL();//new controlStore());
        myMal.translate(istruzioni);
    }

    public String[] removeComments(String[] _instructions) {
        for (int i = 0; i < _instructions.length; i++)
            if (_instructions[i] != null && _instructions[i].contains("//"))
                _instructions[i] = _instructions[i].substring(0, _instructions[i].indexOf("/"));
        return _instructions;
    }

    public void decodificaIndirizzi(String[] _instructions) {
        //divides instructions in blocks defined by labels, and erase all the spaces.
        for (int i = 0; i < _instructions.length; i++) {
            //a first check for J invalid instructions
            String toDecode = _instructions[i];
            String[] toDecodeSplitted = toDecode.split(";");
            for (int j = 0; j < toDecodeSplitted.length; j++)
                if (toDecodeSplitted[j].replace(" ", "").equals("J") ||
                        toDecodeSplitted[j].replace(" ", "").equals("N") ||
                        toDecodeSplitted[j].replace(" ", "").equals("Z"))
                    throw new ErroreDiCompilazione("Unknow instruction: \n" + toDecode + "\n on line " + i + 1);
            dividi(toDecode, i + 1);
        }
        //riserva gli indirizzi
        searchReserved();
        //finds all the occurrency of ifs and elses and catalogue them into appropriate lists.
        ifElseCatalogue();
        //searchs in the blocks lists the block with %noet label, it will be its main.
        boolean trovato = false;
        blocchi.rewind();
        while (blocchi.hasNext()) {
            Blocco now = (Blocco) blocchi.next();
            if (now.getLabel().equals("%noet")) {
                trovato = true;
                break;
            }
        }
        int index = 0;
        //initialize a variable called index, it will be the counter of instruction's addressess. It starts from the first.
        //free instruction, because first n positions are destinated at all elses labels.
        //inizializzo index
        for (int i = 0; i < ifsAndElses.getSize(); i++) {
            while (isReserved(index) || isReserved(index + 256))
                index++;
            index++;
        }
        if (trovato) {
            Blocco first = (Blocco) blocchi.getNow();
            ListLS blockInstructions = first.getBlocco();
            boolean prima = true;
            blockInstructions.rewind();
            first.setTradotto(true);
            while (blockInstructions.hasNext()) {
                Istruzione nowInstr = (Istruzione) blockInstructions.next();
                //execute a check on index, to avoid that an instruction may be assigned to a firstInstruction
                //(in the if block) reserved address.
                if (index >= 256 && index < 256 + ifsAndElses.getSize())
                    index = 256 + ifsAndElses.getSize();
                while (isReserved(index))
                    index++;
                blockInstructions.saveNow();
                int successivo = -1;
                if (blockInstructions.hasNext())
                    successivo = checkAddress((Istruzione) blockInstructions.next());
                blockInstructions.restoreNow();
                int ora = checkAddress(nowInstr);
                if (prima) {
                    prime.insertHead(new Flag(ora != -1 ? ora : index, "%noet"));
                    prima = false;
                }
                //check if this instruction has a goto inside.
                String hasGoto = hasGoto(nowInstr);
                //check if this instruction has a conditioned branch.
                String hasElse = hasElse(nowInstr);
                if (hasGoto != null && hasElse != null)
                    throw new ErroreDiCompilazione("Unknow instruction: \n" +
                            nowInstr.getInstruction() + "\non line" +
                            nowInstr.getNumber());
                //if instruction has a branch it inserts it ina gotos and keep the address to -1 value.
                if (hasGoto != null || hasElse != null) {
                    if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                        unResolved.insertHead(new Flag(index, hasGoto == null ? hasElse : hasGoto));
                        nowInstr.setNextAddress(-1);
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                        finalInstructions.insertTail(nowInstr);
                    } else {
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                        finalInstructions.insertTail(nowInstr);
                    }
                }
                //if instruction is the last keeps null address and takes index in the latest instruction
                else if (!blockInstructions.hasNext()) {
                    ultime.enqueue(new Flag(ora != -1 ? ora : index, first.getLabel()));
                    nowInstr.setNextAddress(-1);
                    nowInstr.setAddress(ora != -1 ? ora : index++);
                    finalInstructions.insertTail(nowInstr);
                }
                //exactly, the case in which next instruction can be the next. il caso in cui l"istruzione successiva
                //sia quella successiva, appunto
                else {
                    nowInstr.setAddress(ora != -1 ? ora : index++);
                    if (index + 1 >= 256 && index + 1 < 256 + ifsAndElses.getSize()) {
                        while (isReserved(index + 1))
                            index++;
                        nowInstr.setNextAddress(successivo != -1 ? successivo : index + 1);
                    } else {
                        while (isReserved(index))
                            index++;
                        nowInstr.setNextAddress(successivo != -1 ? successivo : index);
                    }
                    finalInstructions.insertTail(nowInstr);
                }
            }
        }
        //inizializzo elseCounter
        int elseCounter = 0;
        while (isReserved(elseCounter) || isReserved(elseCounter + 256))
            elseCounter++;
        //now it begins to traduce elses blocks.
        blocchi.rewind();
        while (blocchi.hasNext()) {
            //search between IF_ELSE the nowBlock block, if is an else traduces it.
            Blocco nowBlock = (Blocco) blocchi.next();
            //if extracts the %noet block it will directly go to the next
            if (nowBlock.getLabel().equals("%noet")) {
                if (blocchi.hasNext())
                    nowBlock = (Blocco) blocchi.next();
                else
                    break;
            }
            //search the now block beetwen the elses, when found it's traduced.
            ifsAndElses.rewind();
            boolean traduci = false;
            //try to know if nowblock is an else
            while (ifsAndElses.hasNext()) {
                if (nowBlock.getLabel().equals((((IF_ELSE) ifsAndElses.next()).getElse()).getLabel())) {
                    traduci = true;
                    break;
                }

            }
            if (traduci) {
                boolean prima = true;
                ListLS nowBlockInstructions = nowBlock.getBlocco();
                nowBlockInstructions.rewind();
                nowBlock.setTradotto(true);
                while (nowBlockInstructions.hasNext()) {
                    //execute a check of index, to avoid that an instruction can be assigned to a firstInstruction
                    //(in the if block) reserved address.
                    Istruzione nowInstr = (Istruzione) (nowBlockInstructions.next());
                    if (index >= 256 && index < 256 + ifsAndElses.getSize())
                        index = 256 + ifsAndElses.getSize();
                    while (isReserved(index))
                        index++;
                    nowBlockInstructions.saveNow();
                    int successivo = -1;
                    if (nowBlockInstructions.hasNext())
                        successivo = checkAddress((Istruzione) nowBlockInstructions.next());
                    nowBlockInstructions.restoreNow();
                    int ora = checkAddress(nowInstr);
                    //first instruction has a special cure.
                    if (prima) {
                        //calculate address of first and reserves it for the if block.
                        prima = false;
                        prime.insertHead(new Flag(ora == -1 ? elseCounter : ora, nowBlock.getLabel()));
                        //check if this instruction has a goto inside.
                        String hasGoto = hasGoto(nowInstr);
                        //check if this instruction has a conditioned branch.
                        String hasElse = hasElse(nowInstr);
                        if (hasGoto != null && hasElse != null)
                            throw new ErroreDiCompilazione("Unknow instruction: \n" +
                                    nowInstr.getInstruction() + "\n on line: " +
                                    nowInstr.getNumber());
                        //if instruction has a branch inserts it in gotos and keeps address to -1 value.
                        if (hasGoto != null || hasElse != null) {
                            if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                                unResolved.insertHead(new Flag(elseCounter, hasGoto == null ? hasElse : hasGoto));
                                nowInstr.setAddress(ora == -1 ? elseCounter++ : ora);
                                nowInstr.setNextAddress(-1);
                                finalInstructions.insertTail(nowInstr);
                            } else {
                                nowInstr.setAddress(ora == -1 ? elseCounter++ : ora);
                                finalInstructions.insertTail(nowInstr);
                            }
                        }
                        //if instruction is the last keeps address to null and put counter to the latest.
                        else if (!nowBlockInstructions.hasNext()) {
                            ultime.enqueue(new Flag(ora == -1 ? elseCounter : ora, nowBlock.getLabel()));
                            nowInstr.setNextAddress(-1);
                            nowInstr.setAddress(ora == -1 ? elseCounter++ : ora);
                            finalInstructions.insertTail(nowInstr);
                        } else {
                            nowInstr.setNextAddress(successivo == -1 ? index : successivo);
                            nowInstr.setAddress(ora == -1 ? elseCounter++ : ora);
                            finalInstructions.insertTail(nowInstr);
                        }
                    } else {
                        //check if this instruction has goto inside.
                        String hasGoto = hasGoto(nowInstr);
                        //check if this instruction has a conditioned branch.
                        String hasElse = hasElse(nowInstr);
                        if (hasGoto != null && hasElse != null)
                            throw new ErroreDiCompilazione("Unknow instruction: \n" + nowInstr.getInstruction() + "on line: "
                                    + nowInstr.getNumber());
                        //if instruction has a branch inserts it in gotos and keeps address to -1 value.
                        if (hasGoto != null || hasElse != null) {
                            if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                                unResolved.insertHead(new Flag(ora == -1 ? index : ora, hasGoto == null ? hasElse : hasGoto));
                                nowInstr.setNextAddress(-1);
                                nowInstr.setAddress(ora == -1 ? index++ : ora);
                                finalInstructions.insertTail(nowInstr);
                            } else {
                                nowInstr.setAddress(ora == -1 ? index++ : ora);
                                finalInstructions.insertTail(nowInstr);
                            }
                        }
                        //if it's the last instruction keep address to null value and put counter in latest.
                        else if (!nowBlockInstructions.hasNext()) {
                            ultime.enqueue(new Flag(ora == -1 ? index : ora, nowBlock.getLabel()));
                            nowInstr.setAddress(ora == -1 ? index++ : ora);
                            nowInstr.setNextAddress(-1);
                            finalInstructions.insertTail(nowInstr);
                        } else {
                            if (successivo != -1)
                                nowInstr.setNextAddress(successivo);
                            else if (index + 1 >= 256 && index + 1 < 256 + ifsAndElses.getSize()) {
                                int succ = index + 2;
                                while (isReserved(succ))
                                    succ++;
                                nowInstr.setNextAddress(succ);
                            } else {
                                int succ = index + 1;
                                while (isReserved(succ))
                                    succ++;
                                nowInstr.setNextAddress(ora == -1 ? succ : index);
                            }
                            finalInstructions.insertTail(nowInstr);
                            nowInstr.setAddress(ora != -1 ? ora : index++);
                        }
                    }
                }
            }

        }
        //unscrambling ifs addressess.
        ifsAndElses.rewind();
        while (ifsAndElses.hasNext()) {
            IF_ELSE nowIF_ELSE = (IF_ELSE) (ifsAndElses.next());
            Blocco nowIf = nowIF_ELSE.getIf();
            Blocco nowElse = nowIF_ELSE.getElse();
            prime.rewind();
            //searches for the first instruction of the corresponding else block.
            while (prime.hasNext())
                if (((Flag) prime.next()).getLabel().equals(nowElse.getLabel()))
                    break;
            int ElseAddress = ((Flag) (prime.getNow())).getIndex();
            int ifAddress = ElseAddress + 256;
            ListLS nowIfInstructions = nowIf.getBlocco();
            //extracts first instruction of the if block and put in its positions.
            nowIfInstructions.rewind();
            Istruzione firstInstruction = (Istruzione) nowIfInstructions.next();
            //assign as address the now computed address.
            firstInstruction.setAddress(ifAddress);
            finalInstructions.insertTail(firstInstruction);
            //check if this instruction has a goto inside.
            String hasGoto = hasGoto(firstInstruction);
            //check if this instruction as a conditioned branch.
            String hasElse = hasElse(firstInstruction);
            nowIfInstructions.saveNow();
            int successivo = -1;
            if (nowIfInstructions.hasNext())
                successivo = checkAddress((Istruzione) nowIfInstructions.next());
            nowIfInstructions.restoreNow();
            if (hasGoto != null && hasElse != null)
                throw new ErroreDiCompilazione("Unknow instruction: \n" + firstInstruction.getInstruction() +
                        "\n on line :" + firstInstruction.getNumber());
            //if instruction has a branch inserts it in gotos and keeps address to -1 value.
            if (hasGoto != null || hasElse != null) {
                if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                    unResolved.insertHead(new Flag(ifAddress, hasGoto == null ? hasElse : hasGoto));
                    firstInstruction.setNextAddress(-1);
                }
            } else if (!nowIfInstructions.hasNext()) {
                ultime.enqueue(new Flag(ifAddress, nowIf.getLabel()));
                firstInstruction.setNextAddress(-1);
            } else {
                firstInstruction.setNextAddress(successivo != -1 ? successivo : index);
            }
            //codification of first instruction address and, after, insertion in firsts list. and fine codifica
            //indirizzo prima istruzione, la inserisco tra le prime
            prime.insertHead(new Flag(ifAddress, nowIf.getLabel()));
            nowIf.setTradotto(true);
            while (nowIfInstructions.hasNext()) {
                //check index, to avoid that an instruction can be assigned to a firstInstruction (in an if block)
                //reserved address.
                Istruzione nowInstr = (Istruzione) (nowIfInstructions.next());
                nowIfInstructions.saveNow();
                successivo = -1;
                if (nowIfInstructions.hasNext())
                    successivo = checkAddress((Istruzione) nowIfInstructions.next());
                nowIfInstructions.restoreNow();
                int ora = checkAddress(nowInstr);

                if (index >= 256 && index < 256 + ifsAndElses.getSize())
                    index = 256 + ifsAndElses.getSize();
                while (isReserved(index))
                    index++;
                //check if this instruction has a goto inside
                hasGoto = hasGoto(nowInstr);
                //check if this instruction has a conditioned branch.
                hasElse = hasElse(nowInstr);
                if (hasGoto != null && hasElse != null)
                    throw new ErroreDiCompilazione("Unknow instruction: " +
                            nowInstr.getInstruction() + "\n on line :" + nowInstr.getNumber());
                //if instruction has a branch inserts it in gotos and keeps address to -1 value.
                if (hasGoto != null || hasElse != null) {
                    if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                        unResolved.insertHead(new Flag(ora != -1 ? ora : index, hasGoto == null ? hasElse : hasGoto));
                        nowInstr.setNextAddress(-1);
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                        finalInstructions.insertTail(nowInstr);
                    } else {
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                        finalInstructions.insertTail(nowInstr);
                    }
                }
                //if instruction is the last it keep address to null value and place counter in latests.
                else if (!nowIfInstructions.hasNext()) {
                    ultime.enqueue(new Flag(ora != -1 ? ora : index, nowIf.getLabel()));
                    nowInstr.setNextAddress(-1);
                    nowInstr.setAddress(ora != -1 ? ora : index++);
                    finalInstructions.insertTail(nowInstr);
                } else {
                    if (successivo != -1)
                        nowInstr.setNextAddress(successivo);
                    else if (index + 1 >= 256 && index + 1 < 256 + ifsAndElses.getSize()) {
                        int succ = index + 2;
                        while (isReserved(succ))
                            succ++;
                        nowInstr.setNextAddress(succ);
                    } else {
                        int succ = index + 1;
                        while (isReserved(succ))
                            succ++;
                        nowInstr.setNextAddress(ora == -1 ? succ : index);
                    }
                    finalInstructions.insertTail(nowInstr);
                    nowInstr.setAddress(ora != -1 ? ora : index++);
                }

            }
        }
        //traduces remaining blocks.
        blocchi.rewind();
        while (blocchi.hasNext()) {
            Blocco now = (Blocco) blocchi.next();
            boolean traduci = !now.isTraduced();
            if (traduci) {
                ListLS blockInstructions = now.getBlocco();
                boolean prima = true;
                blockInstructions.rewind();
                now.setTradotto(true);
                while (blockInstructions.hasNext()) {
                    Istruzione nowInstr = (Istruzione) blockInstructions.next();
                    while (isReserved(index))
                        index++;
                    blockInstructions.saveNow();
                    int successivo = -1;
                    if (blockInstructions.hasNext())
                        successivo = checkAddress((Istruzione) blockInstructions.next());
                    blockInstructions.restoreNow();
                    int ora = checkAddress(nowInstr);
                    //check if this instruction as a goto inside.
                    String hasGoto = hasGoto(nowInstr);
                    //check if this instrction has a conditioned branch inside.
                    String hasElse = hasElse(nowInstr);
                    if (prima) {
                        prime.insertHead(new Flag(ora != -1 ? ora : index, now.getLabel()));
                        prima = false;
                    }
                    if (hasGoto != null && hasElse != null)
                        throw new ErroreDiCompilazione("Unknow instruction: " + nowInstr.getInstruction() +
                                "\n on line :" + nowInstr.getNumber());
                    //if instruction has a branch inserts it in gotos and keeps to address -1 value.
                    if (hasGoto != null || hasElse != null) {
                        if ((hasGoto != null && !hasGoto.equals("(MBR)")) || hasElse != null) {
                            unResolved.insertHead(new Flag(ora != -1 ? ora : index, hasGoto == null ? hasElse : hasGoto));
                            nowInstr.setNextAddress(-1);
                            nowInstr.setAddress(ora != -1 ? ora : index++);
                            finalInstructions.insertTail(nowInstr);
                        } else {
                            nowInstr.setAddress(ora != -1 ? ora : index++);
                            finalInstructions.insertTail(nowInstr);
                        }
                    }
                    //if instruction is the last keeps address to null value and takes counter in the latests.
                    else if (!blockInstructions.hasNext()) {
                        ultime.enqueue(new Flag(ora != -1 ? ora : index, now.getLabel()));
                        nowInstr.setNextAddress(-1);
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                        finalInstructions.insertTail(nowInstr);
                    }
                    //if next instruction is the next,exactly.
                    else {
                        if (successivo != -1)
                            nowInstr.setNextAddress(successivo);
                        else if (index + 1 >= 256 && index + 1 < 256 + ifsAndElses.getSize()) {
                            int succ = index + 2;
                            while (isReserved(succ))
                                succ++;
                            nowInstr.setNextAddress(succ);
                        } else {
                            int succ = index + 1;
                            while (isReserved(succ))
                                succ++;
                            nowInstr.setNextAddress(ora == -1 ? succ : index);
                        }
                        finalInstructions.insertTail(nowInstr);
                        nowInstr.setAddress(ora != -1 ? ora : index++);
                    }
                }
            }
        }
        //now it can assign to instructions with nextAddress = -1 a valid address.

        //begins with containing branch's instructionsinizio

        unResolved.rewind();
        while (unResolved.hasNext()) {
            Flag now = (Flag) unResolved.next();
            String blockLabel = now.getLabel();
            int instrIndex = now.getIndex();
            prime.rewind();
            //search for the first instruction of the block desidered by the instruction.
            while (prime.hasNext()) {
                Flag nowPrime = (Flag) prime.next();
                if ((now.getLabel()).equals(nowPrime.getLabel()))
                    break;
            }
            Flag flagTrovato = (Flag) prime.getNow();
            //search for the unresolved instruction.
            finalInstructions.rewind();
            while (finalInstructions.hasNext()) {
                Istruzione nowIstr = (Istruzione) finalInstructions.next();
                if (nowIstr.getAddress() == instrIndex)
                    break;
            }

            Istruzione daModificare = (Istruzione) finalInstructions.getNow();
            if (!blockLabel.equals(flagTrovato.getLabel()))
                throw new ErroreDiCompilazione("Label not found: " + blockLabel + " on line " + daModificare.getNumber());
            daModificare.setNextAddress(flagTrovato.getIndex());
            finalInstructions.setNow(daModificare);
        }

        //goes on with instructions that are the latest of the blocks.

        while (!ultime.isEmpty()) {
            Flag toModify = (Flag) ultime.dequeue();
            int lastInstructionIndex = toModify.getIndex();
            String blockLabel = toModify.getLabel();

            finalInstructions.rewind();
            //it stops at the instruction to modify.
            while (finalInstructions.hasNext()) {
                Istruzione nowIstr = (Istruzione) finalInstructions.next();
                if (nowIstr.getAddress() == lastInstructionIndex)
                    break;
            }

            //tomodify instruction
            Istruzione toModifyIstr = (Istruzione) finalInstructions.getNow();

            //finds the block that has as previous a blockLabel label.
            blocchi.rewind();
            while (blocchi.hasNext()) {
                Blocco nowBlock = (Blocco) blocchi.next();
                if (nowBlock.getPrevLabel().equals(blockLabel))
                    break;
            }

            //the nextBlock.
            Blocco nextBlock = (Blocco) blocchi.getNow();
            //search for the first instruction of the next block.
            prime.rewind();
            //it stops at the corresponding label.
            while (prime.hasNext()) {
                Flag nowPrime = (Flag) prime.next();
                if (nowPrime.getLabel().equals(nextBlock.getLabel()))
                    break;
            }
            //first instruction of the next block.
            Flag firstInstruction = (Flag) prime.getNow();
            //if a next do not exists it can't manage it.
            toModifyIstr.setNextAddress(firstInstruction.getIndex());

            if (!nextBlock.getPrevLabel().equals(blockLabel))
                lastInstruction = toModifyIstr;
        }
        //pulisco le istruzioni da (ADDR)
        purge();
        System.out.println("IN\tCS\tNA\tInstruction");
        finalInstructions.rewind();
        while (finalInstructions.hasNext())
            System.out.println((Istruzione) finalInstructions.next());
    }

    //pulisce le istruzioni contenenti (ADDR) ovvero l'indirizzo riservato
    public void purge() {
        finalInstructions.rewind();
        while (finalInstructions.hasNext()) {
            Istruzione nowInstr = (Istruzione) finalInstructions.next();
            String instr = nowInstr.getInstruction();
            if (instr.length() != 0)
                if (instr.charAt(0) == '(') {
                    instr = instr.substring(instr.indexOf(")") + 1);
                    nowInstr.setInstruction(instr);
                }
        }
    }

    public void ifElseCatalogue() {
        blocchi.rewind();
        while (blocchi.hasNext()) {
            //extracts the list that contains actual block instructions.
            ListLS now = ((Blocco) blocchi.next()).getBlocco();
            now.rewind();
            //perambulation of all the block.
            while (now.hasNext()) {
                //extracts first instruction
                Istruzione estratta = (Istruzione) now.next();
                String corrente = estratta.getInstruction();
                //calls the method that gives back found labels.
                String[] etichette = trovaIfElse(estratta);
                //if gived array is not equal to null so there are, in yet analyzed instruction, ifs and elses.
                if (etichette != null) {
                    blocchi.saveNow();
                    blocchi.rewind();
                    try {
                        Blocco ifB = null;
                        Blocco elseB = null;
                        //search for the block with etichette[0] label beetwen all the blocks.
                        Blocco attuale = (Blocco) blocchi.next();
                        while (!attuale.getLabel().equals(etichette[0]))
                            attuale = (Blocco) blocchi.next();
                        ifB = attuale;
                        //now that it founds if, search for else, or the etichette[1] label.
                        blocchi.rewind();
                        attuale = (Blocco) blocchi.next();
                        try {
                            while (!attuale.getLabel().equals(etichette[1]))
                                attuale = (Blocco) blocchi.next();
                            elseB = attuale;
                        } catch (NoNextException e) {
                            if (etichette[1].length() != 0)
                                throw new ErroreDiCompilazione("Label not found, or incorrect if \non line: " +
                                        estratta.getNumber());
                            else
                                throw new ErroreDiCompilazione("Found else goto without label on line " +
                                        estratta.getNumber());
                        }
                        ifsAndElses.rewind();
                        IF_ELSE toInsert = new IF_ELSE(ifB, elseB);
                        boolean inserisci = true;
                        while (ifsAndElses.hasNext()) {
                            IF_ELSE nowBlockIF_ELSE = (IF_ELSE) ifsAndElses.next();
                            if (toInsert.equals(nowBlockIF_ELSE))
                                inserisci = false;
                            else if (toInsert.isInverse(nowBlockIF_ELSE))
                                throw new ErroreDiCompilazione("Impossible to assign to if an else's label or in" +
                                        " the contrary; \non line: " + estratta.getNumber());
                            else if (toInsert.isIncogruent(nowBlockIF_ELSE)) {
                                throw new ErroreDiCompilazione("Incongruent conditional jump on line " +
                                        estratta.getNumber() + "\n" + estratta.getOriginal());
                            }
                        }
                        if (inserisci)
                            ifsAndElses.insertHead(toInsert);
                    } catch (NoNextException e) {
                        if (etichette[0].length() != 0)
                            throw new ErroreDiCompilazione("Label not found :" + etichette[0] + "\n on line: " +
                                    estratta.getNumber());
                        else
                            throw new ErroreDiCompilazione("Found if() goto without label on line" +
                                    estratta.getNumber());
                    }
                    blocchi.restoreNow();
                }
            }
        }
    }

    public void dividi(String _instruction, int numeroIstruzione) {
        //with this method it builds instruction blocks based on the labels.

        //controllo che l"istruzione contenga il carattere :
        if (_instruction.contains(":")) {
            String[] splitted = _instruction.split(":");
            //if instruction split is greater than 2 so in instruction there's more than an occurrency
            //of " : " throws a compilation error.
            if (splitted.length > 2 || splitted.length == 0)
                throw new ErroreDiCompilazione("Unknow instruction: \n" + _instruction + "\n on line " + numeroIstruzione);
            //if the label not exists
            if (splitted[0].length() == 0)
                throw new ErroreDiCompilazione("Label expected. \n on line " + numeroIstruzione);
            if (splitted[0].contains(" "))
                throw new ErroreDiCompilazione("Label not permitted : " + splitted[0] + "\non line:" +
                        numeroIstruzione);
            String result = splitted[0];
            if (result.length() != 0)
                if (result.charAt(0) == '(') {
                    int j = 1;

                    while (j < result.length() && result.charAt(j++) != ')') ;
                    String toAppend = result.substring(0, j);
                    result = result.substring(j);
                    splitted[1] = toAppend + splitted[1];
                }
            result = result.replaceFirst(" *[a-zA-Z_0-9]* *", "");


            if (result.length() != 0) {
                throw new ErroreDiCompilazione("Label not permitted : " + splitted[0] + "\non line:" + numeroIstruzione);
            }
            String etichetta = "";
            try {
                String prevLabel = ((Blocco) blocchi.getHead()).getLabel();

                etichetta = splitted[0].replaceAll(" *", "");
                Blocco newBlock = new Blocco(etichetta, prevLabel);
                //check if the label is not null.
                if (etichetta.length() == 0)
                    throw new ErroreDiCompilazione("Label expected on line: " + numeroIstruzione);
                //check if the label contains spaces.

                //check if the label was yet declarated.
                blocchi.saveNow();
                blocchi.rewind();
                while (blocchi.hasNext()) {
                    Blocco now = (Blocco) blocchi.next();
                    if (now.getLabel().equals(etichetta))
                        throw new ErroreDiCompilazione("Label already defined: " + etichetta + " on line: " +
                                numeroIstruzione);
                }
                blocchi.restoreNow();
                //end of declaration check.

                //instead if is 1 first instruction of the block will be an empty instruction.
                if (splitted.length == 1)
                    newBlock.insertInstrunction(new Istruzione("", numeroIstruzione));
                else
                    newBlock.insertInstrunction(new Istruzione(splitted[1], numeroIstruzione));
                blocchi.insertHead(newBlock);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new ErroreDiCompilazione("Label expected on line: " + numeroIstruzione);
            } catch (EmptyListException e) {
                Blocco nuovoBlocco = new Blocco(splitted[0].replaceAll(" *", ""), "%noPrev");
                //first and empty instruction case.
                if (splitted.length == 1)
                    nuovoBlocco.insertInstrunction(new Istruzione("", numeroIstruzione));
                else
                    nuovoBlocco.insertInstrunction(new Istruzione(splitted[1], numeroIstruzione));
                blocchi.insertHead(nuovoBlocco);
                return;
            }
        } else {
            try {
                Blocco toRefresh = (Blocco) blocchi.getHead();
                toRefresh.insertInstrunction(new Istruzione(_instruction, numeroIstruzione));
                return;
            } catch (EmptyListException e) {
                Blocco nuovoBlocco = new Blocco("%noet", "%noPrev");
                nuovoBlocco.insertInstrunction(new Istruzione(_instruction, numeroIstruzione));
                blocchi.insertHead(nuovoBlocco);
                return;
            }
        }
    }

    //restituisce l'indirizzo di Main1
    public int getMain1() {
        prime.rewind();
        while (prime.hasNext()) {
            Flag now = (Flag) prime.next();
            if (now.getLabel().equals("Main1"))
                return now.getIndex();
        }
        throw new ErroreDiCompilazione("Main1 label not found");
    }

    public String[] trovaIfElse(Istruzione _toAnalyze) {
        String _instruction = _toAnalyze.getInstruction();
        String[] splitted = _instruction.split(";");
        String[] toReturn = new String[2];
        for (int i = 0; i < splitted.length; i++) {
            try {
                //check if the current instruction part has an if, if it's true extracts its label.
                if (splitted[i].substring(0, 4).equals("if(N") || splitted[i].substring(0, 4).equals("if(Z")) {
                    String etichetta = splitted[i].substring(9);
                    toReturn[0] = etichetta;
                    //now check if this instruction has an else, if it's not, it'll be the next instruction.
                    if (i + 1 < splitted.length)//makes attention not going out from the array.
                        if (splitted[i + 1].substring(0, 4).equals("else"))
                            toReturn[1] = splitted[i + 1].substring(8);
                        else
                            throw new ErroreDiCompilazione("Else expected on line " + _toAnalyze.getNumber());
                    else
                        throw new ErroreDiCompilazione("Else expected on line " + _toAnalyze.getNumber());

                    //checks if the label are different because it can't accept if(X) goto et1 else goto et1.
                    if (toReturn[0].equals(toReturn[1]))
                        throw new ErroreDiCompilazione("Unknown instruction: \n" + _toAnalyze.getOriginal() +
                                " \non line " + _toAnalyze.getNumber());
                    return toReturn;
                }
            } catch (StringIndexOutOfBoundsException e) {

            }
        }
        return null;
    }

    //trova le istruzioni con indirizzo riservato
    public void searchReserved() {
        blocchi.rewind();
        while (blocchi.hasNext()) {
            ListLS nowB = ((Blocco) blocchi.next()).getBlocco();
            nowB.rewind();
            while (nowB.hasNext()) {
                Istruzione nowInstr = (Istruzione) nowB.next();
                int addr = checkAddress(nowInstr);
                if (addr != -1) {
                    //controllo che non questo indirizzo non sia già stato riservato
                    reserved.rewind();
                    while (reserved.hasNext())
                        if (((Reserved) reserved.next()).getAddress() == addr)
                            throw new ErroreDiCompilazione("Address " + addr + " already reserved on line " +
                                    nowInstr.getNumber());
                    reserved.insertHead(new Reserved(addr, nowInstr));
                }

            }
        }
    }

    //restituisce l'indirizzo riservato dell'istruzione, -1 se non è presente
    public int checkAddress(Istruzione toCheck) {
        String toCheckS = toCheck.getInstruction();
        if (toCheckS.length() != 0) {
            if (toCheckS.charAt(0) == '(') {
                if (toCheckS.length() != 1) {
                    int j = 1;
                    String addr = "";
                    while (toCheckS.charAt(j) != ')') {
                        addr += toCheckS.charAt(j++);
                        if (j >= toCheckS.length())
                            throw new ErroreDiCompilazione("Unknown instruction" + toCheckS +
                                    " on line " + toCheck.getNumber());
                    }
                    toCheckS = toCheckS.substring(j);
                    try {
                        int addrI = Integer.decode(addr);
                        //un crontrollo sull'indirizzo
                        if (addrI >= 512 || addr.contains("-"))
                            throw new ErroreDiCompilazione("Value " + addr + " not permitted");
                        return addrI;
                    } catch (NumberFormatException e) {
                        throw new ErroreDiCompilazione(addr + " is NaN on line" + toCheck.getNumber());
                    }
                }
            }
        }
        return -1;
    }

    //restituisce true se l'indirizzo in input è riservato
    public boolean isReserved(int _address) {
        reserved.rewind();
        while (reserved.hasNext()) {
            if (((Reserved) reserved.next()).getAddress() == _address)
                return true;
        }
        return false;
    }

    public String hasGoto(Istruzione _instruction) {
        String testoIstr = (_instruction.getInstruction());
        String[] splitted = testoIstr.split(";");
        for (int i = 0; i < splitted.length; i++) {
            try {
                if (splitted[i].substring(0, 4).equals("goto")) {
                    try {
                        if (splitted[i].equals("goto(MBR)")) {
                            _instruction.setNextAddress(0);
                            String newInstruction = "";
                            for (int c = 0; c < splitted.length; c++) {
                                if (c == i)
                                    newInstruction += "J;";
                                else
                                    newInstruction += splitted[c] + ";";
                            }
                            _instruction.setInstruction(newInstruction);
                            return "(MBR)";
                        } else if (splitted[i].contains("goto(MBRor")) {
                            String[] splittedOR = splitted[i].split("or");
                            if (splittedOR.length != 2)
                                throw new ErroreDiCompilazione("Incorrect goto " + splitted[i] +
                                        " \non line " + _instruction.getNumber());
                            if (splittedOR[0].equals("goto(MBR")) {
                                if (!splittedOR[1].contains(")"))
                                    throw new ErroreDiCompilazione("Incorrect goto on line " +
                                            _instruction.getNumber());
                                splittedOR[1] = splittedOR[1].replace(")", "");
                                if (splittedOR[1].contains("-"))
                                    throw new ErroreDiCompilazione("Incorrect goto on line " +
                                            _instruction.getNumber());
                                int address = Integer.decode(splittedOR[1]);
                                String newInstruction = "";
                                for (int c = 0; c < splitted.length; c++) {
                                    if (c == i)
                                        newInstruction += "J;";
                                    else
                                        newInstruction += splitted[c] + ";";
                                }
                                _instruction.setNextAddress(address);
                                _instruction.setInstruction(newInstruction);
                                return "(MBR)";
                            } else
                                throw new ErroreDiCompilazione("Incorrect goto on line " +
                                        _instruction.getNumber());
                        } else {
                            String newInstruction = "";
                            for (int c = 0; c < splitted.length; c++) {
                                if (c != i)
                                    newInstruction += splitted[c] + ";";
                            }
                            _instruction.setInstruction(newInstruction);
                            return splitted[i].substring(4);
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        //this exception should never go, however it prints its message if ,
                        //unfortunately, it will...
                        System.out.println(e.getMessage());
                    } catch (NumberFormatException e) {
                        throw new ErroreDiCompilazione("Incorrect goto " + splitted[i] + " \non line " +
                                _instruction.getNumber());
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
            }
        }
        return null;
    }

    public String hasElse(Istruzione _instruction) {
        //extracts instruction's text.
        String testoIstr = _instruction.getInstruction();
        //splits it by ;
        String[] splitted = testoIstr.split(";");
        for (int i = 0; i < splitted.length; i++) {
            try {
                if (splitted[i].substring(0, 4).equals("else")) {
                    try {
                        String ifS = splitted[i - 1];
                        String et = splitted[i].substring(8);
                        char condition = ifS.charAt(3);
                        if (condition != 'N' && condition != 'Z')
                            throw new ErroreDiCompilazione("Malformed if " + ifS + " \non line " +
                                    _instruction.getNumber());
                        if (!(ifS.substring(0, 9)).equals("if(" + condition + ")goto"))
                            throw new ErroreDiCompilazione("Malformed if " + ifS + " \non line " +
                                    _instruction.getNumber());
                        String newInstruction = "";
                        for (int c = 0; c < splitted.length; c++) {
                            if (c == i - 1)
                                newInstruction += condition + ";";
                            else if (c == i) {
                                //do nothing
                            } else
                                newInstruction += splitted[c] + ";";
                        }
                        _instruction.setInstruction(newInstruction);
                        return et;
                    } catch (StringIndexOutOfBoundsException e) {
                        throw new ErroreDiCompilazione("Unknow instruction: \n" +
                                _instruction.getOriginal() +
                                "\n on line " + _instruction.getNumber());
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
                //in this case instruction is too short than the expected, its' ok, do nothing.
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new ErroreDiCompilazione("Unknown instruction : \n" +
                        _instruction.getOriginal() +
                        "\n on line " + _instruction.getNumber()
                );
            }
        }
        return null;
    }

    public void decodifica(Istruzione _toDecode) {
        String testoIstr = _toDecode.getInstruction();
        //builds a "nothingdoing" instruction.
        char[] micInstruction = new char[36];
        for (int i = 9; i < 32; i++)
            micInstruction[i] = '0';
        //the 1 of latest 4 places indicates to decoder that in B bus doesn't have to insert nothing.
        for (int i = 32; i < 36; i++)
            micInstruction[i] = '1';
        if (testoIstr.length() == 0)
            _toDecode.setMicInstruction(new String(micInstruction));
            //not empty instruction. Unscrambling is essential.
        else if (testoIstr.length() != 0) {
            //boolean variable that verifies the assignment
            boolean assignment = false;
            //boolean variable that verifies if the the there's more than a shift
            boolean shifting = false;
            String[] splitted = testoIstr.split(";");
            for (int i = 0; i < splitted.length; i++) {
                //assignment instruction( = )
                if (splitted[i].contains("=")) {
                    if (assignment)
                        throw new ErroreDiCompilazione("Unknow instruction \n" +
                                _toDecode.getOriginal() +
                                " \n on line " + _toDecode.getNumber());
                    assignment = true;
                    String[] splittedAss = splitted[i].split("=");
                    if (splittedAss.length <= 1)
                        throw new ErroreDiCompilazione("Unknow instruction \n" +
                                _toDecode.getOriginal() + " \n on line "
                                + _toDecode.getNumber());
                    //checks if = are beetwen characters.
                    for (int c = 0; c < splittedAss.length; c++)
                        if (splittedAss[c].length() == 0)
                            throw new ErroreDiCompilazione("Unknow instruction \n" +
                                    _toDecode.getOriginal() +
                                    " \n on line " + _toDecode.getNumber());
                    //puts to 1 all the bits corresponding to assignment variable.
                    if (splittedAss[splittedAss.length - 1].contains("<") &&
                            splittedAss[splittedAss.length - 1].indexOf("<") >= 1 ||
                            splittedAss[splittedAss.length - 1].contains(">") &&
                                    splittedAss[splittedAss.length - 1].indexOf(">") >= 1) {
                        boolean shiftB = false;
                        String shift = splittedAss[splittedAss.length - 1];
                        if (shift.contains("<<8") && shift.contains(">>1"))
                            throw new ErroreDiCompilazione("Impossible to have more than one shift \n" +
                                    "on line :" + _toDecode.getNumber());
                        if (shift.contains("<<8")) {
                            if (shift.lastIndexOf("8") != shift.length() - 1)
                                throw new ErroreDiCompilazione("Unknown instruction \n" +
                                        _toDecode.getOriginal() +
                                        "\n on line " + _toDecode.getNumber());
                            micInstruction[12] = '1';
                            shiftB = true;
                            shift = shift.replace("<<8", "");
                            splittedAss[splittedAss.length - 1] = shift;
                        } else if (shift.contains(">>1")) {
                            if (shiftB)
                                throw new ErroreDiCompilazione("Two shift are not permitted \n on line " +
                                        _toDecode.getNumber());
                            if (shift.lastIndexOf("1") != shift.length() - 1)
                                throw new ErroreDiCompilazione("Unknown instruction \n" +
                                        _toDecode.getOriginal() +
                                        "\n on line " + _toDecode.getNumber());
                            micInstruction[13] = '1';
                            shift = shift.replace(">>1", "");
                            splittedAss[splittedAss.length - 1] = shift;
                        } else
                            throw new ErroreDiCompilazione("Unknown instruction \n" +
                                    _toDecode.getOriginal() +
                                    "on line :" + _toDecode.getNumber());
                    }
                    for (int c = 0; c < splittedAss.length - 1; c++) {
                        if (splittedAss[c].equals("H")) {
                            micInstruction[20] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("OPC")) {
                            micInstruction[21] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("TOS")) {
                            micInstruction[22] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("CPP")) {
                            micInstruction[23] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("LV")) {
                            micInstruction[24] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("SP")) {
                            micInstruction[25] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("PC")) {
                            micInstruction[26] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("MDR")) {
                            micInstruction[27] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("MAR")) {
                            micInstruction[28] = '1';
                            splittedAss[c] = null;
                        }
                        if (splittedAss[c] != null && splittedAss[c].equals("Z"))
                            splittedAss[c] = null;
                        if (splittedAss[c] != null && splittedAss[c].equals("N"))
                            splittedAss[c] = null;
                    }
                    for (int c = 0; c < splittedAss.length - 1; c++)
                        if (splittedAss[c] != null)
                            throw new ErroreDiCompilazione("Not permitted assigment to " +
                                    splittedAss[c] + " \non line " + _toDecode.getNumber());
                    //sum operations.
                    String operazione = splittedAss[splittedAss.length - 1];
                    if (operazione.contains("+")) {
                        if (operazione.lastIndexOf("+") == operazione.length() - 1)
                            throw new ErroreDiCompilazione("Unknow instruction " +
                                    _toDecode.getOriginal() +
                                    " \n on line " + _toDecode.getNumber());
                        String[] splitOp = operazione.split("\\+");
                        if (splitOp.length > 3 || splitOp.length < 2)
                            throw new ErroreDiCompilazione("Unknow instruction " +
                                    _toDecode.getOriginal() + " \non line " +
                                    _toDecode.getNumber());
                        if (splitOp.length == 3) {
                            boolean H = false;
                            boolean constant = false;
                            for (int c = 0; c < splitOp.length; c++) {
                                if (splitOp[c].equals("H")) {
                                    H = true;
                                }
                                if (splitOp[c].equals("1")) {
                                    constant = true;
                                }
                            }
                            if (H && constant) {
                                String toAlu = "111101";
                                String busB = "no";
                                for (int c = 0; c < splitOp.length; c++) {
                                    if (splitOp[c] != null) {
                                        if (splitOp[c].equals("OPC"))
                                            busB = "1000";
                                        if (splitOp[c].equals("TOS"))
                                            busB = "0111";
                                        if (splitOp[c].equals("CPP"))
                                            busB = "0110";
                                        if (splitOp[c].equals("LV"))
                                            busB = "0101";
                                        if (splitOp[c].equals("SP"))
                                            busB = "0100";
                                        if (splitOp[c].equals("MBRU"))
                                            busB = "0011";
                                        if (splitOp[c].equals("MBR"))
                                            busB = "0010";
                                        if (splitOp[c].equals("PC"))
                                            busB = "0001";
                                        if (splitOp[c].equals("MDR"))
                                            busB = "0000";
                                    }
                                }
                                if (busB.equals("no"))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \n on line " + _toDecode.getNumber());
                                for (int c = 14; c - 14 < toAlu.length(); c++)
                                    micInstruction[c] = toAlu.charAt(c - 14);
                                for (int c = 32; c - 32 < busB.length(); c++)
                                    micInstruction[c] = busB.charAt(c - 32);
                            } else
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() + " \n on line " +
                                        _toDecode.getNumber());
                        } else {
                            boolean H = splitOp[0].equals("H") || splitOp[1].equals("H");
                            boolean constant = splitOp[0].equals("1") || splitOp[1].equals("1");
                            if (!H && !constant)
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() + " \n on line " +
                                        _toDecode.getNumber());
                            String toAlu = "";
                            String busB = "";
                            if (H && constant) {
                                toAlu = "111001";
                                busB = "1111";//doesn't matter.
                            } else if (H || constant) {
                                busB = "no";
                                String busBVar = splitOp[0].equals("H") ? splitOp[1] : splitOp[0];
                                if (busBVar.equals("OPC"))
                                    busB = "1000";
                                if (busBVar.equals("TOS"))
                                    busB = "0111";
                                if (busBVar.equals("CPP"))
                                    busB = "0110";
                                if (busBVar.equals("LV"))
                                    busB = "0101";
                                if (busBVar.equals("SP"))
                                    busB = "0100";
                                if (busBVar.equals("MBRU"))
                                    busB = "0011";
                                if (busBVar.equals("MBR"))
                                    busB = "0010";
                                if (busBVar.equals("PC"))
                                    busB = "0001";
                                if (busBVar.equals("MDR"))
                                    busB = "0000";
                                if (H)
                                    toAlu = "111100";
                                else
                                    toAlu = "110101";
                                if (busB.equals("no"))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \n on line " + _toDecode.getNumber());
                            }
                            for (int c = 14; c - 14 < toAlu.length(); c++)
                                micInstruction[c] = toAlu.charAt(c - 14);
                            for (int c = 32; c - 32 < busB.length(); c++)
                                micInstruction[c] = busB.charAt(c - 32);
                        }
                    }
                    //subtraction operations
                    else if (operazione.contains("-")) {
                        if (operazione.lastIndexOf("-") == operazione.length() - 1)
                            throw new ErroreDiCompilazione("Unknow instruction " +
                                    _toDecode.getOriginal() +
                                    " \n on line " + _toDecode.getNumber());
                        String[] splitOp = operazione.split("\\-");
                        if (splitOp.length != 2)
                            throw new ErroreDiCompilazione("Unknow instruction " +
                                    _toDecode.getOriginal() +
                                    " \n on line " + _toDecode.getNumber());
                        String toAlu = "";
                        String busB = "";
                        if (splitOp[0].length() == 0) {
                            if (splitOp[1].equals("H"))
                                toAlu = "111011";
                            else if (splitOp[1].equals("1"))
                                toAlu = "010010";
                            else
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() +
                                        " \non line " + _toDecode.getNumber());
                            busB = "1111";
                        } else {
                            if (splitOp.length == 1)
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() +
                                        " \non line " + _toDecode.getNumber());

                            boolean H = splitOp[1].equals("H");
                            boolean constant = splitOp[1].equals("1");
                            if (H && constant || !H && !constant)
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() +
                                        " \non line " + _toDecode.getNumber());
                            String busBVar = splitOp[0];
                            busB = "no";
                            if (busBVar.equals("OPC"))
                                busB = "1000";
                            if (busBVar.equals("TOS"))
                                busB = "0111";
                            if (busBVar.equals("CPP"))
                                busB = "0110";
                            if (busBVar.equals("LV"))
                                busB = "0101";
                            if (busBVar.equals("SP"))
                                busB = "0100";
                            if (busBVar.equals("MBRU"))
                                busB = "0011";
                            if (busBVar.equals("MBR"))
                                busB = "0010";
                            if (busBVar.equals("PC"))
                                busB = "0001";
                            if (busBVar.equals("MDR"))
                                busB = "0000";
                            if (H) {
                                toAlu = "111111";
                            } else if (constant) {
                                toAlu = "110111";
                            } else
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() + " \non line " +
                                        _toDecode.getNumber());

                            if (busB.equals("no"))
                                throw new ErroreDiCompilazione("Unknow instruction "
                                        + _toDecode.getOriginal() +
                                        " \non line " + _toDecode.getNumber());

                        }
                        for (int c = 14; c - 14 < toAlu.length(); c++)
                            micInstruction[c] = toAlu.charAt(c - 14);
                        for (int c = 32; c - 32 < busB.length(); c++)
                            micInstruction[c] = busB.charAt(c - 32);
                    }
                    //other operations
                    else {
                        try {
                            String busB = "";
                            String toAlu = "";
                            if (operazione.equals("1")) {
                                toAlu = "010001";
                                busB = "1111";
                            } else if (operazione.equals("0")) {
                                toAlu = "010000";
                                busB = "1111";
                            } else if (operazione.equals("H")) {
                                toAlu = "011000";
                                busB = "1111";
                            } else if (operazione.equals("LV")) {
                                toAlu = "010100";
                                busB = "0101";
                            } else if (operazione.equals("SP")) {
                                toAlu = "010100";
                                busB = "0100";
                            } else if (operazione.equals("PC")) {
                                toAlu = "010100";
                                busB = "0001";
                            } else if (operazione.equals("MDR")) {
                                toAlu = "010100";
                                busB = "0000";
                            } else if (operazione.equals("MBR")) {
                                toAlu = "010100";
                                busB = "0010";
                            } else if (operazione.equals("CPP")) {
                                toAlu = "010100";
                                busB = "0110";
                            } else if (operazione.equals("TOS")) {
                                toAlu = "010100";
                                busB = "0111";
                            } else if (operazione.equals("OPC")) {
                                toAlu = "010100";
                                busB = "1000";
                            } else if (operazione.equals("MBRU")) {
                                toAlu = "010100";
                                busB = "0011";
                            } else if (operazione.substring(0, 4).equals("not(")) {
                                if (operazione.equals("not(H)")) {
                                    toAlu = "011010";
                                    busB = "0000";
                                } else {
                                    toAlu = "101100";
                                    String busBVar = operazione.substring(4);
                                    if (!busBVar.contains(")"))
                                        throw new ErroreDiCompilazione("Unknow instruction " +
                                                _toDecode.getOriginal() +
                                                " \non line " +
                                                _toDecode.getNumber());
                                    busBVar = busBVar.replace(")", "");
                                    busB = "no";
                                    if (busBVar.equals("OPC"))
                                        busB = "1000";
                                    if (busBVar.equals("TOS"))
                                        busB = "0111";
                                    if (busBVar.equals("CPP"))
                                        busB = "0110";
                                    if (busBVar.equals("LV"))
                                        busB = "0101";
                                    if (busBVar.equals("SP"))
                                        busB = "0100";
                                    if (busBVar.equals("MBRU"))
                                        busB = "0011";
                                    if (busBVar.equals("MBR"))
                                        busB = "0010";
                                    if (busBVar.equals("PC"))
                                        busB = "0001";
                                    if (busBVar.equals("MDR"))
                                        busB = "0000";
                                }
                                if (busB.equals("no"))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() + " \non line "
                                            + _toDecode.getNumber());
                            } else if (operazione.contains("AND")) {
                                toAlu = "001100";
                                String[] splitOp = operazione.split("AND");
                                if (splitOp.length != 2)
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());
                                String busBVar = "";
                                if (splitOp[0].equals("H"))
                                    busBVar = splitOp[1];
                                else if (splitOp[1].equals("H"))
                                    busBVar = splitOp[0];
                                if (busBVar.equals(""))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());
                                busB = "no";
                                if (busBVar.equals("OPC"))
                                    busB = "1000";
                                if (busBVar.equals("TOS"))
                                    busB = "0111";
                                if (busBVar.equals("CPP"))
                                    busB = "0110";
                                if (busBVar.equals("LV"))
                                    busB = "0101";
                                if (busBVar.equals("SP"))
                                    busB = "0100";
                                if (busBVar.equals("MBRU"))
                                    busB = "0011";
                                if (busBVar.equals("MBR"))
                                    busB = "0010";
                                if (busBVar.equals("PC"))
                                    busB = "0001";
                                if (busBVar.equals("MDR"))
                                    busB = "0000";
                                if (busB.equals("no"))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());

                            } else if (operazione.contains("OR")) {
                                toAlu = "011100";
                                String[] splitOp = operazione.split("OR");
                                if (splitOp.length != 2)
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());
                                String busBVar = "";
                                if (splitOp[0].equals("H"))
                                    busBVar = splitOp[1];
                                else if (splitOp[1].equals("H"))
                                    busBVar = splitOp[0];
                                if (busBVar.equals(""))
                                    throw new ErroreDiCompilazione("Unknow instruction " +
                                            _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());
                                busB = "no";
                                if (busBVar.equals("OPC"))
                                    busB = "1000";
                                if (busBVar.equals("TOS"))
                                    busB = "0111";
                                if (busBVar.equals("CPP"))
                                    busB = "0110";
                                if (busBVar.equals("LV"))
                                    busB = "0101";
                                if (busBVar.equals("SP"))
                                    busB = "0100";
                                if (busBVar.equals("MBRU"))
                                    busB = "0011";
                                if (busBVar.equals("MBR"))
                                    busB = "0010";
                                if (busBVar.equals("PC"))
                                    busB = "0001";
                                if (busBVar.equals("MDR"))
                                    busB = "0000";
                                if (busB.equals("no"))
                                    throw new ErroreDiCompilazione("Unknow instruction "
                                            + _toDecode.getOriginal() +
                                            " \non line " + _toDecode.getNumber());
                            } else
                                throw new ErroreDiCompilazione("Unknow instruction " +
                                        _toDecode.getOriginal() +
                                        " \non line " + _toDecode.getNumber());
                            for (int c = 14; c - 14 < toAlu.length(); c++)
                                micInstruction[c] = toAlu.charAt(c - 14);
                            for (int c = 32; c - 32 < busB.length(); c++)
                                micInstruction[c] = busB.charAt(c - 32);
                        } catch (StringIndexOutOfBoundsException e) {
                            throw new ErroreDiCompilazione("Unknow instruction " +
                                    _toDecode.getOriginal() +
                                    " \non line " + _toDecode.getNumber());
                        }
                    }
                    splitted[i] = null;
                }
                //branch instruction
                else if (splitted[i].equals("N") || splitted[i].equals("Z") || splitted[i].equals("J")) {
                    if (splitted[i].equals("N")) {
                        micInstruction[10] = '1';
                        if (!assignment)
                            throw new ErroreDiCompilazione("Assignment expected\n on line:" +
                                    _toDecode.getNumber());
                    }
                    if (splitted[i].equals("Z")) {
                        micInstruction[11] = '1';
                        if (!assignment)
                            throw new ErroreDiCompilazione("Assignment expected\n on line:" + _toDecode.getNumber());
                    }
                    if (splitted[i].equals("J"))
                        micInstruction[9] = '1';
                    splitted[i] = null;
                }
                //memory instrunctions
                else if (splitted[i].equals("wr")) {
                    micInstruction[29] = '1';
                    splitted[i] = null;
                } else if (splitted[i].equals("rd")) {
                    micInstruction[30] = '1';
                    splitted[i] = null;
                } else if (splitted[i].equals("fetch")) {
                    micInstruction[31] = '1';
                    splitted[i] = null;
                } else
                    throw new ErroreDiCompilazione("Unknow instruction " +
                            _toDecode.getOriginal() + " \non line " +
                            _toDecode.getNumber());

            }

        }//unscrambling of the address.
        int nextAddress = _toDecode.getNextAddress();
        for (int i = 8; i >= 0; i--) {
            int resto = nextAddress % 2;
            nextAddress = nextAddress / 2;
            if ((resto) == 1)
                micInstruction[i] = '1';
            else
                micInstruction[i] = '0';
        }
        _toDecode.setMicInstruction(new String(micInstruction));
        System.out.println(_toDecode.getMicInstruction());

    }

    public void translate(String[] _instructions) {
        try {
            if (_instructions.length > 512)
                throw new ErroreDiCompilazione("ControlStore is too small for this program.");
            System.out.println("---MAL STANDARD OUTPUT---");
            System.out.println("******MAL Instruction with address assigned");
            decodificaIndirizzi(removeComments(_instructions));
            finalInstructions.rewind();
            System.out.println("******Generated MIC-1 Binary Instructions");
            while (finalInstructions.hasNext())
                decodifica((Istruzione) finalInstructions.next());
            System.out.println("---END MAL STANDARD OUTPUT---\n");
            if (lastInstruction == null)
                warning = true;
        } catch (ErroreDiCompilazione e) {
            throw new ErroreDiCompilazione(e.getMessage());
        }
//	catch(Exception e)
        //{
        //  throw new ErroreDiCompilazione("Unknown translation error");
        //}
    }

    public boolean getWarning() {
        return warning;
    }

    public void refresh() {
        blocchi = new ListLS();
        prime = new ListLS();
        ultime = new Queue();
        ifsAndElses = new ListLS();
        finalInstructions = new ListLS();
        unResolved = new ListLS();
        warning = false;
        lastInstruction = null;
        //myCStore.newMemory();
    }

    //it gives back the address of the first instruction to execute.
    public String getFirstInstruction() {
        int cStorePosition = getMain1();
        if (finalInstructions.isEmpty())
            throw new ErroreDiCompilazione("No MicroProgram found!");
        char[] firstAddress = new char[9];
        for (int i = 8; i >= 0; i--) {
            int resto = cStorePosition % 2;
            cStorePosition = cStorePosition / 2;
            if (resto == 1)
                firstAddress[i] = '1';
            else
                firstAddress[i] = '0';
        }
        return new String(firstAddress);
    }

    //it gives back the address of the last instruction of the microprogram.
    public String getLastInstruction() {
        return lastInstruction.getMicInstruction();
    }

    //it writes on control store
    public controlStore writeRom() {
        controlStore toReturn = new controlStore();
        finalInstructions.rewind();
        while (finalInstructions.hasNext()) {
            Istruzione now = (Istruzione) finalInstructions.next();
            toReturn.writeMicInstruction(now.getAddress(), now.getMicInstruction());
        }
        return toReturn;
    }

}

class Reserved {
    private int address;
    private Istruzione istr;

    public Reserved(int _address, Istruzione _istruzione) {
        address = _address;
        istr = _istruzione;
    }

    public int getAddress() {
        return address;
    }

    public Istruzione getIstruzione() {
        return istr;
    }
}

class Blocco {
    boolean tradotto;
    private ListLS blocco;
    private String label;
    private String prevLabel;

    public Blocco(String _label, String _prevLabel) {
        label = _label;
        blocco = new ListLS();
        prevLabel = _prevLabel;
        tradotto = false;
    }

    public String getLabel() {
        return label;
    }

    public void setTradotto(boolean _tra) {
        tradotto = _tra;
    }

    public String getPrevLabel() {
        return prevLabel;
    }

    public ListLS getBlocco() {
        return blocco;
    }

    public void insertInstrunction(Istruzione _instruction) {
        blocco.insertTail(_instruction);
    }

    public String toString() {
        blocco.rewind();
        String toReturn = label + "\n";
        while (blocco.hasNext())
            toReturn += (Istruzione) blocco.next() + "\n";
        return toReturn;
    }

    public boolean isTraduced() {
        return tradotto;
    }

    public Istruzione getNextInstruction() {
        if (blocco.hasNext())
            return (Istruzione) (this.blocco.next());
        else
            throw new ErroreDiCompilazione("Internal error");
    }
}

class IF_ELSE {
    private Blocco ifB;
    private Blocco elseB;

    public IF_ELSE(Blocco _ifB, Blocco _elseB) {
        ifB = _ifB;
        elseB = _elseB;
    }

    public Blocco getIf() {
        return ifB;
    }

    public Blocco getElse() {
        return elseB;
    }

    public boolean equals(IF_ELSE _toCompare) {
        return ifB.getLabel().equals(_toCompare.getIf().getLabel()) && elseB.getLabel().equals(_toCompare.getElse().getLabel());
    }

    public boolean isInverse(IF_ELSE _toCompare) {
        return ifB.getLabel().equals(_toCompare.getElse().getLabel()) || elseB.getLabel().equals(_toCompare.getIf().getLabel());
    }

    public boolean isIncogruent(IF_ELSE _toCompare) {
        //if corresponds if but not else
        if ((ifB.getLabel().equals(_toCompare.getIf().getLabel())) && (!(elseB.getLabel().equals(_toCompare.getElse().getLabel()))))
            return true;
        //if correspond else but not if
        if ((!ifB.getLabel().equals(_toCompare.getIf().getLabel())) && (elseB.getLabel().equals(_toCompare.getElse().getLabel())))
            return true;
        return false;
    }
}

class Istruzione {
    private int address;
    private int nextAddress;
    private int instructionNumber;
    private String originalInstruction;
    private String istruzione;
    private String micInstruction;

    public Istruzione(int _nextAddress, int _address, String _istruzione) {
        address = _address;
        originalInstruction = _istruzione;
        nextAddress = _nextAddress;
        istruzione = _istruzione;
    }

    public Istruzione(String _istruzione, int _instructionNumber) {
        this(-1, -1, _istruzione);
        originalInstruction = _istruzione;
        istruzione = _istruzione.replace(" ", "");
        instructionNumber = _instructionNumber;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int _address) {
        address = _address;
    }

    public String getInstruction() {
        return istruzione;
    }

    public void setInstruction(String _newInstruction) {
        istruzione = _newInstruction;
    }

    public String getMicInstruction() {
        return micInstruction;
    }

    public void setMicInstruction(String _newMicInstruction) {
        micInstruction = _newMicInstruction;
    }

    public String toString() {
        return "" + instructionNumber + "\t" + address + " \t " + nextAddress + " \t " + istruzione;
    }

    public int getNumber() {
        return instructionNumber;
    }

    public String getOriginal() {
        return originalInstruction;
    }

    public int getNextAddress() {
        return nextAddress;
    }

    public void setNextAddress(int _NextAddress) {
        nextAddress = _NextAddress;
    }
}

class Flag {
    private int index;
    private String label;

    public Flag(int _index, String _label) {
        index = _index;
        label = _label;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }
}