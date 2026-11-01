package com.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", indexes = {
		@Index(name = "idx_order_items_product_id", columnList = "product_id"),
		@Index(name = "idx_order_items_vendor_id", columnList = "vendor_id")
})
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_product"))
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "vendor_id", foreignKey = @ForeignKey(name = "fk_order_items_vendor"))
	private User vendor;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "line_total", nullable = false, precision = 19, scale = 2)
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	public OrderItem(CustomerOrder order, Product product, User vendor, String productName, BigDecimal unitPrice,
			Integer quantity, BigDecimal lineTotal) {
		this.order = order;
		this.product = product;
		this.vendor = vendor;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.lineTotal = lineTotal;
	}

	public Long getId() {
		return id;
	}

	public CustomerOrder getOrder() {
		return order;
	}

	public Product getProduct() {
		return product;
	}

	public User getVendor() {
		return vendor;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
