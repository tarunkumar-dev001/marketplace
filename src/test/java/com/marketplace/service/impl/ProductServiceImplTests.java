package com.marketplace.service.impl;

import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.ProductNotFoundException;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceImplTests {

	private ProductRepository productRepository;
	private UserRepository userRepository;
	private ProductService productService;

	@BeforeEach
	void setUp() {
		productRepository = mock(ProductRepository.class);
		userRepository = mock(UserRepository.class);
		productService = new ProductServiceImpl(productRepository, userRepository);
	}

	@Test
	void vendorCreateAssignsThePersistedAuthenticatedVendorAsOwner() {
		User vendor = activeUser(7L, Role.VENDOR);
		Product product = product("Name", "Description", "10.00", 3, "Category", true);
		when(userRepository.findById(7L)).thenReturn(Optional.of(vendor));
		when(productRepository.save(product)).thenReturn(product);

		Product result = productService.create(product, 7L, Role.VENDOR);

		assertSame(product, result);
		assertSame(vendor, product.getVendor());
		verify(userRepository).findById(7L);
		verify(productRepository).save(product);
	}

	@Test
	void vendorWritesRejectMissingInactiveOrNonVendorAccounts() {
		when(userRepository.findById(7L)).thenReturn(Optional.empty());
		assertThrows(AccessDeniedException.class, () -> createAsVendor());

		User inactiveVendor = inactiveUser(7L, Role.VENDOR);
		when(userRepository.findById(7L)).thenReturn(Optional.of(inactiveVendor));
		assertThrows(AccessDeniedException.class, () -> createAsVendor());

		User customer = activeUser(7L, Role.CUSTOMER);
		when(userRepository.findById(7L)).thenReturn(Optional.of(customer));
		assertThrows(AccessDeniedException.class, () -> createAsVendor());

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void adminWritesRejectMissingInactiveOrNonAdminAccounts() {
		when(userRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(AccessDeniedException.class, () -> createAsAdmin());

		User inactiveAdmin = inactiveUser(1L, Role.ADMIN);
		when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveAdmin));
		assertThrows(AccessDeniedException.class, () -> createAsAdmin());

		User vendor = activeUser(1L, Role.VENDOR);
		when(userRepository.findById(1L)).thenReturn(Optional.of(vendor));
		assertThrows(AccessDeniedException.class, () -> createAsAdmin());

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void suppliedRoleMustMatchThePersistedActorRole() {
		User vendor = activeUser(7L, Role.VENDOR);
		when(userRepository.findById(7L)).thenReturn(Optional.of(vendor));
		assertThrows(AccessDeniedException.class, () -> productService.create(product("Name", "Description", "10.00", 3, "Category", true),
				7L, Role.ADMIN));

		User admin = activeUser(1L, Role.ADMIN);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		assertThrows(AccessDeniedException.class, () -> productService.create(product("Name", "Description", "10.00", 3, "Category", true),
				1L, Role.VENDOR));

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void invalidActorIdsAndNullRolesAreRejectedBeforeRepositoryAccess() {
		Product product = product("Name", "Description", "10.00", 3, "Category", true);

		assertThrows(AccessDeniedException.class, () -> productService.create(product, null, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.create(product, 0L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.create(product, -1L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.create(product, 7L, null));

		verifyNoInteractions(userRepository, productRepository);
	}

	@Test
	void invalidActorInputsBlockUpdateAndDeleteBeforeProductLookup() {
		Product product = product("Name", "Description", "10.00", 3, "Category", true);

		assertThrows(AccessDeniedException.class, () -> productService.update(1L, product, null, Role.ADMIN));
		assertThrows(AccessDeniedException.class, () -> productService.delete(1L, 0L, Role.ADMIN));
		assertThrows(AccessDeniedException.class, () -> productService.update(1L, product, 1L, null));

		verifyNoInteractions(userRepository, productRepository);
	}

	@Test
	void adminCreateLeavesOwnershipNull() {
		User admin = activeUser(1L, Role.ADMIN);
		Product product = product("Name", "Description", "10.00", 3, "Category", true, activeUser(8L, Role.VENDOR));
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(productRepository.save(product)).thenReturn(product);

		Product result = productService.create(product, 1L, Role.ADMIN);

		assertSame(product, result);
		assertNull(product.getVendor());
		verify(userRepository).findById(1L);
		verify(productRepository).save(product);
	}

	@Test
	void customersCannotWriteProductsWhenTheServiceIsCalledDirectly() {
		User customer = activeUser(3L, Role.CUSTOMER);
		when(userRepository.findById(3L)).thenReturn(Optional.of(customer));

		assertThrows(AccessDeniedException.class, () -> productService.create(product("Name", "Description", "10.00", 3, "Category", true),
				3L, Role.CUSTOMER));
		assertThrows(AccessDeniedException.class, () -> productService.update(1L,
				product("Name", "Description", "10.00", 3, "Category", true), 3L, Role.CUSTOMER));
		assertThrows(AccessDeniedException.class, () -> productService.delete(1L, 3L, Role.CUSTOMER));

		verifyNoInteractions(productRepository);
	}

	@Test
	void getByIdAndGetAllRetainRepositoryBehavior() {
		Product product = product("Name", "Description", "10.00", 3, "Category", true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productRepository.findAll()).thenReturn(List.of(product));

		assertSame(product, productService.getById(1L));
		assertEquals(List.of(product), productService.getAll());
		verify(productRepository).findById(1L);
		verify(productRepository).findAll();
	}

	@Test
	void missingProductRetainsNotFoundBehaviorForValidatedAdmin() {
		User admin = activeUser(1L, Role.ADMIN);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> productService.getById(99L));
		assertThrows(ProductNotFoundException.class,
				() -> productService.update(99L, product("Name", "Description", "10.00", 3, "Category", true), 1L, Role.ADMIN));
		assertThrows(ProductNotFoundException.class, () -> productService.delete(99L, 1L, Role.ADMIN));
		verify(productRepository, never()).save(any(Product.class));
		verify(productRepository, never()).delete(any(Product.class));
	}

	@Test
	void vendorCanUpdateAndDeleteOwnProduct() {
		User vendor = activeUser(7L, Role.VENDOR);
		Product existingProduct = product("Original", "Original description", "10.00", 3, "Books", true, vendor);
		Product updateProduct = product("Updated", "Updated description", "15.50", 8, "Media", false);
		when(userRepository.findById(7L)).thenReturn(Optional.of(vendor));
		when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
		when(productRepository.save(existingProduct)).thenReturn(existingProduct);

		Product result = productService.update(1L, updateProduct, 7L, Role.VENDOR);
		productService.delete(1L, 7L, Role.VENDOR);

		assertSame(existingProduct, result);
		assertEquals("Updated", existingProduct.getName());
		assertEquals("Updated description", existingProduct.getDescription());
		assertEquals(new BigDecimal("15.50"), existingProduct.getPrice());
		assertEquals(8, existingProduct.getStockQuantity());
		assertEquals("Media", existingProduct.getCategory());
		assertEquals(false, existingProduct.isActive());
		assertSame(vendor, existingProduct.getVendor());
		verify(productRepository).save(existingProduct);
		verify(productRepository).delete(existingProduct);
	}

	@Test
	void vendorCannotUpdateOrDeleteAnotherVendorsOrLegacyUnownedProduct() {
		User actingVendor = activeUser(7L, Role.VENDOR);
		Product otherVendorProduct = product("Original", "Description", "10.00", 3, "Books", true,
				activeUser(8L, Role.VENDOR));
		Product legacyProduct = product("Legacy", "Description", "10.00", 3, "Books", true);
		when(userRepository.findById(7L)).thenReturn(Optional.of(actingVendor));
		when(productRepository.findById(1L)).thenReturn(Optional.of(otherVendorProduct));
		when(productRepository.findById(2L)).thenReturn(Optional.of(legacyProduct));

		assertThrows(AccessDeniedException.class, () -> productService.update(1L,
				product("Updated", "Description", "15.00", 2, "Books", true), 7L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.update(2L,
				product("Updated", "Description", "15.00", 2, "Books", true), 7L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.delete(1L, 7L, Role.VENDOR));
		assertThrows(AccessDeniedException.class, () -> productService.delete(2L, 7L, Role.VENDOR));

		verify(productRepository, never()).save(any(Product.class));
		verify(productRepository, never()).delete(any(Product.class));
	}

	@Test
	void activeAdminCanUpdateAndDeleteOwnedAndUnownedProducts() {
		User admin = activeUser(1L, Role.ADMIN);
		Product ownedProduct = product("Owned", "Description", "10.00", 3, "Books", true, activeUser(7L, Role.VENDOR));
		Product unownedProduct = product("Legacy", "Description", "10.00", 3, "Books", true);
		when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(productRepository.findById(1L)).thenReturn(Optional.of(ownedProduct));
		when(productRepository.findById(2L)).thenReturn(Optional.of(unownedProduct));
		when(productRepository.save(ownedProduct)).thenReturn(ownedProduct);
		when(productRepository.save(unownedProduct)).thenReturn(unownedProduct);

		productService.update(1L, product("Updated owned", "Description", "15.00", 2, "Books", true), 1L, Role.ADMIN);
		productService.update(2L, product("Updated legacy", "Description", "15.00", 2, "Books", true), 1L, Role.ADMIN);
		productService.delete(1L, 1L, Role.ADMIN);
		productService.delete(2L, 1L, Role.ADMIN);

		assertEquals("Updated owned", ownedProduct.getName());
		assertEquals("Updated legacy", unownedProduct.getName());
		verify(productRepository).delete(ownedProduct);
		verify(productRepository).delete(unownedProduct);
	}

	private void createAsVendor() {
		productService.create(product("Name", "Description", "10.00", 3, "Category", true), 7L, Role.VENDOR);
	}

	private void createAsAdmin() {
		productService.create(product("Name", "Description", "10.00", 3, "Category", true), 1L, Role.ADMIN);
	}

	private User activeUser(long id, Role role) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getRole()).thenReturn(role);
		when(user.isActive()).thenReturn(true);
		return user;
	}

	private User inactiveUser(long id, Role role) {
		User user = activeUser(id, role);
		when(user.isActive()).thenReturn(false);
		return user;
	}

	private Product product(String name, String description, String price, int stockQuantity, String category,
			boolean active) {
		return product(name, description, price, stockQuantity, category, active, null);
	}

	private Product product(String name, String description, String price, int stockQuantity, String category,
			boolean active, User vendor) {
		return new Product(name, description, new BigDecimal(price), stockQuantity, category, active, vendor);
	}
}
