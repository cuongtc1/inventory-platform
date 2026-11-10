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
  },

  // Login page, like the mockup
  login: {
    title: 'Sign in',
    subtitle: 'Manage products, stock and orders in one place.',
    email: 'Email',
    password: 'Password',
    submit: 'Sign in',
    // Sent here from a page, or the session ended
    pleaseLogIn: 'Please sign in to continue.',
  },

  // My profile page
  profile: {
    account: 'Account',
    fullName: 'Full name',
    email: 'Email',
    role: 'Role',
    changePassword: 'Change password',
    currentPassword: 'Current password',
    newPassword: 'New password',
    confirmPassword: 'Confirm new password',
    passwordsDiffer: 'The two passwords are not the same',
    passwordChanged: 'Password changed',
  },

  // Pages that don't exist yet, 404 and unexpected errors
  pages: {
    phase: (phase: number) => `Phase ${phase}`,
    comingSoon: {
      title: 'Coming soon',
      description: (phase: string) => `This part of Stockline arrives in ${phase}.`,
    },
    forbidden: {
      title: 'No permission',
      description: "You don't have permission to see this page.",
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
      allWarehouses: 'All warehouses',
      fromDate: 'From',
      toDate: 'To',
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
      email: 'Enter a valid email address',
      passwordLength: (min: number, max: number) => `Must have ${min} to ${max} characters`,
      phone: "May contain only digits, spaces, '+', '-', '(' and ')'",
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

  user: {
    title: 'Users & Roles',
    newButton: 'New User',
    newTitle: 'New User',
    editTitle: (id: string) => `Edit User #${id}`,
    searchPlaceholder: 'Search by name or email…',
    allRoles: 'All roles',
    selectRole: 'Select a role',
    deactivate: 'Deactivate',
    activate: 'Activate',
    confirmDeactivate: (name: string) => `Deactivate ${name}?`,
    deactivateHint: 'They can no longer sign in. You can activate them again.',
    created: (email: string) => `User ${email} created`,
    updated: (email: string) => `User ${email} saved`,
    deactivated: (email: string) => `User ${email} deactivated`,
    activated: (email: string) => `User ${email} activated`,
    roleNote: 'Admin includes Manager rights; Manager includes Staff rights.',
    ownRoleNote: "You can't change your own role.",
    fields: {
      fullName: 'Name',
      email: 'Email',
      userType: 'Role',
      status: 'Status',
      password: 'Password',
    },
    userType: {
      ADMIN: 'Admin',
      MANAGER: 'Manager',
      STAFF: 'Staff',
    },
    status: {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
    },
  },

  category: {
    title: 'Categories',
    singular: 'Category',
    newButton: 'New Category',
    newTitle: 'New Category',
    editTitle: (id: string) => `Edit Category #${id}`,
    searchPlaceholder: 'Search categories…',
    noParent: 'None (top level)',
    // On the product count: a link to the products of this category
    showProducts: (name: string) => `Show the products of ${name}`,
    created: (name: string) => `Category ${name} created`,
    updated: (name: string) => `Category ${name} saved`,
    deleted: (name: string) => `Category ${name} deleted`,
    confirmDelete: (name: string) => `Delete "${name}"?`,
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
    newTitle: 'New Supplier',
    editTitle: (id: string) => `Edit Supplier #${id}`,
    searchPlaceholder: 'Search suppliers…',
    created: (name: string) => `Supplier ${name} created`,
    updated: (name: string) => `Supplier ${name} saved`,
    deleted: (name: string) => `Supplier ${name} deleted`,
    confirmDelete: (name: string) => `Delete "${name}"?`,
    fields: {
      name: 'Name',
      contactName: 'Contact',
      email: 'Email',
      phone: 'Phone',
    },
  },

  customer: {
    title: 'Customers',
    newButton: 'New Customer',
    newTitle: 'New Customer',
    editTitle: (id: string) => `Edit Customer #${id}`,
    searchPlaceholder: 'Search by name or email…',
    created: (name: string) => `Customer ${name} created`,
    updated: (name: string) => `Customer ${name} saved`,
    deleted: (name: string) => `Customer ${name} deleted`,
    confirmDelete: (name: string) => `Delete "${name}"?`,
    fields: {
      name: 'Name',
      email: 'Email',
      phone: 'Phone',
      orderCount: 'Orders',
    },
  },

  salesOrder: {
    title: 'Sales Orders',
    newButton: 'New Sales Order',
    newTitle: 'New Sales Order',
    searchPlaceholder: 'Search by order or customer…',
    allCustomers: 'All customers',
    selectCustomer: 'Select a customer',
    selectWarehouse: 'Select a warehouse',
    selectProduct: 'Select a product',
    items: 'Items',
    addItem: 'Add item',
    removeItem: 'Remove item',
    noItems: 'Add at least one item.',
    productTwice: 'This product is already on the order',
    placeOrder: 'Place order',
    estimatedTotal: 'Estimated total',
    estimateNote: 'Prices are confirmed when the order is placed.',
    placed: (orderNumber: string) => `Order ${orderNumber} placed`,
    subtitle: (customer: string, by: string, when: string) => `${customer} · created by ${by} · ${when}`,
    history: 'Status history',
    changedBy: (who: string) => `by ${who}`,
    pendingNote: 'Waiting for the stock reservation, which comes with Phase 6. Until then the order can only be cancelled.',
    fields: {
      orderNumber: 'Order',
      customer: 'Customer',
      warehouse: 'Warehouse',
      date: 'Date',
      total: 'Total',
      status: 'Status',
      product: 'Product',
      item: 'Item',
      available: 'Available',
      quantity: 'Qty',
      unitPrice: 'Unit price',
      price: 'Price',
      lineTotal: 'Line total',
    },
    status: {
      PENDING: 'Pending',
      RESERVED: 'Reserved',
      PAID: 'Paid',
      SHIPPED: 'Shipped',
      COMPLETED: 'Completed',
      REJECTED: 'Rejected',
      CANCELLED: 'Cancelled',
    },
    steps: {
      created: 'Created',
      reserved: 'Stock reserved',
      paid: 'Paid',
      shipped: 'Shipped',
      done: 'Done',
    },
    actions: {
      pay: 'Mark as paid',
      ship: 'Mark as shipped',
      complete: 'Mark as completed',
      cancel: 'Cancel order',
      confirmCancel: (orderNumber: string) => `Cancel order ${orderNumber}?`,
      cancelHint: 'This can not be undone.',
      keep: 'Keep it',
      paid: (orderNumber: string) => `Order ${orderNumber} marked as paid`,
      shipped: (orderNumber: string) => `Order ${orderNumber} marked as shipped`,
      completed: (orderNumber: string) => `Order ${orderNumber} completed`,
      cancelled: (orderNumber: string) => `Order ${orderNumber} cancelled`,
    },
  },

  warehouse: {
    title: 'Warehouses',
    singular: 'Warehouse',
    newButton: 'New Warehouse',
    newTitle: 'New Warehouse',
    editTitle: (id: string) => `Edit Warehouse #${id}`,
    searchPlaceholder: 'Search by code or name…',
    created: (code: string) => `Warehouse ${code} created`,
    updated: (code: string) => `Warehouse ${code} saved`,
    deleted: (code: string) => `Warehouse ${code} deleted`,
    confirmDelete: (code: string) => `Delete "${code}"?`,
    fields: {
      code: 'Code',
      name: 'Name',
      address: 'Address',
      skuCount: 'SKUs',
    },
  },

  stockLevel: {
    title: 'Stock Levels',
    adjustButton: 'Adjust Stock',
    adjustRow: 'Adjust',
    searchPlaceholder: 'Search by product or SKU…',
    selectProduct: 'Select a product',
    selectWarehouse: 'Select a warehouse',
    quantityHelp: 'The change, not the new total: 50 adds, -2 takes away.',
    quantityNotZero: 'Enter a change other than 0',
    referencePlaceholder: 'Why, e.g. Count',
    adjusted: (product: string, warehouse: string, onHand: number) =>
      `${product} in ${warehouse}: ${onHand} on hand`,
    fields: {
      product: 'Product',
      warehouse: 'Warehouse',
      onHand: 'On hand',
      reserved: 'Reserved',
      available: 'Available',
      level: 'Level',
      reorderLevel: 'Reorder level',
      status: 'Status',
      quantity: 'Quantity',
      reference: 'Reference',
    },
  },

  stockMovement: {
    title: 'Stock Movements',
    searchPlaceholder: 'Search by product or SKU…',
    allTypes: 'All types',
    fields: {
      time: 'Time',
      product: 'Product',
      warehouse: 'Warehouse',
      type: 'Type',
      quantity: 'Qty',
      reference: 'Reference',
      by: 'By',
    },
    type: {
      IN: 'In',
      OUT: 'Out',
      ADJUST: 'Adjust',
      RESERVE: 'Reserve',
      RELEASE: 'Release',
    },
  },

  lowStock: {
    title: 'Low-Stock Alerts',
    status: 'Low',
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
