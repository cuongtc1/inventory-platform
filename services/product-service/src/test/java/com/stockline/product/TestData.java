package com.stockline.product;

import com.stockline.common.entity.BaseEntity;
import com.stockline.product.constant.ProductStatus;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.Supplier;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

/**
 * Ready-made entities for tests, so each test class doesn't write its own helpers.
 * Use with a static import: {@code import static com.stockline.product.TestData.category;}
 *
 * <p>Methods with an {@code id} are for unit tests, where no database generates ids; pass {@code null} (or use
 * the methods without an id) for repository tests, where the entity is saved and gets a real id.
 */
public final class TestData {

	private TestData() {
	}

	public static Category category(String name) {
		return new Category(name, null);
	}

	public static Category category(Long id, String name, Category parent) {
		return withId(new Category(name, parent), id);
	}

	public static Supplier supplier(String name) {
		return new Supplier(name, null, null, null);
	}

	public static Supplier supplier(Long id, String name, String contactName, String email, String phone) {
		return withId(new Supplier(name, contactName, email, phone), id);
	}

	/**
	 * Like {@link #product(String, Category, Supplier)}, without a supplier.
	 */
	public static Product product(String sku, Category category) {
		return product(sku, category, null);
	}

	/**
	 * An ACTIVE product with cost 8.00, price 12.00 and reorder level 20; the name is "Product &lt;sku&gt;".
	 */
	public static Product product(String sku, Category category, Supplier supplier) {
		return new Product(sku, "Product " + sku, category, supplier,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE);
	}

	/**
	 * Sets the private {@code id} that the database normally generates. {@code null} leaves it unset.
	 */
	public static <T extends BaseEntity> T withId(T entity, Long id) {
		if (id != null) {
			ReflectionTestUtils.setField(entity, "id", id);
		}
		return entity;
	}
}
