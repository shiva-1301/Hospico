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

// Type for /api/users/me response
type UserProfileResponse = {
  id: number;
  email: string;
  name?: string;
  phone?: string;
  age?: number;
  gender?: string;
  role?: string;
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

// Fetch full user profile from /api/users/me (includes name, age, gender, phone)
export const fetchUserProfile = createAsyncThunk<UserProfileResponse, void>(
  "auth/fetchUserProfile",
  async (_, { rejectWithValue }) => {
    try {
      const result = await apiRequest<UserProfileResponse>("/api/users/me", "GET");
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
    // Update user profile data in Redux (e.g. after profile save)
    setUser: (state, action: PayloadAction<Partial<AuthUser>>) => {
      if (state.user) {
        state.user = { ...state.user, ...action.payload };
      }
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
              name: action.payload.name || undefined,
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
              name: action.payload.name || undefined,
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

    // FETCH USER PROFILE (fills name, age, gender, phone from /api/users/me)
    builder
      .addCase(fetchUserProfile.fulfilled, (state, action: PayloadAction<UserProfileResponse>) => {
        const data = action.payload;
        if (data && data.id) {
          state.user = {
            id: data.id.toString(),
            email: data.email || state.user?.email || "",
            name: data.name || undefined,
            phone: data.phone || undefined,
            age: data.age ?? undefined,
            gender: data.gender || undefined,
          };
          state.isAuthenticated = true;
        }
      })
      .addCase(fetchUserProfile.rejected, (_state, action) => {
        console.warn("Failed to fetch user profile:", action.payload);
      });
  },
});

export const { logout, initializeAuth, clearError, setUser } = authSlice.actions;
export default authSlice.reducer;