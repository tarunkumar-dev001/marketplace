package com.marketplace.repository;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	boolean existsByCustomerId(Long customerId);
	boolean existsByItemsProductId(Long productId);

	Page<CustomerOrder> findByCustomerId(Long customerId, Pageable pageable);

	@EntityGraph(attributePaths = {"items", "items.product", "items.vendor", "customer"})
	Optional<CustomerOrder> findByIdWithItems(Long id);

	@EntityGraph(attributePaths = {"items", "items.product"})
	Optional<CustomerOrder> findByIdAndCustomerId(Long id, Long customerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"items", "items.product"})
	@Query("select o from CustomerOrder o where o.id = :id and o.customer.id = :customerId")
	Optional<CustomerOrder> findByIdAndCustomerIdForUpdate(@Param("id") Long id, @Param("customerId") Long customerId);

	@EntityGraph(attributePaths = {"items", "items.product"})
	@Query("select distinct o from CustomerOrder o where o.customer.id = :customerId and o.id in :orderIds")
	List<CustomerOrder> findByCustomerIdAndIdInWithItems(@Param("customerId") Long customerId,
			@Param("orderIds") List<Long> orderIds);

	@Query("select distinct o from CustomerOrder o join o.items i where i.vendor.id = :vendorId")
	Page<CustomerOrder> findDistinctByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

	@EntityGraph(attributePaths = {"items", "items.product"})
	@Query("select distinct o from CustomerOrder o join o.items i where i.vendor.id = :vendorId and o.id in :orderIds")
	List<CustomerOrder> findByVendorIdAndIdInWithItems(@Param("vendorId") Long vendorId,
			@Param("orderIds") List<Long> orderIds);

	@EntityGraph(attributePaths = {"items", "items.product"})
	@Query("select o from CustomerOrder o")
	Page<CustomerOrder> findAllWithItems(Pageable pageable);

	@EntityGraph(attributePaths = {"items", "items.product", "items.vendor", "customer"})
	@Query("select o from CustomerOrder o where o.id in :ids")
	List<CustomerOrder> findAllWithItemsById(@Param("ids") List<Long> ids);

	@Query("select count(distinct o) from CustomerOrder o join o.items i where i.vendor.id = :vendorId")
	long countVendorOrders(@Param("vendorId") Long vendorId);

	@Query("select count(distinct o) from CustomerOrder o join o.items i where i.vendor.id = :vendorId and o.status = :status")
	long countVendorOrdersByStatus(@Param("vendorId") Long vendorId, @Param("status") OrderStatus status);

	@Query("select coalesce(sum(i.quantity), 0) from CustomerOrder o join o.items i where i.vendor.id = :vendorId and o.status <> com.marketplace.enums.OrderStatus.CANCELLED")
	long sumVendorUnits(@Param("vendorId") Long vendorId);

	@Query("select coalesce(sum(i.lineTotal), 0) from CustomerOrder o join o.items i where i.vendor.id = :vendorId and o.status <> com.marketplace.enums.OrderStatus.CANCELLED")
	java.math.BigDecimal sumVendorSales(@Param("vendorId") Long vendorId);

	@Query("select count(o) from CustomerOrder o")
	long countAllOrders();

	@Query("select count(o) from CustomerOrder o where o.status = :status")
	long countAllOrdersByStatus(@Param("status") OrderStatus status);

	@Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.order.status <> com.marketplace.enums.OrderStatus.CANCELLED")
	long sumAllUnits();

	@Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status <> com.marketplace.enums.OrderStatus.CANCELLED")
	java.math.BigDecimal sumAllSales();
}
