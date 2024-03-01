

import java.math.BigInteger;

public class Distributeur {//Objet qui sert de distributeur d'intervalle
    private BigInteger courant;

    public Distributeur() {
        courant = BigInteger.ZERO;
    }

    public synchronized String getNumber() {//renvoie les nombres allant de X000000 à X999999 où X est le million à distribuer
        String str = courant + " ";
        courant = courant.add(BigInteger.valueOf(999999));
        str += courant;
        courant = courant.add(BigInteger.valueOf(1));
        return str;
    }
}