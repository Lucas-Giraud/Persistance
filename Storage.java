

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Storage extends Thread {//thread qui gère le stockage des valeurs dans des fichiers
    private long currentPage;
    private TabRes index;
    private Hashtable<BigInteger, Integer> clone;

    public Storage (TabRes index, int currentPage) {//Constructeur qui récupère les valeurs à stocker ainsi que le numéro de la page à remplir
        this.index = index;
        this.currentPage = currentPage;
        this.clone = new Hashtable<>();
    }

    public Storage(){//Constructeur par défaut qui initialise les attributs à 0 ou null
        this.index = new TabRes(null, true);
        this.currentPage = 0;
    }

    public void cloner (TabRes index){//méthode qui effecue une copie des valeurs à stocker afin de pouvoir vider l'index plein dans serveurMC
        Set<BigInteger> s = index.getIndex().keySet();
        for(BigInteger k : s){
            if(currentPage == 0){
                currentPage = Integer.parseInt(""+k.divide(BigInteger.valueOf(100000)));
            }
            clone.put(k, index.get(k));
        }
        index.clear();
    }

    public void store() {//méthode qui stocke l'index dans un fichier de type .ser
        try {
            cloner(index);
            System.out.println("je stocke sur ./Storage/" + currentPage + ".ser");
            FileOutputStream fileOut = new FileOutputStream("./Storage/" + currentPage + ".ser");//création des flux d'écriture
            BufferedOutputStream bos = new BufferedOutputStream(fileOut);
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(clone);
            bos.close();
            out.close();//fermeture des flux
            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Hashtable<BigInteger, Integer> readFile (long Page) {//méthode qui renvoie la hashtable contenue dans un fichier en fonction de la page donnée en paramètre
        File ReadFile = new File("./Storage/" + Page + ".ser");
        Hashtable<BigInteger, Integer> h = new Hashtable<>();
        if (ReadFile.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(ReadFile);
                BufferedInputStream bis = new BufferedInputStream(fileIn);
                ObjectInputStream in = new ObjectInputStream(bis);
                h = (Hashtable<BigInteger, Integer>) in.readObject();
                in.close();
                fileIn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return h;
        }
        return h;//renvoie une hashtable vide si le fichier demandé n'existe pas
    }

    public void run() {
        store();
    }
}