import {
  createSlice,
  createAsyncThunk,
  type PayloadAction,
} from "@reduxjs/toolkit";
import { apiRequest } from "../../api";

export type AuthUser = {
  id: string;
  email: string;
  name?: string;
  phone?: string;
  age?: number;
  gender?: string;
};

export type AuthState = {
  user: AuthUser | null;
  status: "idle" | "loading" | "succeeded" | "failed";
  error: string | null;
  isAuthenticated: boolean;
  initialized: boolean;
};

type Credentials = { email: string; password: string };
type SignupPayload = { email: string; password: string; name?: string; phone?: string };
// Updated AuthResponse type to match backend response
export type AuthResponse = {
  success: boolean;
  message: string;
  id: number;
  email: string;
  name: string;
  role: string;
  token: string;
};

export const login = createAsyncThunk<AuthResponse, Credentials>(
  "auth/login",
  async (body, { rejectWithValue }) => {
    try {
      const result = await apiRequest<AuthResponse, Credentials>(
        "/api/auth/login",
        "POST",
        body
      );

      return result;
    } catch (err) {
      return rejectWithValue((err as Error).message);
    }
  }
);

export const signup = createAsyncThunk<AuthResponse, SignupPayload>(
  "auth/signup",
  async (body, { rejectWithValue }) => {
    try {
      const result = await apiRequest<AuthResponse, SignupPayload>(
        "/api/auth/signup",
        "POST",
        body
      );

      return result;
    } catch (err) {
      return rejectWithValue((err as Error).message);
    }
  }
);

const initialState: AuthState = {
  user: null,
  status: "idle",
  error: null,
  isAuthenticated: false,
  initialized: false,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.isAuthenticated = false;
      // Remove the JWT token from localStorage
      localStorage.removeItem('jwt_token');
    },
    initializeAuth: (state) => {
      // Check if we have a stored token
      const token = localStorage.getItem('jwt_token');
      if (token) {
        // For now, we'll just set isAuthenticated to true
        // In a real app, you might want to verify the token
        state.isAuthenticated = true;
        state.initialized = true;
      } else {
        state.initialized = true;
      }
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // LOGIN
    builder
      .addCase(login.pending, (state) => {
        state.status = "loading";
        state.error = null;
        state.initialized = false;
      })
      .addCase(
        login.fulfilled,
        (state, action: PayloadAction<AuthResponse>) => {
          state.status = "succeeded";
          if (action.payload && action.payload.id) {
            state.user = {
              id: action.payload.id.toString(),
              email: action.payload.email,
            };
          }
          // Store the JWT token in localStorage
          if (action.payload && action.payload.token) {
            localStorage.setItem('jwt_token', action.payload.token);
          }
          state.isAuthenticated = true;
          state.initialized = true;
        }
      )
      .addCase(login.rejected, (state, action) => {
        state.status = "failed";
        state.error =
          (action.payload as string) ?? action.error.message ?? "Login failed";
        state.isAuthenticated = false;
        state.initialized = true;
      });

    // SIGNUP
    builder
      .addCase(signup.pending, (state) => {
        state.status = "loading";
        state.error = null;
        state.initialized = false;
      })
      .addCase(
        signup.fulfilled,
        (state, action: PayloadAction<AuthResponse>) => {
          state.status = "succeeded";
          if (action.payload && action.payload.id) {
            state.user = {
              id: action.payload.id.toString(),
              email: action.payload.email,
            };
          }
          // Store the JWT token in localStorage
          if (action.payload && action.payload.token) {
            localStorage.setItem('jwt_token', action.payload.token);
          }
          state.isAuthenticated = true;
          state.initialized = true;
        }
      )
      .addCase(signup.rejected, (state, action) => {
        state.status = "failed";
        state.error =
          (action.payload as string) ?? action.error.message ?? "Signup failed";
        state.isAuthenticated = false;
        state.initialized = true;
      });
  },
});

export const { logout, initializeAuth, clearError } = authSlice.actions;
export default authSlice.reducer;