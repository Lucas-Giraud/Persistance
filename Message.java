

import java.io.*;

public class Message implements Serializable {//Objet qui sert à faire trnasister les diverses informations envoyées par les workers, clients et serveur 
	public Commande commande;
	public Object parametre;
	public String texte;

	public Message(Commande commande, Object parametre, String texte) {
		this.commande = commande;
		this.parametre = parametre;
		this.texte = texte;
	}
}