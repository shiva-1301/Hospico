import "./App.css";
import { useEffect, useState } from "react";
import OfflineScreen from "./components/OfflineScreen";
// import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { Navigate } from "react-router-dom";
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
import DoctorLogin from "./pages/DoctorLogin";
import DoctorDashboard from "./pages/DoctorDashboard";
import HospitalDashboard from "./pages/HospitalDashboard";
import Emergency from "./pages/Emergency";
import HospitalProfile from "./pages/HospitalProfile";
import Profile from "./pages/Profile.tsx";
import MyAppointments from "./pages/MyAppointments";
import MedicalReports from "./pages/MedicalReports";
import DoctorDashboard from "./pages/DoctorDashboard";
import ChatWidget from "./features/chatbot/src/components/ChatWidget";

import { ThemeProvider } from "./context/ThemeContext";

const extractClinicIdFromHospitalEmail = (email?: string) => {
  if (!email) return null;
  const match = email.match(/\.(\d+)@hospiico\.com$/i);
  return match?.[1] ?? null;
};

function App() {
  useAuthInitializer();
  useKeepAlive(); // Keep services alive on Render

  function TitleUpdater() {
    const location = useLocation();

    useEffect(() => {
      const path = location.pathname;
      const titleMap: { [key: string]: string } = {
        '/': 'Dashboard - HospiiCo',
        '/dashboard': 'Dashboard - HospiiCo',
        '/find-hospitals': 'Find Hospitals - HospiiCo',
        '/hospitals': 'Find Hospitals - HospiiCo',
        '/emergency': 'Emergency - HospiiCo',
        '/login': 'Login - HospiiCo',
        '/signup': 'Sign Up - HospiiCo',
        '/partner-login': 'Partner Login - HospiiCo',
        '/doctor-login': 'Doctor Login - HospiiCo',
        '/doctor-dashboard': 'Doctor Dashboard - HospiiCo',
        '/hospital-dashboard': 'Hospital Dashboard - HospiiCo',
        '/profile': 'My Profile - HospiiCo',
        '/my-appointments': 'My Appointments - HospiiCo',
        '/resources': 'Resources - HospiiCo',
      };

      // dynamic routes handling
      let title = titleMap[path];
      if (!title) {
        if (path.startsWith('/find-hospital')) title = 'Hospital Profile - HospiiCo';
        else title = 'HospiiCo';
      }

      document.title = title;
    }, [location]);

    return null;
  }

  const {
    initialized,
    user,
  } = useSelector((s: RootState) => s.auth);
  const isDoctor = user?.role?.toUpperCase() === "DOCTOR";
  const isHospital = user?.role?.toUpperCase() === "HOSPITAL";
  const hospitalClinicId = extractClinicIdFromHospitalEmail(user?.email) ?? user?.id ?? "";

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
              <Route path="/doctor-login" element={<DoctorLogin />} />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    {isDoctor
                      ? <Navigate to={`/doctor-dashboard/${user?.id ?? ""}`} replace />
                      : isHospital
                        ? <Navigate to={`/hospital-dashboard/${hospitalClinicId}`} replace />
                        : <Dashboard />}
                  </ProtectedRoute>
                }
              />
              <Route
                path="/doctor-dashboard"
                element={
                  <ProtectedRoute>
                    <DoctorDashboard />
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
              <Route
                path="/hospital-dashboard"
                element={
                  <ProtectedRoute>
                    <HospitalDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/hospital-dashboard/:hospitalId"
                element={
                  <ProtectedRoute>
                    <HospitalDashboard />
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
                path="/doctor-dashboard"
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
