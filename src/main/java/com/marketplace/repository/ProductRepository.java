package com.marketplace.repository;

import com.marketplace.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			select p from Product p
			where p.active = true
			and (:category is null or lower(p.category) = lower(:category))
			and (:search is null or lower(p.name) like lower(concat('%', :search, '%')))
			""")
	Page<Product> findPublicProducts(@Param("category") String category, @Param("search") String search,
			Pageable pageable);

	Page<Product> findByVendorId(Long vendorId, Pageable pageable);

	boolean existsByNameAndVendorId(String name, Long vendorId);

	Product findByNameAndVendorId(String name, Long vendorId);

	@Query(value = "select p from Product p left join fetch p.vendor", countQuery = "select count(p) from Product p")
	Page<Product> findAllForAdmin(Pageable pageable);

	Optional<Product> findByIdAndActiveTrue(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id and p.active = true")
	Optional<Product> findByIdAndActiveTrueForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
