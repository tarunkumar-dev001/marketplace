package com.marketplace.entity;

import com.marketplace.enums.OrderStatus;
import com.marketplace.exception.OrderCancellationNotAllowedException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders", indexes = @Index(
		name = "idx_customer_orders_customer_created_at", columnList = "customer_id, created_at"))
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_orders_customer"))
	private User customer;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
	private String currency;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CustomerOrder() {
	}

	public CustomerOrder(User customer, OrderStatus status, String currency, BigDecimal totalAmount) {
		this.customer = customer;
		this.status = status;
		this.currency = currency;
		this.totalAmount = totalAmount;
	}

	public OrderItem addItem(Product product, User vendor, String productName, BigDecimal unitPrice, Integer quantity,
			BigDecimal lineTotal) {
		OrderItem item = new OrderItem(this, product, vendor, productName, unitPrice, quantity, lineTotal);
		items.add(item);
		return item;
	}

	public void cancel() {
		if (status != OrderStatus.PLACED) {
			throw new OrderCancellationNotAllowedException();
		}
		status = OrderStatus.CANCELLED;
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

	public User getCustomer() {
		return customer;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Long getVersion() {
		return version;
	}

	public List<OrderItem> getItems() {
		return List.copyOf(items);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
