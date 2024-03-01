

import java.math.BigInteger;
import java.util.Hashtable;

public class TabRes {//Objet qui implémente une Hashtable, avec des attributs supplémentaires
    private Hashtable<BigInteger, Integer> index;
    private BigInteger max;
    private volatile boolean Filling;

    public TabRes (BigInteger max, boolean filling) {//Constructeur qui récupère la valeur maximale à stocker ainsi que le boolean définissant son remplissage
        this.max = max;
        this.index = new Hashtable<>();
        this.Filling = filling;
    }

    public boolean isFull() {//plusoeurs méthodes de type getters setters
        return index.size() >= 1000000;
    }

    public boolean isFilling() {
        return Filling;
    }

    public void setFilling (boolean filling) {
        this.Filling = filling;
    }

    public int getTaille() {
        return index.size();
    }

    public void ajoute (BigInteger bi, Integer i) {
        index.put(bi, i);
    }

    public int get (BigInteger bi){
        return index.get(bi);
    }

    public Hashtable<BigInteger, Integer> getIndex() {
        return index;
    }

    public BigInteger getMax() {
        return max;
    }

    public void setMax (BigInteger newMax) {
        this.max = newMax;
    }

    public void clear() {
        this.index.clear();
    }

    public void setIndex (Hashtable<BigInteger, Integer> index){
        this.index = index;
    }
}