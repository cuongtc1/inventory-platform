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
    // Browser tab title: "Products · Stockline"
    documentTitle: (page: string) => `${page} · Stockline`,
  },

  // Sidebar and top bar (AppLayout)
  layout: {
    openMenu: 'Open menu',
    collapseMenu: 'Collapse menu',
    expandMenu: 'Expand menu',
    notifications: 'Notifications',
    profile: 'My profile',
    // Until login exists (Phase 3) there is no signed-in user to show
    guest: 'Guest',
  },

  // Pages that don't exist yet, 404 and unexpected errors
  pages: {
    placeholder: (task: string) => `This page is built in ${task}.`,
    phase: (phase: number) => `Phase ${phase}`,
    comingSoon: {
      title: 'Coming soon',
      description: (phase: string) => `This part of Stockline arrives in ${phase}.`,
    },
    notFound: {
      title: 'Page not found',
      description: 'The page you are looking for does not exist.',
      backHome: 'Back to the dashboard',
    },
    error: {
      title: 'Something went wrong',
      description: 'This page could not be shown. Reload to try again.',
      // A lazy-loaded page file was not found, usually because a new version was deployed meanwhile
      newVersion: 'A new version of Stockline is available. Reload the page to continue.',
      reload: 'Reload',
    },
  },

  dashboard: {
    title: 'Dashboard',
    connection: {
      title: 'Backend connection',
      checking: 'Checking product-service…',
      connected: (categories: number) =>
        `Connected to product-service: ${categories} ${categories === 1 ? 'category' : 'categories'}.`,
      failed: 'product-service is not reachable',
    },
    quickLinks: 'Catalog',
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
      saveChanges: 'Save changes',
      retry: 'Try again',
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
    // Under a table: "21-40 of 57"
    pageRange: (from: number, to: number, total: number) => `${from}-${to} of ${total}`,
    // Under an edit form: "Created 22/10/2026 01:30 by system"
    audit: {
      created: (when: string, by: string) => `Created ${when} by ${by}`,
      updated: (when: string, by: string) => `Last updated ${when} by ${by}`,
    },
    // Form checks done in the browser (Ant Design fills in ${label} with the field's label). The backend checks
    // everything again; its messages are shown as they come.
    validation: {
      required: '${label} is required',
      code: "May contain only letters, digits, '.', '_' and '-'",
      optionsFailed: 'The choices for this form could not be loaded.',
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
    newTitle: 'New Product',
    editTitle: (id: string) => `Edit Product #${id}`,
    searchPlaceholder: 'Search by name or SKU…',
    allCategories: 'All categories',
    allSuppliers: 'All suppliers',
    noSupplier: 'No supplier',
    selectCategory: 'Select a category',
    created: (sku: string) => `Product ${sku} created`,
    updated: (sku: string) => `Product ${sku} saved`,
    deleted: (sku: string) => `Product ${sku} deleted`,
    confirmDelete: (name: string) => `Delete "${name}"?`,
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
