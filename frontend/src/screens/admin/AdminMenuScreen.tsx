import React, { useState, useRef } from 'react';
import AdminLayout from './AdminLayout';
import { uploadMenuItemImageImmediate, addMenuItem, getMenuItems } from '../../services/restaurantService';
import { useRestaurant } from '../../context/useRestaurant';
import './AdminMenuScreen.css';

// ─── Types ────────────────────────────────────────────────────────────────────

interface MenuItem {
  id: string;
  name: string;
  category: string;
  price: string;
  description: string;
  imageUrl?: string;
}

interface FormState {
  name: string;
  description: string;
  price: string;
  category: string;
  imageUrl: string;
  imageName: string;
  imageUploading: boolean;
  imageError: string | null;
  isVeg: boolean;
}

const EMPTY_FORM: FormState = {
  name: '', description: '', price: '', category: '', imageUrl: '', imageName: '', imageUploading: false, imageError: null, isVeg: false,
};

const CATEGORIES = ['Burger', 'Pizza', 'Sandwich', 'Chicken', 'Drinks', 'Sides', 'Desserts'];

// ─── Seed menu items ──────────────────────────────────────────────────────────
// TODO: Seed items can be used for demo/testing purposes
// const SEED_ITEMS: MenuItem[] = [
//   { id: 'm1', name: 'Burger Bistro',       category: 'Burger',   price: '₹720', description: 'Classic smash burger with lettuce, tomato & special sauce.' },
//   { id: 'm2', name: "Smokin' Burger",       category: 'Burger',   price: '₹840', description: 'Smoky BBQ patty with caramelised onions.' },
//   { id: 'm3', name: 'Margherita Pizza',     category: 'Pizza',    price: '₹840', description: 'San Marzano tomato, fresh mozzarella, basil.' },
//   { id: 'm4', name: 'Pepperoni Pizza',      category: 'Pizza',    price: '₹960', description: 'Loaded with premium pepperoni slices.' },
//   { id: 'm5', name: 'Club Sandwich',        category: 'Sandwich', price: '₹540',  description: 'Triple-decker with chicken, bacon & avocado.' },
//   { id: 'm6', name: 'Crispy Chicken',       category: 'Chicken',  price: '₹720', description: 'Southern-style fried chicken fillet.' },
//   { id: 'm7', name: 'Mango Shake',          category: 'Drinks',   price: '₹300',  description: 'Fresh mango blended with milk & ice cream.' },
//   { id: 'm8', name: 'Lemonade',             category: 'Drinks',   price: '₹240',  description: 'Freshly squeezed with a hint of mint.' },
// ];

// ─── Category colour map ──────────────────────────────────────────────────────

const CAT_COLORS: Record<string, { bg: string; text: string }> = {
  Burger:   { bg: 'rgba(255,118,34,0.10)',  text: '#FF7622' },
  Pizza:    { bg: 'rgba(239,68,68,0.10)',   text: '#EF4444' },
  Sandwich: { bg: 'rgba(245,158,11,0.10)',  text: '#F59E0B' },
  Chicken:  { bg: 'rgba(234,179,8,0.10)',   text: '#CA8A04' },
  Drinks:   { bg: 'rgba(59,130,246,0.10)',  text: '#3B82F6' },
  Sides:    { bg: 'rgba(34,197,94,0.10)',   text: '#16A34A' },
  Desserts: { bg: 'rgba(168,85,247,0.10)',  text: '#9333EA' },
};

const catStyle = (cat: string) =>
  CAT_COLORS[cat] ?? { bg: 'rgba(107,114,128,0.10)', text: '#6B7280' };

// ─── Icons ────────────────────────────────────────────────────────────────────

const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/>
    <line x1="5" y1="12" x2="19" y2="12" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/>
  </svg>
);

const TrashIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="3 6 5 6 21 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M10 11v6M14 11v6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
    <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const UploadIcon = () => (
  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"
      stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    <polyline points="17 8 12 3 7 8" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round"/>
    <line x1="12" y1="3" x2="12" y2="15" stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round"/>
  </svg>
);

const CloseIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
    <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
  </svg>
);

const VegBadgeIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="4" y="4" width="16" height="16" rx="2" stroke="#22C55E" strokeWidth="2"/>
  </svg>
);

const NonVegBadgeIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polygon points="12,4 20,16 4,16" stroke="#DC2626" strokeWidth="2" fill="none"/>
  </svg>
);

// ─── Category group pill ──────────────────────────────────────────────────────

const CategoryGroupCard: React.FC<{
  category: string;
  items: MenuItem[];
  onDelete: (id: string) => void;
}> = ({ category, items, onDelete }) => {
  const style = catStyle(category);
  return (
    <div className="adm-menu-group">
      <div className="adm-menu-group__header">
        <span
          className="adm-menu-group__badge"
          style={{ background: style.bg, color: style.text }}
        >
          {category}
        </span>
        <span className="adm-menu-group__count">{items.length} items</span>
      </div>
      <div className="adm-menu-group__items">
        {items.map((item) => (
          <div key={item.id} className="adm-menu-item-row">
            <div className="adm-menu-item-row__thumb" style={{ background: style.bg }}>
              <span style={{ fontSize: 18 }}>
                {category === 'Burger' ? '🍔' : category === 'Pizza' ? '🍕' :
                 category === 'Sandwich' ? '🥪' : category === 'Chicken' ? '🍗' :
                 category === 'Drinks' ? '🥤' : category === 'Sides' ? '🍟' : '🍰'}
              </span>
            </div>
            <div className="adm-menu-item-row__info">
              <span className="adm-menu-item-row__name">{item.name}</span>
              <span className="adm-menu-item-row__desc">{item.description}</span>
            </div>
            <span className="adm-menu-item-row__price">{item.price}</span>
            <button
              className="adm-menu-item-row__delete"
              onClick={() => onDelete(item.id)}
              aria-label={`Delete ${item.name}`}
            >
              <TrashIcon />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

// ─── Add item form (slide-in panel) ──────────────────────────────────────────

interface AddFormProps {
  onClose: () => void;
  onSubmit: (form: FormState) => void;
}

const AddItemForm: React.FC<AddFormProps> = ({ onClose, onSubmit }) => {
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [dragOver, setDragOver] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const fileInputRef = useRef<HTMLInputElement>(null);

  const set = (key: keyof FormState, value: string | boolean | null) => {
    console.log(`Setting ${key} to ${value} (type: ${typeof value})`);
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleFile = async (file: File | null) => {
    if (!file) return;
    
    setForm((prev) => ({ ...prev, imageName: file.name, imageUploading: true, imageError: null }));

    try {
      // This endpoint will be added next
      const imageUrl = await uploadMenuItemImageImmediate(file);
      setForm((prev) => ({ ...prev, imageUrl, imageUploading: false }));
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to upload image';
      setForm((prev) => ({ ...prev, imageUploading: false, imageError: errorMsg }));
      setForm((prev) => ({ ...prev, imageName: '', imageUrl: '' }));
    }
  };

  const validate = (): boolean => {
    const e: typeof errors = {};
    if (!form.name.trim())        e.name        = 'Item name is required.';
    if (!form.price.trim())       e.price       = 'Price is required.';
    if (!form.category)           e.category    = 'Please select a category.';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = (ev: React.FormEvent) => {
    ev.preventDefault();
    if (validate()) onSubmit(form);
  };

  const isSubmitDisabled = form.imageUploading;

  return (
    <div className="adm-form-overlay" role="dialog" aria-modal="true" aria-label="Add menu item">
      <div className="adm-form-panel">

        {/* Header */}
        <div className="adm-form-panel__header">
          <h2 className="adm-form-panel__title">Add Menu Item</h2>
          <button className="adm-form-panel__close" onClick={onClose} aria-label="Close form">
            <CloseIcon />
          </button>
        </div>

        <form className="adm-form-panel__body" onSubmit={handleSubmit} noValidate>

          {/* Item Name */}
          <div className="adm-field">
            <label className="adm-field__label" htmlFor="item-name">Item Name</label>
            <input
              id="item-name"
              className={`adm-field__input${errors.name ? ' adm-field__input--error' : ''}`}
              type="text"
              placeholder="e.g. Truffle Burger"
              value={form.name}
              onChange={(e) => set('name', e.target.value)}
              autoComplete="off"
            />
            {errors.name && <span className="adm-field__error">{errors.name}</span>}
          </div>

          {/* Description */}
          <div className="adm-field">
            <label className="adm-field__label" htmlFor="item-desc">
              Description
              <span className="adm-field__optional"> — optional</span>
            </label>
            <textarea
              id="item-desc"
              className="adm-field__textarea"
              placeholder="Ingredients, allergens, preparation notes…"
              rows={3}
              value={form.description}
              onChange={(e) => set('description', e.target.value)}
            />
          </div>

          {/* Price + Category row */}
          <div className="adm-field-row">
            <div className="adm-field">
              <label className="adm-field__label" htmlFor="item-price">Base Price (in ₹)</label>
              <div className="adm-field__prefix-wrap">
                <span className="adm-field__prefix">₹</span>
                <input
                  id="item-price"
                  className={`adm-field__input adm-field__input--prefixed${errors.price ? ' adm-field__input--error' : ''}`}
                  type="number"
                  min="0"
                  step="1"
                  placeholder="0"
                  value={form.price}
                  onChange={(e) => set('price', e.target.value)}
                />
              </div>
              {errors.price && <span className="adm-field__error">{errors.price}</span>}
            </div>

            <div className="adm-field">
              <label className="adm-field__label" htmlFor="item-category">Category</label>
              <select
                id="item-category"
                className={`adm-field__select${errors.category ? ' adm-field__input--error' : ''}`}
                value={form.category}
                onChange={(e) => set('category', e.target.value)}
              >
                <option value="">Select category…</option>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
              {errors.category && <span className="adm-field__error">{errors.category}</span>}
            </div>
          </div>

          {/* Vegetarian Toggle */}
          <div className="adm-field">
            <label className="adm-field__label">Item Type</label>
            <div className="adm-veg-toggle">
              <button
                type="button"
                className={`adm-veg-toggle__btn${!form.isVeg ? ' adm-veg-toggle__btn--active' : ''}`}
                onClick={() => set('isVeg', false)}
                aria-pressed={!form.isVeg}
              >
                <NonVegBadgeIcon />
                <span>Non-Veg</span>
              </button>
              <button
                type="button"
                className={`adm-veg-toggle__btn${form.isVeg ? ' adm-veg-toggle__btn--active' : ''}`}
                onClick={() => set('isVeg', true)}
                aria-pressed={form.isVeg}
              >
                <VegBadgeIcon />
                <span>Veg</span>
              </button>
            </div>
          </div>

          {/* Image upload dropzone */}
          <div className="adm-field">
            <label className="adm-field__label">Item Image
              <span className="adm-field__optional"> — optional</span>
            </label>
            <div
              className={`adm-dropzone${dragOver ? ' adm-dropzone--over' : ''}${form.imageName ? ' adm-dropzone--filled' : ''}`}
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragOver(false);
                handleFile(e.dataTransfer.files[0] ?? null);
              }}
              onClick={() => !form.imageUploading && fileInputRef.current?.click()}
              role="button"
              tabIndex={form.imageUploading ? -1 : 0}
              aria-label="Upload image — click or drag and drop"
              onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !form.imageUploading) fileInputRef.current?.click(); }}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="adm-dropzone__input"
                onChange={(e) => handleFile(e.target.files?.[0] ?? null)}
                disabled={form.imageUploading}
                aria-hidden="true"
                tabIndex={-1}
              />
              {form.imageUploading ? (
                <div className="adm-dropzone__uploading">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" className="adm-dropzone__spinner" aria-hidden="true">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5"
                      strokeLinecap="round" strokeDasharray="32" strokeDashoffset="12"/>
                  </svg>
                  <span className="adm-dropzone__upload-text">Uploading image…</span>
                </div>
              ) : form.imageName ? (
                <>
                  <span className="adm-dropzone__file-icon" aria-hidden="true">🖼️</span>
                  <span className="adm-dropzone__filename">{form.imageName}</span>
                  <span className="adm-dropzone__change">Click to change</span>
                </>
              ) : (
                <>
                  <UploadIcon />
                  <span className="adm-dropzone__primary">Drag &amp; drop an image here</span>
                  <span className="adm-dropzone__secondary">or click to browse — PNG, JPG, WEBP</span>
                </>
              )}
            </div>
            {form.imageError && (
              <span className="adm-field__error" role="alert">{form.imageError}</span>
            )}
          </div>

          {/* Actions */}
          <div className="adm-form-panel__actions">
            <button
              type="button"
              className="adm-form-panel__btn adm-form-panel__btn--cancel"
              onClick={onClose}
              disabled={isSubmitDisabled}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="adm-form-panel__btn adm-form-panel__btn--submit"
              disabled={isSubmitDisabled}
              aria-busy={isSubmitDisabled}
            >
              <PlusIcon />
              {isSubmitDisabled ? 'Uploading…' : 'Add to Menu'}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
};

// ─── Main screen ──────────────────────────────────────────────────────────────

const AdminMenuScreen: React.FC = () => {
  const { activeRestaurantId } = useRestaurant();
  
  const [items, setItems] = useState<MenuItem[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [activeCategory, setActiveCategory] = useState<string>('All');

  // On component mount, load menu items from backend
  React.useEffect(() => {
    const loadMenuItems = async () => {
      try {
        
        if (!activeRestaurantId) {
          setItems([]);
          return;
        }

        // Fetch menu items from the backend
        const menuItems = await getMenuItems(activeRestaurantId);
        
        // Convert API response to local MenuItem format
        const formattedItems: MenuItem[] = menuItems.map(item => ({
          id: item.id,
          name: item.name,
          category: item.category,
          price: `₹${item.price}`,
          description: item.description,
          imageUrl: item.imageUrl,
        }));
        
        setItems(formattedItems);
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to load menu items';
        console.error(errorMsg);
        setItems([]);
      }
    };

    loadMenuItems();
  }, [activeRestaurantId]);

  const allCategories = ['All', ...Array.from(new Set(items.map((i) => i.category)))];

  const visibleItems = activeCategory === 'All'
    ? items
    : items.filter((i) => i.category === activeCategory);

  const grouped = CATEGORIES.reduce<Record<string, MenuItem[]>>((acc, cat) => {
    const catItems = visibleItems.filter((i) => i.category === cat);
    if (catItems.length > 0) acc[cat] = catItems;
    return acc;
  }, {});

  const handleDelete = (id: string) => {
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  const handleAdd = async (form: FormState) => {
    try {
      if (!activeRestaurantId) {
        alert('Restaurant not found. Please complete provisioning first.');
        return;
      }

      console.log('Form state before submission:', form);
      console.log('isVeg value:', form.isVeg, 'type:', typeof form.isVeg);

      // Call the backend API to add the menu item
      const response = await addMenuItem(activeRestaurantId, {
        name: form.name.trim(),
        description: form.description.trim(),
        price: parseInt(form.price, 10),
        category: form.category,
        imageUrl: form.imageUrl || undefined,
        isVeg: form.isVeg,
      });

      // Add the returned item to local state
      const newItem: MenuItem = {
        id: response.id,
        name: response.name,
        category: response.category,
        price: `₹${response.price}`,
        description: response.description,
        imageUrl: response.imageUrl,
      };
      setItems((prev) => [...prev, newItem]);
      setShowForm(false);
    } catch (error) {
      alert(error instanceof Error ? error.message : 'Failed to add menu item');
    }
  };

  return (
    <AdminLayout>
      <div className="adm-menu">

        {/* Page header */}
        <div className="adm-menu__header">
          <div>
            <h1 className="adm-menu__title">Menu Management</h1>
            <p className="adm-menu__subtitle">
              {!items || items.length === 0
                ? 'Your menu is empty. Start by adding your first item.'
                : `${items.length} items across ${new Set(items.map((i) => i.category)).size} categories.`
              }
            </p>
          </div>
          <button
            className="adm-menu__add-btn"
            onClick={() => setShowForm(true)}
            aria-label="Add new menu item"
          >
            <PlusIcon />
            Add Menu Item
          </button>
        </div>

        {/* Category filter pills */}
        {items && items.length > 0 && (
          <div className="adm-menu__filters" role="group" aria-label="Filter by category">
            {allCategories.map((cat) => (
              <button
                key={cat}
                className={`adm-menu__filter-pill${activeCategory === cat ? ' adm-menu__filter-pill--active' : ''}`}
                onClick={() => setActiveCategory(cat)}
                aria-pressed={activeCategory === cat}
              >
                {cat}
              </button>
            ))}
          </div>
        )}

        {/* Menu groups */}
        {!items || items.length === 0 ? (
          <div className="adm-menu__empty-state" role="status" aria-live="polite">
            <p className="adm-menu__empty-state__text">
              No menu items found. Start building your menu catalogue by adding your first item above.
            </p>
            <button
              className="adm-menu__add-btn"
              onClick={() => setShowForm(true)}
              aria-label="Add first menu item"
            >
              <PlusIcon />
              Add First Item
            </button>
          </div>
        ) : Object.keys(grouped).length === 0 ? (
          <div className="adm-menu__empty">
            No items in this category yet.
          </div>
        ) : (
          <div className="adm-menu__groups">
            {Object.entries(grouped).map(([cat, catItems]) => (
              <CategoryGroupCard
                key={cat}
                category={cat}
                items={catItems}
                onDelete={handleDelete}
              />
            ))}
          </div>
        )}

      </div>

      {/* Add item form overlay */}
      {showForm && (
        <AddItemForm
          onClose={() => setShowForm(false)}
          onSubmit={handleAdd}
        />
      )}
    </AdminLayout>
  );
};

export default AdminMenuScreen;
