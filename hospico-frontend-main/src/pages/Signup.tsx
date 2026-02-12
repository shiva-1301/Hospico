import { useState, useEffect } from "react";
import { useAppDispatch } from "../store/store";
import { useSelector } from "react-redux";
import { signup } from "../features/auth/authSlice";
import { Link, useNavigate } from "react-router-dom";
import type { RootState } from "../store/store";

const Signup = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, isAuthenticated, error } = useSelector((state: RootState) => state.auth);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/dashboard");
    }
  }, [isAuthenticated, navigate]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const resultAction = await dispatch(signup({ email, password, name, phone }));

    if (signup.fulfilled.match(resultAction)) {
      navigate("/dashboard"); // Redirect to dashboard after successful signup
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-8 sm:py-12 bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
      <div className="w-full max-w-sm sm:max-w-md lg:max-w-lg">
        <form
          onSubmit={onSubmit}
          className="w-full bg-white dark:bg-slate-800 rounded-lg p-4 sm:p-6 lg:p-8 shadow-lg border border-gray-200 dark:border-gray-700 transition-colors duration-200"
        >
          <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold mb-4 sm:mb-6 text-gray-900 dark:text-white text-center">
            Sign up
          </h1>
          <div className="space-y-3 sm:space-y-4">
            <input
              className="w-full px-3 sm:px-4 py-2 sm:py-3 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
              placeholder="Name"
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <input
              className="w-full px-3 sm:px-4 py-2 sm:py-3 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
              placeholder="Email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <input
              className="w-full px-3 sm:px-4 py-2 sm:py-3 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
              placeholder="Phone"
              type="tel"
              autoComplete="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
            <input
              className="w-full px-3 sm:px-4 py-2 sm:py-3 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
              placeholder="Password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              type="submit"
              className="w-full px-4 py-2 sm:py-3 rounded-md bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium transition-colors text-sm sm:text-base"
              disabled={!email || !password || status === "loading"}
            >
              {status === "loading" ? "Creating account..." : "Create account"}
            </button>
            <p className="text-xs sm:text-sm text-gray-600 dark:text-gray-400 text-center">
              Have an account?{" "}
              <Link
                className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                to="/login"
              >
                Login
              </Link>
            </p>
          </div>
        </form>
        {error && (
          <p className="text-red-500 dark:text-red-400 text-sm text-center mt-2">{error}</p>
        )}
      </div>
    </div>
  );
};

export default Signup;