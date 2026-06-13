import React, { useState } from 'react';
import MenuItemListRow from './MenuItemListRow';
import type { MenuItemResponse } from '../../services/restaurantService';
import './MenuCategoryAccordion.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const ChevronDownIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="6 9 12 15 18 9" stroke="currentColor" strokeWidth="2.5"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// ─── Component ────────────────────────────────────────────────────────────────

interface MenuCategoryAccordionProps {
  category: string;
  items: MenuItemResponse[];
  restaurantId: string;
  onItemAdd?: (item: MenuItemResponse) => void;
}

const MenuCategoryAccordion: React.FC<MenuCategoryAccordionProps> = ({
  category,
  items,
  restaurantId,
  onItemAdd,
}) => {
  const [isOpen, setIsOpen] = useState(true); // Default to open

  const handleToggle = () => {
    setIsOpen(!isOpen);
  };

  return (
    <div className="mca-accordion">
      {/* Category header with toggle */}
      <button
        className={`mca-accordion__header ${isOpen ? 'mca-accordion__header--open' : ''}`}
        onClick={handleToggle}
        aria-expanded={isOpen}
        aria-controls={`category-${category}`}
      >
        <h2 className="mca-accordion__title">{category}</h2>
        <div className={`mca-accordion__chevron ${isOpen ? 'mca-accordion__chevron--rotated' : ''}`}>
          <ChevronDownIcon />
        </div>
      </button>

      {/* Category items list - collapsible */}
      {isOpen && (
        <div 
          id={`category-${category}`}
          className="mca-accordion__content"
          role="region"
          aria-labelledby={`category-${category}`}
        >
          <div className="mca-accordion__items">
            {items.map((item) => (
              <MenuItemListRow
                key={item.id}
                item={item}
                restaurantId={restaurantId}
                onAddClick={onItemAdd}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default MenuCategoryAccordion;
