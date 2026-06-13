import React from 'react';
import { LocationProvider } from '../context/LocationContext';
import HomeScreen from './HomeScreen';

const MainApp: React.FC = () => (
  <LocationProvider>
    <HomeScreen />
  </LocationProvider>
);

export default MainApp;
