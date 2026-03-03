import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { Navigate, useLocation } from "react-router-dom";
import { type ReactNode } from "react";
import FullScreenLoader from "./FullScreenLoader";

const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, initialized } = useSelector(
    (s: RootState) => s.auth
  );
  const location = useLocation();

  // TODO: Implement this
  if (!initialized) return <FullScreenLoader />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <>{children}</>;
};

export default ProtectedRoute;
