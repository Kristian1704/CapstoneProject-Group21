package med.supply.system.model;

import med.supply.system.exception.ExceptionHandler;

public class StorageItem {
    private final String sku;
    private final String name;
    private int quantity;

    public StorageItem(String sku, String name, int quantity) {

        if (sku == null || sku.isBlank())
            throw new ExceptionHandler.InvalidStorageItemException("SKU must not be blank");

        if (name == null || name.isBlank())
            throw new ExceptionHandler.InvalidStorageItemException("Item name must not be blank");

        if (quantity < 0)
            throw new ExceptionHandler.InvalidStorageItemException("Quantity must be non-negative");

        this.sku = sku.trim();
        this.name = name.trim();
        this.quantity = quantity;
    }

    public String getSku() { return this.sku; }

    public String getName() { return this.name; }

    public int getQuantity() { return this.quantity; }

    public void setQuantity(int quantity) {
        if (quantity < 0)
            throw new ExceptionHandler.InvalidStorageItemException("Quantity must be non-negative");

        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "StorageItem{sku='" + sku + "', name='" + name + "', quantity=" + quantity + "}";
    }
}
