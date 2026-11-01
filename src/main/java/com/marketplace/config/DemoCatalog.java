package com.marketplace.config;

import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public final class DemoCatalog {
	private DemoCatalog() {}

	public static String deriveEmail(String email, int number) {
		int at = email.indexOf('@');
		if (at <= 0 || at == email.length() - 1) {
			throw new IllegalArgumentException("Invalid demo email");
		}
		return email.substring(0, at) + "+" + number + email.substring(at);
	}

	public static void seedProducts(List<User> vendors, ProductRepository repository) {
		String[] categories = {"Electronics", "Fashion", "Home & Kitchen", "Books", "Sports", "Beauty"};
		String[] prefixes = {"Essential", "Premium", "Classic", "Urban", "Smart", "Everyday"};
		for (int i = 0; i < 36; i++) {
			String category = categories[i % categories.length];
			String name = prefixes[i / categories.length] + " " + category + " Item " + (i % categories.length + 1);
			User vendor = vendors.get(i % vendors.size());
			if (repository.findByNameAndVendorId(name, vendor.getId()) != null) continue;
			Product product = new Product(name, "A carefully selected marketplace product from " + category + ".", 
					BigDecimal.valueOf(299 + (i % 12) * 250L).setScale(2), 10 + i % 30, category, true, vendor);
			String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
			product.setImageUrl("https://picsum.photos/seed/" + slug + "/640/480");
			repository.save(product);
		}
	}
}
