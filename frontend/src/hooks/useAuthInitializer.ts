import { useEffect } from "react";
import { useSelector } from "react-redux";
import { useAppDispatch } from "../store/store";
import type { RootState } from "../store/store";
import { initializeAuth, fetchUserProfile } from "../features/auth/authSlice";

/**
 * Runs once when the app loads to check if the user
 * is already logged in via HTTP-only cookie session.
 * Also fetches full user profile (name, age, gender, phone) from the API
 * whenever authentication state changes (login, signup, or page refresh).
 */
export const useAuthInitializer = () => {
  const dispatch = useAppDispatch();
  const isAuthenticated = useSelector((s: RootState) => s.auth.isAuthenticated);

  useEffect(() => {
    dispatch(initializeAuth());
  }, [dispatch]);

  // Fetch full profile whenever user becomes authenticated
  // (covers fresh login, signup, AND page refresh with existing token)
  useEffect(() => {
    if (isAuthenticated) {
      const token = localStorage.getItem('jwt_token');
      if (token) {
        dispatch(fetchUserProfile());
      }
    }
  }, [dispatch, isAuthenticated]);
};