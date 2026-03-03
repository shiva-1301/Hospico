import { useEffect } from "react";
import { useAppDispatch } from "../store/store";
import { initializeAuth, fetchUserProfile } from "../features/auth/authSlice";

/**
 * Runs once when the app loads to check if the user
 * is already logged in via HTTP-only cookie session.
 * Also fetches full user profile (name, age, gender, phone) from the API.
 */
export const useAuthInitializer = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(initializeAuth());

    // If a token exists, fetch full user profile so name/age/gender/phone are available
    const token = localStorage.getItem('jwt_token');
    if (token) {
      dispatch(fetchUserProfile());
    }
  }, [dispatch]);
};