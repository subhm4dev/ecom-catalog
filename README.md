# 🔧 E-Commerce Catalog Service

Product Catalog Management Service with Categories, Products, and Variants.

## Port

**8084**

## Endpoints

- `POST /v1/product` - Create product
- `GET /v1/product/{id}` - Get product by ID

## Events

Publishes `ProductCreated` event to Kafka when a product is created.

## Running Locally

```bash
mvn spring-boot:run
```

