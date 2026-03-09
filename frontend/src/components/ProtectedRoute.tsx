import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { Navigate, useLocation } from "react-router-dom";
import { type ReactNode } from "react";
import FullScreenLoader from "./FullScreenLoader";

const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, initialized } = useSelector(
    (s: RootState) => s.auth
  );
  const role = useSelector((s: RootState) => s.auth.user?.role);
  const location = useLocation();
  const isDoctor = role?.toUpperCase() === "DOCTOR";
  const isHospital = role?.toUpperCase() === "HOSPITAL";

  // TODO: Implement this
  if (!initialized) return <FullScreenLoader />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (isDoctor && !location.pathname.startsWith("/doctor-dashboard")) {
    return <Navigate to="/doctor-dashboard" replace />;
  }

  if (isHospital && !location.pathname.startsWith("/hospital-dashboard")) {
    return <Navigate to="/hospital-dashboard" replace />;
  }
  return <>{children}</>;
};

export default ProtectedRoute;
