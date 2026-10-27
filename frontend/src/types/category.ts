import type { Audited } from './api'

/** Backend `CategoryResponse`. `parentId` / `parentName` are null for a top-level category. */
export interface CategoryResponse extends Audited {
  id: number
  name: string
  parentId: number | null
  parentName: string | null
  /** Products directly in this category, not in its subcategories. */
  productCount: number
}

/** Body of POST / PUT /api/categories. */
export interface CategoryRequest {
  name: string
  parentId?: number | null
}
