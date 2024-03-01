

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;

public class ServeurMC {
	static int port = 8080;
	public static ArrayList<ConnexionWorker> workers = new ArrayList<>();

	public static void main (String[] args) throws Exception {
		ServerSocket ss = new ServerSocket(port);//créé un serveur socket qui permet la connexion de plusieurs clients / workers
		System.out.println("SOCKET ECOUTE CREE => " + port);
		TabRes index1 = new TabRes(BigInteger.valueOf(1000000), true);//créé deux instances de type TabRes qui vont stocker les résultats envoyés par les Workers
		TabRes index2 = new TabRes(BigInteger.valueOf(2000000), false);
		Distributeur d = new Distributeur();//créé une instance de type Distributeur qui donne les intervalles de nombre à calculer
		boolean arreter = false;
		while (!arreter) {
			try {
				Socket soc = ss.accept();//récupère le socket fait à partir de la connexion entre le serveur et un client / worker
				BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));//créé un flux de lecture et lis le premier message envoyé par le connecté
				String s = br.readLine();
				if(s.equals("worker")){//si le message est worker, créé et lance un thread qui gère la connexion entre le worker et le serveur
					ConnexionWorker cw = new ConnexionWorker(soc, index1, index2, d);
					System.out.println("NOUVELLE CONNEXION WORKER- SOCKET => " + soc);
					cw.start();
					workers.add(cw);//ce thread est ajouté à la liste workers qui sert à informer le client des worker actif quand il en fait la demande 
				}else if (s.equals("client")) {//sinon si le message est client, créé et lance un thread qui gère la connexion entre le serveur et le worker
					ConnexionClient cc = new ConnexionClient(soc);
					cc.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for(int i = 0; i < workers.size(); i++){
			workers.get(i).join();
		}
		ss.close();
	}

}


class ConnexionWorker extends Thread {//thread qui gère la communication entre le serveur et le worker
	private Socket s;
	private ObjectInputStream sisr;
	private ObjectOutputStream sisw;
	private TabRes index1;
	private TabRes index2;
	private Distributeur d;

	public ConnexionWorker (Socket s, TabRes index1, TabRes index2, Distributeur d) {//Constructeur qui récupère les deux index, le socket et le distributeur défini dans le serveur
		this.s = s;
		this.index1 = index1;
		this.index2 = index2;
		this.d = d;
		try {//créé deux flux de lecture et d'écriture
			sisr = new ObjectInputStream(s.getInputStream());
			sisw = new ObjectOutputStream(s.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendInt (Message message) {//méthode qui envoie au worker un intervalle
		message = new Message(null, d.getNumber(), "");
		try {
			sisw.writeObject(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void storeRes (Message message) {//méthode qui qui implémentes les deux index
		List<String> res = Collections.synchronizedList((List<String>) message.parametre);//créé une liste synchronisée à partir de la liste passée en paramètre du message
		for (int i = 0; i < res.size(); i++) {
			String str = (String) res.get(i);//récupère chaque éléments de la liste qui sont de la forme "BigInteger Integer"
			//System.out.println(str);
			BigInteger bi = new BigInteger(str.split(" ")[0]);//sépare cette chaîne en deux partie
			int j = Integer.parseInt(str.split(" ")[1]);
			if (index1.isFilling()) {//Si c'est le premier index qui est en train d'etre remplie 
				if (bi.compareTo(index1.getMax()) < 0) {//et que le BigInteger est plus petit que le maximum de l'index
					index1.ajoute(bi, j); // le stocke dans index 1 
				} else {
					index2.ajoute(bi, j);// sinon dans index 2
				}
			} else if (index2.isFilling()) {//si index1 est pas sen train d'etre remplie, suis le meme principe que juste avant
				if (bi.compareTo(index2.getMax()) < 0) {
					index2.ajoute(bi, j);
				} else {
					index1.ajoute(bi, j);
				}
			}
		}
	}

	public void run() {
		int currentPage = 0;//numéro de la prochaine page à stocker
		try {
			while (true) {
				Message message =(Message) sisr.readObject();//lis le message envoyé par le worker
				if (message.texte.equals("END")) {//si le texte du message END, enlève le worker de la liste et interrompt la boucle while
					ServeurMC.workers.remove(this);
					break;
				} else if (message.commande == Commande.Intervalle) {//sinon si la commande est Intervalle, appelle la fonction sendInt
					sendInt(message);
				} else if (message.commande == Commande.GivePersi) {//sinon si la commance est GivePersi, appelle la fonction storeRes
					//System.out.println("+10M");
					storeRes(message);
				}
				//System.out.println("i1 : "+index1.getTaille()+" "+index1.isFull()+"\ni2 : "+index2.getTaille()+" "+index2.isFull());
				if (index1.isFull() && index1.isFilling()) {//si l'index1 était en train d'être rempli et qu'il est plein, créé et lance un thread Storage  
					Storage storage = new Storage(index1, currentPage);
					storage.start();
					index1.setFilling(false);//échange l'état de remplissage des 2 index pour que l'un puisse être stocké sans être modifié
					index2.setFilling(true);
					index1.setMax(index1.getMax().add(BigInteger.valueOf(2000000)));//ajoute 2 millions à la valeur max de l'index et 1 à la page Courante
					System.out.println("switch à index2");
					currentPage++;
				} else if (index2.isFull() && index2.isFilling()) {//même principe pour index2
					Storage storage = new Storage(index2, currentPage);
					storage.start();
					index2.setFilling(false);
					index1.setFilling(true);
					index2.setMax(index2.getMax().add(BigInteger.valueOf(2000000)));
					System.out.println("switch à index1");
					currentPage++;
				}
			}
			sisr.close();//ferme tous les flux
			sisw.close();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ConnexionClient extends Thread {//thread qui gère la communication entre le serveur et le client
	private Socket s;
	private ObjectInputStream sisr;
	private ObjectOutputStream sisw;
	public TabRes index3 = new TabRes(null, false);

	public ConnexionClient (Socket s) {//Constructeur qui récupère le socket et créé des flux de lecture et d'écriture
		this.s = s;
		try {
			sisr = new ObjectInputStream(s.getInputStream());
			sisw = new ObjectOutputStream(s.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getPersiParti (BigInteger bi) {//méthode qui calcule la page où se situe le nombre donné en paramètre 
		BigInteger d = bi.divide(BigInteger.valueOf(1000000));
		long Page = (int) d.longValue();
		Storage storage = new Storage();
		index3.setIndex(storage.readFile(Page));//récupère la bonne page dans index3
		Integer p = -1;
		if (index3.getTaille() != 0) {
			p = index3.getIndex().get(bi);
		}
		index3.clear();
		return p;//renvoie la persistance du nombre sauf si l'index n'a pas été trouvé dans le dossier
	}

	public double ordMoy (Message message) {//méthode qui ordonne un intervalle passé en paramètre du message
		String str = (String) message.parametre;
		BigInteger n1 = new BigInteger(str.split(" ")[0]);
		BigInteger n2 = new BigInteger(str.split(" ")[1]);
		int cmp = n1.compareTo(n2);
		double p = -1;
		if (cmp == -1) {
			p = getMoyPersiInt(n1,n2);//si n1 < n2, appelle la fonction getMmoyPersiInt normalement
		} else if (cmp == 0){
			p = (double) getPersiParti(n2);//si l'intervalle a juste 1 nombre c'est la persistance de ce nombre qui va etre cherchée
		} else {
			p = getMoyPersiInt(n2,n1);//si n2 < n1, appelle la fonction getMoyPersiInt avec n2 en premier car l'interval à été donné à l'envers
		}
		return p;
	}

	public double getMoyPersiInt (BigInteger dep, BigInteger fin) {//fonction qui renvoie la persistance moyenne d'un intervalle
		BigInteger b1 = dep.divide(BigInteger.valueOf(1000000));//récupère les pages où se situe le début et la fin de l'intervalle
		BigInteger b2 = fin.divide(BigInteger.valueOf(1000000));
		long Page1 = (int) b1.longValue();
		long Page2 = (int) b2.longValue();
		Storage storage = new Storage();//créé une instance de storage
		double s = 0;
		double cpt = 0;
		if (Page1 == Page2) {//si l'intervalle se situe sur 1 seule page
			index3.setIndex(storage.readFile(Page1));
			if (index3.getTaille() != 0) {
				for (BigInteger i = dep; i.compareTo(fin) <= 0; i = i.add(BigInteger.ONE)) {//parcours l'intervalle et ajoute les persistances à s
					s+=index3.get(i);
					cpt++;//compte le nombre de valeurs dans l'intervalle
				}
				index3.clear();
				return s/cpt;
			}
		} else {//si les 2 pages ne sont pas identiques
			BigInteger j = dep;
			for (long i = Page1; i <= Page2; i++) {//parcours toutes les pages couvertes par l'intervalle et imcrémente cpt et s de la meme manière que précédement
				BigInteger maxPage = BigInteger.valueOf(1000000*i+999999);
				index3.setIndex(storage.readFile(i));
				if (index3.getTaille() != 0) {
					while ((j.compareTo(fin)<=0) && (j.compareTo(maxPage)<=0)) {
						s += index3.get(j);
						j = j.add(BigInteger.ONE);
						cpt++;
					}

				} else {
					index3.clear();
					return -1;//renvoie -1 si la page n'a pas été trouvée
				}
			}
			index3.clear();
			return s/cpt;
		}
		index3.clear();
		return -1;
	}

	public int ordMed (Message message) {//méthode qui ordonne un intervalle passé en paramètre du message
		String str = (String) message.parametre;
		BigInteger n1 = new BigInteger(str.split(" ")[0]);
		BigInteger n2 = new BigInteger(str.split(" ")[1]);
		int cmp = n1.compareTo(n2);
		int p = -1;
		if(cmp <= 0){
			p = getMedPersiInt(n1,n2);//si n1 < n2
		}else {
			p = getMedPersiInt(n2,n1);//si n2 < n1
		}
		return p;
	}

	public long[] getTabOcc (BigInteger dep, BigInteger fin) {// méthode qui récupère et renvoie un tableau contenant les occurences de chaque persistances sur un intervalle donné
		//utilise la meme méthode de parcours des pages que getMoyPersiInt
		long[] tabOcc = new long[12];
		for (int i = 0; i < 12; i++) {
			tabOcc[i] = 0;
		}
		BigInteger b1 = dep.divide(BigInteger.valueOf(1000000));
		BigInteger b2 = fin.divide(BigInteger.valueOf(1000000));
		long Page1 = (int) b1.longValue();
		long Page2 = (int) b2.longValue();
		Storage storage = new Storage();
		if (Page1 == Page2) {
			index3.setIndex(storage.readFile(Page1));
			if (index3.getTaille() != 0) {
				for (BigInteger i = dep; i.compareTo(fin) <= 0; i = i.add(BigInteger.ONE)) {
					tabOcc[index3.get(i)]++;
				}
				index3.clear();
				return tabOcc;
			}
		} else {
			BigInteger j = dep;
			for (long i = Page1; i <= Page2; i++) {
				BigInteger maxPage = BigInteger.valueOf(1000000*i+999999);
				index3.setIndex(storage.readFile(i));
				if (index3.getTaille() != 0) {
					while ((j.compareTo(fin) <= 0) && (j.compareTo(maxPage) <= 0)) {
						tabOcc[index3.get(j)]++;
						j = j.add(BigInteger.ONE);
					}
				} else {
					index3.clear();
					return null;
				}
			}
			index3.clear();
			return tabOcc;
		}
		index3.clear();
		return null;
	}

	public int getMedPersiInt (BigInteger dep, BigInteger fin) {//récupère et renvoie la valeur de la persistance médiane d'un intervalle donnée
		long[] tabOcc = getTabOcc(dep, fin);
		if(tabOcc==null){
			return -1;
		}
		long s = 0;
		for (int i = 0;  i < 12; i++) {//calcul le total de valeur dans l'intervalle
			s += tabOcc[i];
		}
		int i = 0;
		long med = 0;
		if(s % 2 == 0)//calcul la moitié des valeurs pour trouver la valeur du milieu de l'intervalle
			med = s/2;
		else
			med = (s+1)/2;
		long cpt = tabOcc[0];
		while (cpt < med){//cherche quelle persistance correspond à la médiane
			i++;
			cpt += tabOcc[i];
		}
		return i;
	}

	public int lastPage() {//méthode qui cherche quelle est la dernière page stockée
		int Page = -1;
		File file;
		do {
			Page++;
			file = new File("./Storage/" + Page + ".ser");
		} while (file.exists());
		Page--;
		return Page;
	}

	public void run() {
		try {
			while (true) {
				Message message = (Message) sisr.readObject();//lis le message envoyé par le Client
				if (message.texte.equals("END"))//si le texte contient END, interrompt la boucle while
					break;
				if (message.commande == Commande.PersiParti) {//si la commande du message est PersiParti, appelle la fpntcion getPersiParti et renvoie un message avec un texte différent suivant que la valeur a été calculée ou non
					int p = getPersiParti(new BigInteger((String) message.parametre));
					if (p == -1) {
						message = new Message(null, null, "La persistance demandee est n'a pas encore été calculée.\n");
					} else {
						message = new Message(null, null, "La persistance demandee est de " + p + "\n");
					}
				} else if (message.commande == Commande.MoyPersiInt) {//si la commande est MoyPersiInt, appelle la fonction ordMoy et renvoie un message avec un texte différent si une des valeurs de l'intervalle a été calculée ou non
					double p = ordMoy(message);
					if (p == -1) 
						message = new Message(null, null, "Au moins une des valeurs de l'intervalle demandé n'a pas encore été calculée");
					else 
						message = new Message(null, null, "La moyenne des persistances dans l'intervalle demandé est " + p);
				} else if (message.commande == Commande .MedPersiInt) {//si la commande est MedPersiInt, appelle la fonction ordMedet renvoie un message avec un texte différent si une des valeurs de l'intervalle a été calculée ou non
					int p = ordMed(message);
					if (p == -1) 
						message = new Message(null, null, "Au moins une des valeurs de l'intervalle demandé n'a pas encore été calculée");
					else
						message = new Message(null, null, "La persistance médiane dans l'intervalle demandé est " + p);
				}else if (message.commande == Commande.OccParti) {//si la commande est OccParti
					int Page = lastPage();//récupère la dernière page stockée
					BigInteger bi = BigInteger.valueOf(Page*1000000+999999);
					long[] tabOcc = getTabOcc(BigInteger.ZERO, bi);//récupère toutes les occurences des persistances calculées de 0 jusqu'a la fin de la page
					int n = Integer.parseInt((String) message.parametre);
					message = new Message(null, null, "Il y " + tabOcc[n] + " nombres ayant pour persistance " + n);//renvoie un message avec le nombre d'occurences de la persistance demandée
			 	} else if (message.commande == Commande.OccGen){//si la commande est OccGen, récupère toute les occurences des persistances calculées de 0 jusqu'a la fin de la page et les renvoie
					int Page = lastPage();
					BigInteger bi = BigInteger.valueOf(Page*1000000+999999);
					long[] tabOcc = getTabOcc(BigInteger.ZERO, bi);
					message = new Message(null, tabOcc, "");
				} else if (message.commande == Commande.MoyPersiGen) {//si la commande est MoyPersiGen, récupère la moyenne des persistances calculées jusqu'à la dernière page et en renvoie la moyenne de
					int Page = lastPage();
					BigInteger bi = BigInteger.valueOf(Page*1000000+999999);
					double p = ordMoy(new Message(null, 0 + " " + bi, ""));
					message = new Message(null, null, "La moyenne des persistances calculée jusqu'à maintenat est " + p);
				} else if (message.commande == Commande.MedPersiGen) {//si la commande est MedPersiGen, fait la même chose que MoyPersiInt mais avec la médiane
					int Page = lastPage();
					BigInteger bi = BigInteger.valueOf(Page*1000000+999999);
					int p = ordMed(new Message(null, 0 + " " + bi, ""));
					message = new Message(null, null, "La persistance médiane dans l'intervalle demandé est " + p);
				} else if (message.commande == Commande.Workers){//si la commande est workers, renvoie la liste des workers de serveurMC
					message = new Message(null, null, "Il y a " + ServeurMC.workers.size() + " workers actifs :" + ServeurMC.workers);
				}
				sisw.writeObject(message);
			}
			sisr.close();//ferme tous les flux
			sisw.close();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}