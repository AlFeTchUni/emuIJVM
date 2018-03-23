import Emulator.Emulator;

public class Main {
    public static void main(String[] args) {
        try {
            Emulator myEm = new Emulator();
        } catch (Exception e) {

            System.out.println("emuIJVM è andato in crash, ti prego di farmelo presente, questo è u bug. " +
                    "Se riesci a recuperare il " +
                    "programma che era in esecuzione... inviamelo! dariospy[AT]tiscali.it\n" +
                    "L'errore è\n" + e.getMessage() + "\nStack Trace" + e.getStackTrace()
            );
        }
    }
}
