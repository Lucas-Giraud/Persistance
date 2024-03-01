

public enum Commande {//Liste de "commandes" qui vont servir de repère pour chaque échange entre le serveur et le(s) worker(s) / client(s)
	Message(""),
	Intervalle(""),
	GivePersi(""),
	MoyPersiInt(""),
	MedPersiInt(""),
	PersiParti(""),
	OccParti(""),
	OccGen(""),
	MoyPersiGen(""),
	MedPersiGen(""),
	Workers("");

	public String texte = "";

	private Commande(String texte) {
		this.texte = texte;
	}
}