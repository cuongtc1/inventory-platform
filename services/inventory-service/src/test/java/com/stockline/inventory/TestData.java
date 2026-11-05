package com.stockline.inventory;

import com.stockline.inventory.entity.Warehouse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Ready-made entities for tests. Use with a static import:
 * {@code import static com.stockline.inventory.TestData.warehouse;}
 *
 * <p>Methods with an {@code id} are for unit tests, where no database generates ids.
 */
public final class TestData {

	private TestData() {
	}

	public static Warehouse warehouse(String code, String name) {
		return new Warehouse(code, name, null);
	}

	public static Warehouse warehouse(Long id, String code, String name, String address) {
		Warehouse warehouse = new Warehouse(code, name, address);
		// The id is set by the database; unit tests have none, so set the private field directly
		ReflectionTestUtils.setField(warehouse, "id", id);
		return warehouse;
	}
}
