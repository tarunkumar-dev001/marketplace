package com.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "price", nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@Column(name = "category", nullable = false, length = 100)
	private String category;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "vendor_id", foreignKey = @ForeignKey(name = "fk_products_vendor"))
	private User vendor;

	protected Product() {
	}

	public Product(String name, String description, BigDecimal price, Integer stockQuantity, String category,
			boolean active) {
		this(name, description, price, stockQuantity, category, active, null);
	}

	public Product(String name, String description, BigDecimal price, Integer stockQuantity, String category,
			boolean active, User vendor) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.stockQuantity = stockQuantity;
		this.category = category;
		this.active = active;
		this.vendor = vendor;
	}

	public void assignVendor(User vendor) {
		this.vendor = vendor;
	}

	public void update(String name, String description, BigDecimal price, Integer stockQuantity, String category,
			boolean active) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.stockQuantity = stockQuantity;
		this.category = category;
		this.active = active;
	}

	public void decrementStock(Integer quantity) {
		if (quantity == null || quantity <= 0 || stockQuantity == null || stockQuantity < quantity) {
			throw new IllegalArgumentException("Insufficient stock");
		}
		stockQuantity -= quantity;
	}

	public void restoreStock(Integer quantity) {
		if (quantity == null || quantity <= 0 || stockQuantity == null) {
			throw new IllegalStateException("Invalid stock restoration");
		}
		try {
			stockQuantity = Math.addExact(stockQuantity, quantity);
		} catch (ArithmeticException exception) {
			throw new IllegalStateException("Stock exceeds supported range", exception);
		}
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Integer getStockQuantity() {
		return stockQuantity;
	}

	public String getCategory() {
		return category;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public User getVendor() {
		return vendor;
	}
}
