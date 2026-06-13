import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchUserOrderHistory, type RestaurantOrder, type Page } from '../services/orderService';
import { getRestaurantDetails, type RestaurantResponse } from '../services/restaurantService';
import './MyOrdersScreen.css';

type Tab = 'ongoing' | 'history';

interface OrderWithRestaurant extends RestaurantOrder {
  restaurant?: RestaurantResponse;
}

const RESTAURANT_FALLBACK = 'https://via.placeholder.com/200x120?text=Restaurant';

const MyOrdersScreen: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<Tab>('history');
  const [orders, setOrders] = useState<OrderWithRestaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const loadOrders = async () => {
      try {
        setLoading(true);
        const historyData = await fetchUserOrderHistory(page, 10);
        
        // Fetch restaurant details for each order
        const ordersWithRestaurants = await Promise.all(
          historyData.content.map(async (order) => {
            try {
              const restaurant = await getRestaurantDetails(order.restaurantId);
              return { ...order, restaurant };
            } catch {
              return order;
            }
          })
        );

        setOrders(ordersWithRestaurants);
        setTotalPages(historyData.totalPages);
        setError(null);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load orders';
        setError(message);
        setOrders([]);
      } finally {
        setLoading(false);
      }
    };

    loadOrders();
  }, [page]);

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

  const formatAmount = (amount: number) => {
    return `₹${amount.toFixed(2)}`;
  };

  const getStatusBadgeClass = (status: string) => {
    const normalizedStatus = status.toUpperCase();
    if (normalizedStatus === 'COMPLETED' || normalizedStatus === 'DELIVERED') {
      return 'completed';
    }
    if (normalizedStatus === 'CANCELLED' || normalizedStatus === 'CANCELED') {
      return 'cancelled';
    }
    return 'default';
  };

  const getStatusLabel = (status: string) => {
    const normalizedStatus = status.toUpperCase();
    if (normalizedStatus === 'COMPLETED' || normalizedStatus === 'DELIVERED') {
      return 'Completed';
    }
    if (normalizedStatus === 'CANCELLED' || normalizedStatus === 'CANCELED') {
      return 'Cancelled';
    }
    return status;
  };

  const handleReOrder = (order: OrderWithRestaurant) => {
    // TODO: Implement re-order functionality
    navigate(`/restaurant/${order.restaurantId}?reorder=${order.id}`);
  };

  const handleRate = (order: OrderWithRestaurant) => {
    // TODO: Implement rating functionality
    console.log('Rate order:', order.id);
  };

  const filteredOrders = orders.filter((order) => {
    if (activeTab === 'ongoing') {
      return false; // For now, no ongoing orders - to be implemented later
    }
    return true; // All orders in history
  });

  return (
    <div className="myorders-screen">
      {/* ── Header ── */}
      <div className="myorders-header">
        <button className="myorders-header__back" onClick={() => navigate('/profile')}>
          ←
        </button>
        <h1 className="myorders-header__title">My Orders</h1>
        <div className="myorders-header__spacer" />
      </div>

      {/* ── Tabs ── */}
      <div className="myorders-tabs">
        <button
          className={`myorders-tab ${activeTab === 'ongoing' ? 'myorders-tab--active' : ''}`}
          onClick={() => setActiveTab('ongoing')}
        >
          Ongoing
        </button>
        <button
          className={`myorders-tab ${activeTab === 'history' ? 'myorders-tab--active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          History
        </button>
      </div>

      {/* ── Content ── */}
      <div className="myorders-content">
        {/* Loading State */}
        {loading && (
          <div className="myorders-empty">
            <p>Loading orders…</p>
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="myorders-error">
            <p>{error}</p>
            <button
              className="myorders-error__retry"
              onClick={() => window.location.reload()}
            >
              Retry
            </button>
          </div>
        )}

        {/* Ongoing Tab Message */}
        {activeTab === 'ongoing' && !loading && !error && (
          <div className="myorders-empty">
            <p>No ongoing orders</p>
          </div>
        )}

        {/* History Tab - Orders List */}
        {activeTab === 'history' && !loading && !error && (
          <>
            {filteredOrders.length === 0 ? (
              <div className="myorders-empty">
                <p>No order history</p>
              </div>
            ) : (
              <div className="myorders-list">
                {filteredOrders.map((order) => (
                  <div key={order.id} className="myorders-card">
                    {/* Card Header - Restaurant Info */}
                    <div className="myorders-card__header">
                      <div className="myorders-card__thumb">
                        <img
                          src={order.restaurant?.imageUrl || RESTAURANT_FALLBACK}
                          alt={order.restaurant?.name || 'Restaurant'}
                          className="myorders-card__image"
                          onError={(e) => {
                            (e.currentTarget as HTMLImageElement).src = RESTAURANT_FALLBACK;
                          }}
                        />
                      </div>
                      <div className="myorders-card__info">
                        <div className="myorders-card__title-row">
                          <h3 className="myorders-card__name">
                            {order.restaurant?.name || 'Restaurant'}
                          </h3>
                          <span className="myorders-card__order-id">
                            #{order.id.substring(0, 6).toUpperCase()}
                          </span>
                        </div>
                        <div className="myorders-card__meta">
                          <span className="myorders-card__amount">
                            {formatAmount(order.totalAmount)}
                          </span>
                          <span className="myorders-card__dot">•</span>
                          <span className="myorders-card__date">
                            {formatDate(order.createdAt)}
                          </span>
                          <span className="myorders-card__dot">•</span>
                          <span className="myorders-card__items">
                            {String(order.items.length).padStart(2, '0')} Items
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Card Body - Status Badge */}
                    <div className="myorders-card__body">
                      <div className={`myorders-badge myorders-badge--${getStatusBadgeClass(order.status)}`}>
                        {getStatusLabel(order.status)}
                      </div>
                    </div>

                    {/* Card Footer - Action Buttons */}
                    <div className="myorders-card__footer">
                      <button
                        className="myorders-card__btn myorders-card__btn--outline"
                        onClick={() => handleRate(order)}
                      >
                        Rate
                      </button>
                      <button
                        className="myorders-card__btn myorders-card__btn--primary"
                        onClick={() => handleReOrder(order)}
                      >
                        Re-Order
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="myorders-pagination">
                <button
                  className="myorders-pagination__btn"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  ← Previous
                </button>
                <span className="myorders-pagination__info">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  className="myorders-pagination__btn"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default MyOrdersScreen;
