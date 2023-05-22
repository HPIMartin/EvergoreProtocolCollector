package dev.schoenberg.evergore.protocolParser;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.google.common.base.*;

public class CsvParser {
	public List<ItemDto> parseCsv(String csvFilePath) throws IOException {
		List<ItemDto> items = new ArrayList<>();

		for (String line : Files.readAllLines(Paths.get(csvFilePath), Charsets.UTF_8)) {
			System.out.println(line);
			if (line.contains("Schwerter;1;10;Eisenbarren")) {
				System.out.println("shit");
			}
			String[] values = line.split(";");

			// Assuming the CSV structure is consistent, you can access the values by their indices
			String itemName = values[0];
			int price = Integer.parseInt("0" + values[1]);
			String category = values[2];
			int amount = Integer.parseInt("0" + values[3]);

			// Create a list to hold the ingredients
			List<IngredientDto> ingredients = new ArrayList<>();

			// Iterate over the ingredient columns (starting from index 4) in pairs
			for (int i = 4; i < values.length; i += 2) {
				if (i + 1 >= values.length) {
					break;  // Skip if there are not enough values for an ingredient pair
				}

				int ingredientAmount = Integer.parseInt("0" + values[i]);
				String ingredientItem = values[i + 1];
				if (ingredientItem.trim().isBlank()) {
					continue;
				}

				ingredients.add(new IngredientDto(ingredientAmount, ingredientItem));
			}

			// Create the ItemDto object and add it to the list
			items.add(new ItemDto(itemName, price, category, amount, ingredients));
		}

		return items;
	}

	public class ItemDto {
		public final String itemName;
		public final int price;
		public final String category;
		public final int amount;
		public final List<IngredientDto> ingredients;

		public ItemDto(String itemName, int price, String category, int amount, List<IngredientDto> ingredients) {
			this.itemName = itemName;
			this.price = price;
			this.category = category;
			this.amount = amount;
			this.ingredients = ingredients;
		}

		public int getIntegerValue() {
			return ingredients.size();
		}

		public String getStringValue() {
			return itemName;
		}

		public String getFirstString() {
			return category;
		}

		public String getSecondString() {
			return itemName;
		}
	}

	public class IngredientDto {
		public final int amount;
		public final String item;

		public IngredientDto(int amount, String item) {
			this.amount = amount;
			this.item = item;
		}
	}
}
