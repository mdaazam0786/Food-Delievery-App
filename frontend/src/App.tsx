import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import SplashScreen from './screens/SplashScreen';
import OnboardingScreen from './screens/OnboardingScreen';
import LoginScreen from './screens/LoginScreen';
import SignUpScreen from './screens/SignUpScreen';
import ForgotPasswordScreen from './screens/ForgotPasswordScreen';
import LocationRequestScreen from './screens/LocationRequestScreen';
import SearchScreen from './screens/SearchScreen';
import CategoryScreen from './screens/CategoryScreen';
import RestaurantScreen from './screens/RestaurantScreen';
import RestaurantProfileScreen from './screens/RestaurantProfileScreen';
import ProfileScreen from './screens/ProfileScreen';
import EditProfileScreen from './screens/EditProfileScreen';
import MyOrdersScreen from './screens/MyOrdersScreen';
import ItemDetailScreen from './screens/ItemDetailScreen';
import CheckoutScreen from './screens/CheckoutScreen';
import MainApp from './screens/MainApp';
import AdminDashboardScreen from './screens/admin/AdminDashboardScreen';
import AdminOrdersScreen from './screens/admin/AdminOrdersScreen';
import AdminMenuScreen from './screens/admin/AdminMenuScreen';
import AdminAnalyticsScreen from './screens/admin/AdminAnalyticsScreen';
import DriverDashboard from './screens/driver/DriverDashboard';
import OAuthCallbackScreen from './screens/OAuthCallbackScreen';
import OAuthLegacyRedirect from './screens/OAuthLegacyRedirect';
import { RestaurantProvider } from './context/RestaurantContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './context/ToastContext';
import ToastStack from './components/common/ToastStack';
import AdminGuard from './screens/admin/AdminGuard';
import DriverGuard from './screens/driver/DriverGuard';

// ── Error boundary to surface crashes instead of white screen ────────────────
class AdminErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { error: Error | null }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { error };
  }
  render() {
    if (this.state.error) {
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', height: '100vh', padding: 32, gap: 12,
          background: '#f0f2f8', textAlign: 'center',
        }}>
          <p style={{ fontWeight: 700, fontSize: 15, color: '#1E2229', margin: 0 }}>
            Admin panel crashed
          </p>
          <pre style={{
            fontSize: 12, color: '#e53e3e', background: '#fff',
            padding: 16, borderRadius: 8, maxWidth: 560, overflow: 'auto',
            textAlign: 'left', whiteSpace: 'pre-wrap',
          }}>
            {this.state.error.message}
            {'\n'}
            {this.state.error.stack}
          </pre>
          <button
            onClick={() => window.location.reload()}
            style={{
              padding: '10px 24px', background: '#FF7622', color: '#fff',
              border: 'none', borderRadius: 10, fontWeight: 700, fontSize: 13,
              cursor: 'pointer',
            }}
          >
            Reload
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

// ── Admin pages — each wrapped in the guard ───────────────────────────────────
const AdminDashboardPage = () => <AdminGuard><AdminDashboardScreen /></AdminGuard>;
const AdminOrdersPage    = () => <AdminGuard><AdminOrdersScreen /></AdminGuard>;
const AdminMenuPage      = () => <AdminGuard><AdminMenuScreen /></AdminGuard>;
const AdminAnalyticsPage = () => <AdminGuard><AdminAnalyticsScreen /></AdminGuard>;

// ── Driver pages — each wrapped in the driver guard ────────────────────────────
const DriverDashboardPage = () => <DriverGuard><DriverDashboard /></DriverGuard>;

const App: React.FC = () => (
  <BrowserRouter>
    <RestaurantProvider>
      <CartProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<SplashScreen />} />
            <Route path="/onboarding" element={<OnboardingScreen />} />
            <Route path="/login" element={<LoginScreen />} />
            <Route path="/auth/callback" element={<OAuthCallbackScreen />} />
            <Route path="/oauth2/redirect" element={<OAuthLegacyRedirect />} />
            <Route path="/register" element={<SignUpScreen />} />
            <Route path="/forgot-password" element={<ForgotPasswordScreen />} />
            <Route path="/location" element={<LocationRequestScreen />} />
            <Route path="/search" element={<SearchScreen />} />
            <Route path="/category" element={<CategoryScreen />} />
            <Route path="/restaurant" element={<RestaurantScreen />} />
            <Route path="/restaurant/:id" element={<RestaurantProfileScreen />} />
            <Route path="/item" element={<ItemDetailScreen />} />
            <Route path="/checkout" element={<CheckoutScreen />} />
            <Route path="/profile" element={<ProfileScreen />} />
            <Route path="/profile/edit" element={<EditProfileScreen />} />
            <Route path="/orders" element={<MyOrdersScreen />} />
            <Route path="/app" element={<MainApp />} />

            {/* ── Admin Panel ── */}
            <Route path="/admin" element={
              <AdminErrorBoundary>
                <AdminDashboardPage />
              </AdminErrorBoundary>
            } />
            <Route path="/admin/orders" element={
              <AdminErrorBoundary>
                <AdminOrdersPage />
              </AdminErrorBoundary>
            } />
            <Route path="/admin/menu" element={
              <AdminErrorBoundary>
                <AdminMenuPage />
              </AdminErrorBoundary>
            } />
            <Route path="/admin/analytics" element={
              <AdminErrorBoundary>
                <AdminAnalyticsPage />
              </AdminErrorBoundary>
            } />

            {/* ── Driver Panel ── */}
            <Route path="/driver" element={<DriverDashboardPage />} />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>

          <ToastStack />
        </ToastProvider>
      </CartProvider>
    </RestaurantProvider>
  </BrowserRouter>
);

export default App;
