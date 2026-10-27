/**
 * Every text the UI shows, in one place (docs/conventions.md section 10), grouped like the backend's messages
 * files. Pages use `texts.product.title`, never a string literal. Wording follows the mockup (docs/mockup/).
 *
 * Texts that come from the backend (an error's `detail`, field messages) are shown as they are; the backend
 * already looks them up in its own messages files.
 *
 * `as const` keeps every value as a fixed literal, so a typo like `texts.product.titel` is a compile error.
 */
export const texts = {
  app: {
    name: 'Stockline',
    logOut: 'Log out',
    // Placeholder start page until routing and pages exist (T2.1)
    foundationsReady: 'Frontend foundations are ready. Routing and pages follow in T2.1–T2.4.',
  },

  common: {
    actions: {
      new: 'New',
      edit: 'Edit',
      delete: 'Delete',
      save: 'Save',
      cancel: 'Cancel',
      search: 'Search',
      reset: 'Reset',
      back: 'Back',
    },
    confirmDelete: {
      title: 'Delete this record?',
      content: 'This cannot be undone.',
      ok: 'Delete',
      cancel: 'Cancel',
    },
    feedback: {
      saved: 'Saved',
      deleted: 'Deleted',
      loading: 'Loading…',
      empty: 'No data',
    },
    fields: {
      createdAt: 'Created',
      createdBy: 'Created by',
      updatedAt: 'Updated',
      updatedBy: 'Updated by',
      actions: 'Actions',
    },
    filters: {
      allStatuses: 'All statuses',
    },
    errors: {
      // Shown when the backend can't be reached or answered with a 5xx; for 4xx its own `detail` is shown
      network: 'Cannot reach the server. Check your connection and try again.',
      unexpected: 'Something went wrong. Please try again.',
      validation: 'Please correct the highlighted fields.',
    },
  },

  // Sidebar sections and items, in the mockup's order
  nav: {
    sections: {
      overview: 'Overview',
      catalog: 'Catalog',
      inventory: 'Inventory',
      orders: 'Orders',
      insights: 'Insights',
      admin: 'Admin',
    },
    items: {
      dashboard: 'Dashboard',
      products: 'Products',
      categories: 'Categories',
      suppliers: 'Suppliers',
      warehouses: 'Warehouses',
      stockLevels: 'Stock Levels',
      stockMovements: 'Stock Movements',
      lowStock: 'Low-Stock Alerts',
      purchaseOrders: 'Purchase Orders',
      salesOrders: 'Sales Orders',
      customers: 'Customers',
      reports: 'Reports',
      notifications: 'Notifications',
      users: 'Users & Roles',
    },
  },

  category: {
    title: 'Categories',
    singular: 'Category',
    newButton: 'New Category',
    searchPlaceholder: 'Search categories…',
    fields: {
      name: 'Name',
      parent: 'Parent',
      productCount: 'Products',
    },
  },

  supplier: {
    title: 'Suppliers',
    singular: 'Supplier',
    newButton: 'New Supplier',
    searchPlaceholder: 'Search suppliers…',
    fields: {
      name: 'Name',
      contactName: 'Contact',
      email: 'Email',
      phone: 'Phone',
    },
  },

  product: {
    title: 'Products',
    singular: 'Product',
    newButton: 'New Product',
    searchPlaceholder: 'Search products…',
    fields: {
      sku: 'SKU',
      name: 'Name',
      category: 'Category',
      supplier: 'Supplier',
      costPrice: 'Cost price',
      salePrice: 'Sale price',
      price: 'Price',
      reorderLevel: 'Reorder level',
      status: 'Status',
    },
    status: {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
    },
  },
} as const
