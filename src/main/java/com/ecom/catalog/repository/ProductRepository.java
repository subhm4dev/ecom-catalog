package com.ecom.catalog.repository;

import com.ecom.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    /**
     * Find product by SKU and tenant ID (for SKU uniqueness check)
     * 
     * @param sku SKU
     * @param tenantId Tenant ID
     * @return Optional Product
     */
    Optional<Product> findBySkuAndTenantId(String sku, UUID tenantId);
    
    /**
     * Find active product by ID and tenant ID
     * 
     * @param id Product ID
     * @param tenantId Tenant ID
     * @return Optional Product (only if not deleted)
     */
    Optional<Product> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);
    
    /**
     * Find product by ID (including deleted)
     * Used by admins for recovery/audit
     * 
     * @param id Product ID
     * @return Optional Product (may be deleted)
     */
    @Override
    @NonNull
    Optional<Product> findById(@NonNull UUID id);
    
    /**
     * Find all active products for a seller within a tenant
     * 
     * @param sellerId Seller ID
     * @param tenantId Tenant ID
     * @return List of active products
     */
    List<Product> findBySellerIdAndTenantIdAndDeletedFalse(UUID sellerId, UUID tenantId);
    
    /**
     * Search products with filters
     * 
     * @param tenantId Tenant ID
     * @param categoryId Optional category ID
     * @param minPrice Optional minimum price
     * @param maxPrice Optional maximum price
     * @param query Optional search query (name or description)
     * @param pageable Pagination
     * @return Page of products
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.tenantId = :tenantId AND " +
           "p.deleted = false AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(
        @Param("tenantId") UUID tenantId,
        @Param("categoryId") UUID categoryId,
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        @Param("query") String query,
        Pageable pageable
    );
    
    /**
     * Find all active products by category
     * 
     * @param categoryId Category ID
     * @param tenantId Tenant ID
     * @param pageable Pagination
     * @return Page of products
     */
    Page<Product> findByCategoryIdAndTenantIdAndDeletedFalse(
        UUID categoryId,
        UUID tenantId,
        Pageable pageable
    );
    
    /**
     * Count products by category (for validation before category deletion)
     * 
     * @param tenantId Tenant ID
     * @param categoryId Category ID
     * @return Count of active products in this category
     */
    long countByTenantIdAndCategoryIdAndDeletedFalse(UUID tenantId, UUID categoryId);
}

