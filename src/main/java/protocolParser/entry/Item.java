package protocolParser.entry;

public class Item {
	public final int quantity;
	public final String name;
	public final int quality;

	public Item(int quantity, String name, int quality) {
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
	}

	public String toString(String separator) {
		return quantity + separator + name + separator + quality;
	}
}
