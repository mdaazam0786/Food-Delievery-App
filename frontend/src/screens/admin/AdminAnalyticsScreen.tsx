import React, { useEffect, useState } from 'react';
import AdminLayout from './AdminLayout';
import { getAccessToken } from '../../services/authService';
import { formatINR } from '../../utils/currencyFormatter';

interface Analytics {
  todaysEarnings: number;
  totalOrders: number;
  totalEarnings: number;
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

const AdminAnalyticsScreen: React.FC = () => {
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    try {
      setLoading(true);
      setError(null);

      const token = getAccessToken();
      if (!token) {
        throw new Error('No access token — user is not authenticated.');
      }

      const response = await fetch(`${BASE_URL}/api/admin/analytics/todays-earnings`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch analytics: ${response.statusText}`);
      }

      const data = await response.json();
      setAnalytics(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => formatINR(value);

  return (
    <AdminLayout>
      <div style={{ padding: '8px 0' }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, color: '#1E2229', marginBottom: 8 }}>
          Analytics
        </h1>
        <p style={{ color: '#9EA1B1', fontSize: 14 }}>
          Revenue trends, order volumes, and performance metrics.
        </p>

        {loading && (
          <div style={{
            marginTop: 32, background: '#fff', borderRadius: 16,
            border: '1px solid #ECEEF4', padding: '48px 32px',
            textAlign: 'center', color: '#C0C4D0', fontSize: 15,
          }}>
            Loading analytics...
          </div>
        )}

        {error && (
          <div style={{
            marginTop: 32, background: '#fff', borderRadius: 16,
            border: '1px solid #ECEEF4', padding: '48px 32px',
            textAlign: 'center', color: '#D32F2F', fontSize: 15,
          }}>
            {error}
          </div>
        )}

        {analytics && !loading && (
          <div style={{ marginTop: 32, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 16 }}>
            {/* Today's Earnings Card */}
            <div style={{
              background: '#fff', borderRadius: 16, border: '1px solid #ECEEF4',
              padding: 24, boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
            }}>
              <h3 style={{ color: '#9EA1B1', fontSize: 13, fontWeight: 600, marginBottom: 12, textTransform: 'uppercase' }}>
                Today's Earnings
              </h3>
              <p style={{ fontSize: 28, fontWeight: 700, color: '#1E2229' }}>
                {formatCurrency(analytics.todaysEarnings)}
              </p>
            </div>

            {/* Total Orders Card */}
            <div style={{
              background: '#fff', borderRadius: 16, border: '1px solid #ECEEF4',
              padding: 24, boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
            }}>
              <h3 style={{ color: '#9EA1B1', fontSize: 13, fontWeight: 600, marginBottom: 12, textTransform: 'uppercase' }}>
                Total Orders
              </h3>
              <p style={{ fontSize: 28, fontWeight: 700, color: '#1E2229' }}>
                {analytics.totalOrders}
              </p>
            </div>

            {/* Total Earnings Card */}
            <div style={{
              background: '#fff', borderRadius: 16, border: '1px solid #ECEEF4',
              padding: 24, boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
            }}>
              <h3 style={{ color: '#9EA1B1', fontSize: 13, fontWeight: 600, marginBottom: 12, textTransform: 'uppercase' }}>
                Total Earnings
              </h3>
              <p style={{ fontSize: 28, fontWeight: 700, color: '#1E2229' }}>
                {formatCurrency(analytics.totalEarnings)}
              </p>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};

export default AdminAnalyticsScreen;
