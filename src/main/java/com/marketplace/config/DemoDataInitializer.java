package com.marketplace.config;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.Product;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.CustomerOrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.math.BigDecimal;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataInitializer.class);

	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final CustomerOrderRepository customerOrderRepository;
	private final PasswordEncoder passwordEncoder;
	private final DemoProperties properties;

	public DemoDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
			DemoProperties properties) {
		this(userRepository, null, null, passwordEncoder, properties);
	}

	public DemoDataInitializer(UserRepository userRepository, ProductRepository productRepository,
			CustomerOrderRepository customerOrderRepository, PasswordEncoder passwordEncoder,
			DemoProperties properties) {
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.customerOrderRepository = customerOrderRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int created = 0;
		created += ensureUser(properties.getAdminEmail(), properties.getAdminPassword(), Role.ADMIN) ? 1 : 0;
		created += ensureUser(properties.getVendorEmail(), properties.getVendorPassword(), Role.VENDOR) ? 1 : 0;
		created += ensureUser(properties.getCustomerEmail(), properties.getCustomerPassword(), Role.CUSTOMER) ? 1 : 0;
		ensureDemoCatalogAndOrder();
		LOGGER.info("Demo user initialization complete: {} created, {} existing preserved", created, 3 - created);
	}

	private void ensureDemoCatalogAndOrder() {
		if (productRepository == null || customerOrderRepository == null) {
			return;
		}
		User vendor = userRepository.findByEmail(canonicalizeRequired(properties.getVendorEmail(), "Demo email"))
				.orElseThrow(() -> new IllegalStateException("Demo vendor was not initialized"));
		User customer = userRepository.findByEmail(canonicalizeRequired(properties.getCustomerEmail(), "Demo email"))
				.orElseThrow(() -> new IllegalStateException("Demo customer was not initialized"));

		Product headphones = ensureProduct(vendor, "Wireless Headphones",
				"Comfortable over-ear headphones with active noise cancellation.",
				new BigDecimal("2999.00"), 25, "Electronics");
		Product notebook = ensureProduct(vendor, "Premium Notebook",
				"Lay-flat hardcover notebook for work and study.",
				new BigDecimal("499.00"), 40, "Stationery");

		if (!customerOrderRepository.existsByCustomerId(customer.getId())) {
			CustomerOrder order = new CustomerOrder(customer, OrderStatus.PLACED, "INR",
					headphones.getPrice().add(notebook.getPrice()));
			order.addItem(headphones, vendor, headphones.getName(), headphones.getPrice(), 1,
					headphones.getPrice());
			order.addItem(notebook, vendor, notebook.getName(), notebook.getPrice(), 1,
					notebook.getPrice());
			customerOrderRepository.save(order);
		}
	}

	private Product ensureProduct(User vendor, String name, String description, BigDecimal price, int stock,
			String category) {
		Product existing = productRepository.findByNameAndVendorId(name, vendor.getId());
		if (existing != null) {
			return existing;
		}
		return productRepository.save(new Product(name, description, price, stock, category, true, vendor));
	}

	private boolean ensureUser(String email, String rawPassword, Role role) {
		String canonicalEmail = canonicalizeRequired(email, "Demo email");
		requireRawPassword(rawPassword);

		User existing = userRepository.findByEmail(canonicalEmail).orElse(null);
		if (existing != null) {
			if (!existing.isActive() && properties.isActivateExistingUsers()) {
				existing.setActive(true);
				userRepository.save(existing);
			}
			return false;
		}

		User created = new User(canonicalEmail, passwordEncoder.encode(rawPassword), role, true);
		userRepository.save(created);
		return true;
	}

	private static String canonicalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(fieldName + " must be configured");
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static void requireRawPassword(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalStateException("Demo password must be configured");
		}
	}
}
