import { useEffect, useRef, useState } from 'react'
import {
  createAdminProduct,
  deleteAdminProduct,
  getAdminCategories,
  getAdminProducts,
  getAdminSizes,
  uploadAdminImage,
  updateAdminProduct,
} from '../services/adminService'
import './AdminPages.css'

function emptyProductForm() {
  return {
    name: '',
    price: '',
    description: '',
    image: '',
    categoryId: '',
    images: [{ imageUrl: '' }],
    sizes: [{ sizeId: '', quantity: 0 }],
  }
}

function ProductsPage() {
  const [items, setItems] = useState([])
  const [categories, setCategories] = useState([])
  const [sizeOptions, setSizeOptions] = useState([])
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(8)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyProductForm())
  const [uploadingMainImage, setUploadingMainImage] = useState(false)
  const [uploadingSubImageIndex, setUploadingSubImageIndex] = useState(null)
  const [selectedSubImageIndex, setSelectedSubImageIndex] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const mainImageInputRef = useRef(null)
  const subImageInputRef = useRef(null)

  async function loadCategories() {
    const res = await getAdminCategories({ page: 0, size: 100 })
    setCategories(res.data.items || [])
  }

  async function loadSizes() {
    const res = await getAdminSizes()
    const items = Array.isArray(res.data) ? res.data : []
    setSizeOptions(items.map((item) => ({ id: item.id, label: item.sizeName })))
  }

  async function loadData(nextPage = page) {
    const params = { keyword, page: nextPage, size }
    if (categoryId) params.categoryId = Number(categoryId)

    const res = await getAdminProducts(params)
    setItems(res.data.items || [])
    setTotal(res.data.total || 0)
    setTotalPages(res.data.totalPages || 0)
    setPage(res.data.page || 0)
  }

  useEffect(() => {
    loadCategories()
    loadSizes()
  }, [])

  useEffect(() => {
    loadData(0)
  }, [keyword, categoryId])

  function openCreate() {
    setEditing(null)
    setForm(emptyProductForm())
    setShowModal(true)
  }

  function openEdit(item) {
    setEditing(item)
    const subImages = item.images?.filter((img) => {
      if (img?.isMain) return false
      if (item.image && img?.imageUrl === item.image) return false
      return true
    })

    setForm({
      name: item.name || '',
      price: item.price || '',
      description: item.description || '',
      image: item.image || '',
      categoryId: item.categoryId || '',
      images: subImages?.length ? subImages.map((img) => ({ imageUrl: img.imageUrl })) : [{ imageUrl: '' }],
      sizes: item.sizes?.length ? item.sizes.map((s) => ({ sizeId: s.sizeId, quantity: s.quantity || 0 })) : [{ sizeId: '', quantity: 0 }],
    })
    setShowModal(true)
  }

  async function uploadImageFile(file) {
    const res = await uploadAdminImage(file)
    return res?.data?.url || ''
  }

  async function handlePickMainImage(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setUploadingMainImage(true)
    try {
      const imageUrl = await uploadImageFile(file)
      if (imageUrl) {
        setForm((prev) => ({ ...prev, image: imageUrl }))
      }
    } finally {
      setUploadingMainImage(false)
    }
  }

  async function handlePickSubImage(event) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (!file || selectedSubImageIndex === null) return

    setUploadingSubImageIndex(selectedSubImageIndex)
    try {
      const imageUrl = await uploadImageFile(file)
      if (imageUrl) {
        updateImageRow(selectedSubImageIndex, { imageUrl })
      }
    } finally {
      setUploadingSubImageIndex(null)
      setSelectedSubImageIndex(null)
    }
  }

  function updateImageRow(index, patch) {
    setForm((prev) => {
      const next = [...prev.images]
      next[index] = { ...next[index], ...patch }
      return { ...prev, images: next }
    })
  }

  function updateSizeRow(index, patch) {
    setForm((prev) => {
      const next = [...prev.sizes]
      next[index] = { ...next[index], ...patch }
      return { ...prev, sizes: next }
    })
  }

  async function submitForm(event) {
    event.preventDefault()

    const normalizedCategoryId = Number(form.categoryId)
    if (!Number.isFinite(normalizedCategoryId) || normalizedCategoryId <= 0) {
      alert('Vui lòng chọn danh mục hợp lệ.')
      return
    }

    const normalizedImages = form.images
      .map((item) => ({ imageUrl: String(item.imageUrl || '').trim() }))
      .filter((item) => item.imageUrl)

    const normalizedSizes = form.sizes
      .filter((item) => item.sizeId)
      .map((item) => ({
        sizeId: Number(item.sizeId),
        quantity: Number(item.quantity || 0),
      }))
      .filter((item) => Number.isFinite(item.sizeId) && item.sizeId > 0)
      .filter((item, index, list) => list.findIndex((entry) => entry.sizeId === item.sizeId) === index)

    const payload = {
      name: String(form.name || '').trim(),
      price: Number(form.price),
      description: form.description,
      image: String(form.image || '').trim(),
      categoryId: normalizedCategoryId,
      images: normalizedImages,
      sizes: normalizedSizes,
    }

    setSubmitting(true)
    try {
      if (editing) {
        await updateAdminProduct(editing.id, payload)
      } else {
        await createAdminProduct(payload)
      }

      setShowModal(false)
      loadData(0)
    } catch (error) {
      const message = error?.response?.data?.message || error?.message || 'Không thể lưu sản phẩm.'
      alert(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function removeItem(id) {
    if (!window.confirm('Xóa sản phẩm này?')) return
    await deleteAdminProduct(id)
    loadData(page)
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel__header">
        <h2 className="admin-panel__title">Quản lý sản phẩm</h2>
        <div className="admin-toolbar admin-toolbar--products">
          <input className="admin-input" placeholder="Tìm sản phẩm" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <select className="admin-select" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Tất cả danh mục</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
          <button type="button" className="admin-btn admin-btn--primary" onClick={openCreate}>Thêm sản phẩm</button>
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên</th>
              <th>Giá</th>
              <th>Danh mục</th>
              <th>Tồn kho</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td>{item.name}</td>
                <td>{item.price}</td>
                <td>{item.categoryName || '-'}</td>
                <td>{(item.sizes || []).reduce((sum, s) => sum + Number(s.quantity || 0), 0)}</td>
                <td>
                  <button type="button" className="admin-btn admin-btn--ghost" onClick={() => openEdit(item)}>Sửa</button>{' '}
                  <button type="button" className="admin-btn admin-btn--danger" onClick={() => removeItem(item.id)}>Xóa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {items.length === 0 && <div className="admin-empty">Không có dữ liệu</div>}

      <div className="admin-pagination">
        <span>Tổng: {total}</span>
        <div className="admin-toolbar">
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page <= 0} onClick={() => loadData(page - 1)}>Trước</button>
          <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
          <button type="button" className="admin-btn admin-btn--ghost" disabled={page + 1 >= totalPages} onClick={() => loadData(page + 1)}>Sau</button>
        </div>
      </div>

      {showModal && (
        <div className="admin-modal" onClick={() => setShowModal(false)}>
          <div className="admin-modal__content" onClick={(e) => e.stopPropagation()}>
            <h3>{editing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm'}</h3>
            <form onSubmit={submitForm} className="admin-grid">
              <label>
                Tên
                <input className="admin-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
              <label>
                Giá
                <input type="number" className="admin-input" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required min="0" />
              </label>
              <label>
                Danh mục
                <select className="admin-select" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
                  <option value="">Chọn danh mục</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Ảnh chính
                <div className="admin-file-inline">
                  <input
                    className="admin-input admin-input--url-short"
                    value={form.image}
                    onChange={(e) => setForm({ ...form, image: e.target.value })}
                    placeholder="/uploads/images/..."
                  />
                  <button
                    type="button"
                    className="admin-btn admin-btn--ghost"
                    onClick={() => mainImageInputRef.current?.click()}
                    disabled={uploadingMainImage}
                  >
                    {uploadingMainImage ? 'Đang tải...' : 'Chọn ảnh'}
                  </button>
                  <input
                    ref={mainImageInputRef}
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={handlePickMainImage}
                  />
                </div>
              </label>
              <label className="span-2">
                Mô tả
                <textarea className="admin-input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} />
              </label>

              <div className="span-2">
                <h4>Ảnh phụ</h4>
                <div className="admin-media-list">
                  {form.images.map((image, index) => (
                    <div className="admin-media-row admin-media-row--image" key={index}>
                      <input
                        className="admin-input admin-input--url-short"
                        value={image.imageUrl}
                        onChange={(e) => updateImageRow(index, { imageUrl: e.target.value })}
                        placeholder="https://... hoặc /uploads/..."
                      />
                      <button
                        type="button"
                        className="admin-btn admin-btn--ghost"
                        onClick={() => {
                          setSelectedSubImageIndex(index)
                          subImageInputRef.current?.click()
                        }}
                        disabled={uploadingSubImageIndex === index}
                      >
                        {uploadingSubImageIndex === index ? 'Đang tải...' : 'Chọn ảnh'}
                      </button>
                      <button
                        type="button"
                        className="admin-btn admin-btn--ghost"
                        onClick={() => setForm((prev) => ({ ...prev, images: prev.images.filter((_, i) => i !== index) }))}
                      >
                        Xóa
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    className="admin-btn admin-btn--ghost"
                    onClick={() => setForm((prev) => ({ ...prev, images: [...prev.images, { imageUrl: '' }] }))}
                  >
                    + Thêm ảnh
                  </button>
                  <input
                    ref={subImageInputRef}
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={handlePickSubImage}
                  />
                </div>
              </div>

              <div className="span-2">
                <h4>Size và tồn kho</h4>
                <div className="admin-media-list">
                  {form.sizes.map((item, index) => (
                    <div className="admin-media-row" key={index}>
                      <select className="admin-select" value={item.sizeId} onChange={(e) => updateSizeRow(index, { sizeId: e.target.value })}>
                        <option value="">Chọn size</option>
                        {sizeOptions.map((opt) => (
                          <option key={opt.id} value={opt.id}>{opt.label}</option>
                        ))}
                      </select>
                      <input
                        type="number"
                        className="admin-input"
                        min="0"
                        value={item.quantity}
                        onChange={(e) => updateSizeRow(index, { quantity: e.target.value })}
                      />
                      <button
                        type="button"
                        className="admin-btn admin-btn--ghost"
                        onClick={() => setForm((prev) => ({ ...prev, sizes: prev.sizes.filter((_, i) => i !== index) }))}
                      >
                        Xóa
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    className="admin-btn admin-btn--ghost"
                    onClick={() => setForm((prev) => ({ ...prev, sizes: [...prev.sizes, { sizeId: '', quantity: 0 }] }))}
                  >
                    + Thêm size
                  </button>
                </div>
              </div>

              <div className="span-2 admin-toolbar">
                <button type="submit" className="admin-btn admin-btn--primary" disabled={submitting}>
                  {submitting ? 'Đang lưu...' : 'Lưu'}
                </button>
                <button type="button" className="admin-btn admin-btn--ghost" onClick={() => setShowModal(false)}>Đóng</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default ProductsPage
