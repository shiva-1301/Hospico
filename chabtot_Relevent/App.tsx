import "./App.css";
import { useEffect, useState } from "react";
import OfflineScreen from "./components/OfflineScreen";
// import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Navbar from "./components/Navbar";
import Dashboard from "./pages/Dashboard";
import FindHospitals from "./pages/FindHospitals";
import ProtectedRoute from "./components/ProtectedRoute";
import { useSelector } from "react-redux";
import type { RootState } from "./store/store";
import { useAuthInitializer } from "./hooks/useAuthInitializer";
import { useKeepAlive } from "./hooks/useKeepAlive";
import FullScreenLoader from "./components/FullScreenLoader";
import PartnerLogin from "./pages/PartnerLogin";
import PartnerSignup from "./pages/PartnerSignup";
import Emergency from "./pages/Emergency";
import HospitalProfile from "./pages/HospitalProfile";
import Profile from "./pages/Profile.tsx";
import MyAppointments from "./pages/MyAppointments";
import MedicalReports from "./pages/MedicalReports";
import HospitalDashboard from "./pages/HospitalDashboard";
import DoctorDashboard from "./pages/DoctorDashboard";
import ChatWidget from "./components/ChatWidget";

import { ThemeProvider } from "./context/ThemeContext";

function App() {
  useAuthInitializer();
  useKeepAlive(); // Keep services alive on Render

  function TitleUpdater() {
    const location = useLocation();

    useEffect(() => {
      const path = location.pathname;
      const titleMap: { [key: string]: string } = {
        '/': 'Dashboard - Hospiico',
        '/dashboard': 'Dashboard - Hospiico',
        '/find-hospitals': 'Find Hospitals - Hospiico',
        '/hospitals': 'Find Hospitals - Hospiico',
        '/emergency': 'Emergency - Hospiico',
        '/login': 'Login - Hospiico',
        '/signup': 'Sign Up - Hospiico',
        '/partner-login': 'Partner Login - Hospiico',
        '/profile': 'My Profile - Hospiico',
        '/my-appointments': 'My Appointments - Hospiico',
        '/resources': 'Resources - Hospiico',
      };

      // dynamic routes handling
      let title = titleMap[path];
      if (!title) {
        if (path.startsWith('/find-hospital')) title = 'Hospital Profile - Hospiico';
        else title = 'Hospiico';
      }

      document.title = title;
    }, [location]);

    return null;
  }

  const {
    initialized,
    // isAuthenticated
  } = useSelector((s: RootState) => s.auth);

  if (!initialized) {
    return <FullScreenLoader />;
  }

  return (
    <ThemeProvider>
      {/* Offline Screen Check */}
      <ConnectivityHandler>
        <BrowserRouter>
          <TitleUpdater />
          <div className="h-screen overflow-y-auto bg-white dark:bg-slate-900 transition-colors duration-200">
            <Navbar />
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/partner-login" element={<PartnerLogin />} />
              <Route path="/partner-signup" element={<PartnerSignup />} />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <Dashboard />
                  </ProtectedRoute>
                }
              />
              <Route path="/find-hospitals" element={<FindHospitals />} />
              <Route path="/hospitals" element={<FindHospitals />} />
              <Route path="/emergency" element={<Emergency />} />
              <Route path="/find-hospital/:id" element={<HospitalProfile />} />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <Profile />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/my-appointments"
                element={
                  <ProtectedRoute>
                    <MyAppointments />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reports"
                element={
                  <ProtectedRoute>
                    <MedicalReports />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/hospital-dashboard"
                element={
                  <ProtectedRoute>
                    <HospitalDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/doctor-dashboard/:doctorId"
                element={
                  <ProtectedRoute>
                    <DoctorDashboard />
                  </ProtectedRoute>
                }
              />
              <Route path="/" element={<Dashboard />} />
            </Routes>
            <ChatWidget />
          </div>
        </BrowserRouter>
      </ConnectivityHandler>
    </ThemeProvider>
  );
}

// Internal component to handle connectivity logic cleanly
function ConnectivityHandler({ children }: { children: React.ReactNode }) {
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (!isOnline) {
    return <OfflineScreen onRetry={() => window.location.reload()} />;
  }

  return <>{children}</>;
}

export default App;
