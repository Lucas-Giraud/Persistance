

import java.net.*;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Worker {
    static int port = 8080;
    public static boolean arreter = false;

    public static void main (String[] args) throws Exception {
        Socket socket = new Socket("localhost", port);//créé une connexion avec le serveur à l'aide de son adresse IP et du port associé au serveur
        System.out.println("SOCKET = " + socket);
        System.out.println("Pour fermer le Worker, tapez END");
        PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);//ouvre un flux d'écriture d'objet de type String et nettoie le flux après chaque envoi
        pw.println(args[0]);
        ObjectOutputStream sisw = new ObjectOutputStream(socket.getOutputStream());//créé deux flux d'écriture" et de lecture
        ObjectInputStream sisr = new ObjectInputStream(socket.getInputStream());
        int nombreDeCoeurs = Runtime.getRuntime().availableProcessors();
        Message MIntervalle = new Message(Commande.Intervalle, null, nombreDeCoeurs + "");
        StopWorker sw = new StopWorker();//créé et lance un thread qui gère les entrées dans le terminal
        sw.start();
        while (!arreter) {
            List<String> res = Collections.synchronizedList(new ArrayList<>());
            while (res.size() < 100000) { 
                try {
                    sisw.writeObject(MIntervalle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Message message = (Message) sisr.readObject();//récupère un message contenant l'intervalle, puis le distribut en fonction de son nombre de coeurs
                dispense(message, res);
            }
            Message MPersi = new Message(Commande.GivePersi, res, "");//une fois que la liste est pleine, elle est envoyée au serveru puis vidée
            sisw.writeObject(MPersi);
            res.clear();
        }
        Message message = new Message(null, null, "END");
        sisw.writeObject(message);//ferme tous les flux
        sisw.close();
        sisr.close();
        socket.close();
    }

    public static void dispense (Message message, List<String> res) {//méthode qui créé autant de thread que la machine possède de coeurs et chacun récupère un plus petit intervalle à calculer
        String s = (String) message.parametre;
        //System.out.println(s);
        ArrayList<Persistance> threads = new ArrayList<>();
        BigInteger n1 = new BigInteger(s.split(" ")[0]);
        BigInteger n2 = new BigInteger(s.split(" ")[1]);
        int coeur = Runtime.getRuntime().availableProcessors();
        int pas = Integer.parseInt((n2.subtract(n1)).divide(BigInteger.valueOf(coeur))+"");
        while (n1.compareTo(n2) <= 0) {
            Persistance p = new Persistance(n1, res, pas);//créé et lance les threads
            p.start();
            threads.add(p);
            n1 = n1.add(BigInteger.valueOf(pas));
        }
        for (int i = 0; i < threads.size(); i++) {//attends que tous les threads de la liste aient fini de calculer
            try {
                threads.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class StopWorker extends Thread {//thread qui gère les entrées dans le terminal
    private BufferedReader br;

    public StopWorker(){//constructeur par défaut qui initialise le bufferdReader
        try {
            this.br = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){//dès que le message entré dans le terminal correspond à END, coupe le Worker
        String s = "";
        while(!s.equals("END")){
            try {
                s = br.readLine();                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Worker.arreter = true;
    }
}

class Persistance extends Thread {//thread de calcul
    private BigInteger nombre;
    private List<String> res;
    private int cpt;

    public Persistance (BigInteger n, List<String> res, int cpt) {//Constructeur qui récupère la valeur de départ de son intervalle, la liste à remplir, ainsi que le nombre de valeur à calculer
        this.nombre = n;
        this.res = res;
        this.cpt = cpt;
    } 

    public int getPersi (BigInteger n) {//méthode récursive qui calcule la persistance multiplicative d'un nombre
        String str = n + "";
        if (str.length() == 1)
            return 0;
        BigInteger m = BigInteger.ONE;
        for (int i = 0; i < str.length(); i++) {
            m = m.multiply(new BigInteger(str.charAt(i) + ""));
        }
        return 1 + getPersi(m);
    }

    public void run() {
        for (int i = 0; i < cpt; i++) {
            int resultat = getPersi(nombre.add(BigInteger.valueOf(i)));
            res.add(nombre.add(BigInteger.valueOf(i)) + " " + resultat);
            //System.out.println(nombre.add(BigInteger.valueOf(i)) + " " + resultat);
        }
    }
}