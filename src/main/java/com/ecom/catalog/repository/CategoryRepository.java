package com.ecom.catalog.repository;

import com.ecom.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Category entity
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    /**
     * Find all categories for a tenant
     * 
     * @param tenantId Tenant ID
     * @return List of categories
     */
    List<Category> findByTenantId(UUID tenantId);
    
    /**
     * Find categories by parent ID
     * 
     * @param parentId Parent category ID
     * @param tenantId Tenant ID
     * @return List of child categories
     */
    List<Category> findByParentIdAndTenantId(UUID parentId, UUID tenantId);
    
    /**
     * Find category by name and tenant ID
     * 
     * @param name Category name
     * @param tenantId Tenant ID
     * @return Optional Category
     */
    java.util.Optional<Category> findByNameAndTenantId(String name, UUID tenantId);
}

