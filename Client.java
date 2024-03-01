import java.io.*;
import java.net.*;

public class Client {
	static int port = 8080;
	public static boolean arreter = false;

	public static void main (String[] args) throws Exception {
		Socket socket = new Socket("172.31.18.38", port);//créé une connexion avec l'adresse IP de la machine qui fait tourner le serveur ainsi que le port utilisé
		System.out.println("SOCKET = " + socket);
		PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);//créé un flux d'écriture d'objet de type String et nettoie le flux après chaque envoie
        pw.println(args[0]);
		ObjectOutputStream sisw = new ObjectOutputStream(socket.getOutputStream());//créé des flux d'écriture et de lecture d'objet
		ObjectInputStream sisr = new ObjectInputStream(socket.getInputStream());
		GererSaisie saisie = new GererSaisie(sisw,sisr);//créé une instance de GererSaisie, le démarre et attends la fin de son execution pour poursuivre l'éxecution du main
		saisie.start();
		saisie.join();
		System.out.println("END"); //affiche le message END et ferme tous les flux entre le serveur et le Client
		Message message = new Message(null, null, "END");
		sisw.writeObject(message);
		sisr.close();
		sisw.close();
		socket.close();
	}
}

class GererSaisie extends Thread {//Thread qui va gérer le dialogue entre le serveur et le client
	private BufferedReader entreeClavier;
	private ObjectInputStream sisr;
	private ObjectOutputStream sisw;

	public GererSaisie (ObjectOutputStream sisw, ObjectInputStream sisr) {//Constructeur de la classe qui récupère les flux de Client et ouvre un flux de lecture dans le terminal
		entreeClavier = new BufferedReader(new InputStreamReader(System.in));
		this.sisw = sisw;
		this.sisr = sisr;
	}

	public void run() {
		String str;
		try {
			do {//affichage dans la console des différentes options que le Client a
				System.out.println("tapez :");
				System.out.println("1 pour demander une persistance particuliere");
				System.out.println("2 pour demander la persistance moyenne d'un intervalle");
				System.out.println("3 pour demander la persistance médiane d'un intervalle");
				System.out.println("4 pour demander le nombre d'occurence d'une persistance");
				System.out.println("5 pour demander le nombre d'occurence de chaque persistance");
				System.out.println("6 pour demander la moyenne des persistances a cet instant");
				System.out.println("7 pour demander la mediane des persistances a cet instant");
				System.out.println("8 pour consulter les workers actifs");
				System.out.println("9 pour quitter");
				str = entreeClavier.readLine();
				String s = "";
				Message message=new Message(null, null, "");
				switch (str) {
					case "1": {
						do {
							System.out.println("Entrez la valeur a calculer : ");
							s = entreeClavier.readLine();
						} while (Integer.parseInt(s)<0);
						message.parametre = s;
						message.commande = Commande.PersiParti;
						System.out.println("Recherche en cours...");
						break;
					}
					case "2": {
						do {
							System.out.println("Entrez la valeur de départ : ");
							s = entreeClavier.readLine();
						} while (Integer.parseInt(s)<0);
						String s1 = "";
						do {
							System.out.println("Entrez la valeur de fin incluse : ");
							s1 = entreeClavier.readLine();
						} while (Integer.parseInt(s)<0);
						s = s + " " + s1;
						message.parametre = s;
						message.commande = Commande.MoyPersiInt;
						System.out.println("Recherche en cours...");
						break;
					}
					case "3": {
						do {
							System.out.println("Entrez la valeur de départ : ");
							s = entreeClavier.readLine();
						} while (Integer.parseInt(s)<0);
						String s1 = "";
						do {
							System.out.println("Entrez la valeur de fin incluse : ");
							s1 = entreeClavier.readLine();
						} while (Integer.parseInt(s)<0);
						s = s + " " + s1;
						message.parametre = s;
						message.commande = Commande.MedPersiInt;
						System.out.println("Recherche en cours...");
						break;
					}
					case "4" : {
						do {
							System.out.println("Entrez la valeur de la persistance : ");
							s = entreeClavier.readLine();
						} while(Integer.parseInt(s) < 0 || Integer.parseInt(s) > 11);
						message.parametre = s;
						message.commande = Commande.OccParti;
						System.out.println("Recherche en cours...");
						break;
					}
					case "5" : {
						message.commande = Commande.OccGen;
						System.out.println("Recherche en cours...");
						break;
					}
					case "6" : {
						message.commande = Commande.MoyPersiGen;
						System.out.println("Recherche en cours...");
						break;
					}
					case "7" : {
						message.commande = Commande.MedPersiGen;
						System.out.println("Recherche en cours...");
						break;
					}
					case "8" : {
						message.commande = Commande.Workers;
						System.out.println("Recherche en cours...");
						break;
					}
				}
				if(!str.equals("9")){//comme 9 est l'option de sortie, rien ne doit se passer lors de son entrée
					sisw.writeObject(message);
					try {
						message = (Message) sisr.readObject();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(message.parametre == null) //le seul numéro qui renvoie un paramètre est 5 donc tant que c est pas 5 on affiche le texte
						System.out.println(message.texte+"\n");
					else {
						long[] tabOcc = (long[]) message.parametre;//si c est 5 on reçoit une tableau de long donc on affiche avec une boucle
						for (int i = 0; i < 12; i++) {
							System.out.println(i + " : " + tabOcc[i]);
						}
						System.out.println();
					}
				}
			} while (!str.equals("9"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}