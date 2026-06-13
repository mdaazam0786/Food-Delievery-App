/**
 * Home feed image assets — served from /public/images/
 *
 * Generic filenames (images.jpeg, images (1).jpeg, images (2).jpeg) were
 * identified visually: sushi, aloo paratha, red velvet cake.
 */

const img = (file: string) => `/images/${encodeURIComponent(file)}`;

export interface CategoryImage {
  id:    string;
  label: string;
  src:   string;
  alt:   string;
}

export const CATEGORY_IMAGES: CategoryImage[] = [
  { id: 'pizza',    label: 'Pizza',        src: img('homemade-pizza-in-air-fryer.jpg'), alt: 'Pepperoni pizza' },
  { id: 'burger',   label: 'Burger',       src: img('MSG-Smash-Burger-FT-RECIPE0124-d9682401f3554ef683e24311abdf342b.jpg'), alt: 'Smash burger' },
  { id: 'biryani',  label: 'Biryani',      src: img('SES-cuisine-of-north-india-1957883-d32a933f506d43f59ac38a8eb956884a.jpg'), alt: 'Paneer tikka with naan' },
  { id: 'dosa',     label: 'Dosa',         src: img('Dosa-Recipe-2-500x500.jpg'), alt: 'Crispy masala dosa' },
  { id: 'sushi',    label: 'Sushi',        src: img('images.jpeg'), alt: 'Sushi maki rolls' },
  { id: 'chinese',  label: 'Chinese',      src: img('vegetable-momos.jpg'), alt: 'Steamed vegetable momos' },
  { id: 'desserts', label: 'Desserts',     src: img('images (2).jpeg'), alt: 'Red velvet cake' },
  { id: 'north',    label: 'North Indian', src: img('images (1).jpeg'), alt: 'Aloo paratha with butter' },
  { id: 'south',    label: 'South Indian', src: img('Dosa-Recipe-2-500x500.jpg'), alt: 'South Indian dosa' },
  { id: 'rolls',    label: 'Rolls',        src: img('bbvgfbmg_kathi-rolls_625x300_12_February_24.jpg'), alt: 'Kathi rolls' },
  { id: 'pasta',    label: 'Pasta',        src: img('red-sauce-pasta-1.webp'), alt: 'Red sauce pasta' },
  { id: 'drinks',   label: 'Beverages',    src: img('8624835-how-to-make-a-cappuccino-beauty-4x3-0301-13d55eaad60b42058f24369c292d4ccb.jpg'), alt: 'Cappuccino' },
];

/** Fallback when a restaurant has no imageUrl from the API */
export const RESTAURANT_FALLBACK = img('homemade-pizza-in-air-fryer.jpg');
