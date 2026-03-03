import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { useAppDispatch } from "../store/store";
import { login } from "../features/auth/authSlice";
import { Link, useNavigate } from "react-router-dom";
import type { RootState } from "../store/store";

const Login = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, isAuthenticated, error } = useSelector((s: RootState) => s.auth);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/dashboard");
    }
  }, [isAuthenticated, navigate]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const resultAction = await dispatch(login({ email, password }));

    if (login.fulfilled.match(resultAction)) {
      navigate("/dashboard"); // Redirect to dashboard
    }
  };

  return (
    <div className="min-h-[calc(100vh-5rem)] flex items-center justify-center px-4 bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
      <div className="flex w-full max-w-6xl bg-white dark:bg-slate-800 rounded-lg shadow-xl overflow-hidden transition-colors duration-200">
        {/* Left marketing panel */}
        <div
          className="hidden md:flex flex-1 bg-cover bg-center items-center p-8 text-white relative"
          style={{
            backgroundImage: `url('/src/assets/images/hospital-login-bg.jpg')`, // Updated with a placeholder path or keep dynamic if needed
          }}
        >
          {/* Overlay to darken image for better text readability */}
          <div className="absolute inset-0 bg-blue-900 opacity-80 z-0"></div>
          <div className="relative z-10">
            <h1 className="text-4xl font-bold mb-4">
              A Smarter Way to Access Healthcare
            </h1>
            <p className="text-lg">
              Revolutionizing healthcare accessibility by instantly connecting
              patients to trusted hospitals and clinics around them.
            </p>
          </div>
        </div>

        {/* Right login form panel */}
        <div className="w-full md:w-1/2 flex items-center justify-center sm:p-8">
          <div className="w-full max-w-sm">
            <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white text-center">
              Welcome!
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-6 text-center">
              Please enter your email and password to continue
            </p>

            {error && (
              <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
                <p className="text-red-800 dark:text-red-300 text-sm">{error}</p>
              </div>
            )}

            <form
              onSubmit={handleLogin}
              className="space-y-4"
            >
              <input
                className="w-full px-4 py-2 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
                placeholder="Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <input
                className="w-full px-4 py-2 rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder-gray-500 dark:placeholder-gray-400 transition-colors"
                placeholder="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />

              <button
                type="submit"
                className="w-full px-4 py-3 rounded-md bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium transition-colors text-base flex items-center justify-center"
                disabled={!email || !password || status === "loading"}
              >
                {status === "loading" ? "Logging in..." : "Continue →"}
              </button>
            </form>

            <div className="flex items-center my-6">
              <hr className="flex-grow border-t border-gray-300 dark:border-gray-600" />
              <span className="px-3 text-gray-500 dark:text-gray-400 text-sm">
                Or sign in as
              </span>
              <hr className="flex-grow border-t border-gray-300 dark:border-gray-600" />
            </div>

            <div className="space-y-4">
              <Link
                to="/partner-login"
                className="w-full flex items-center justify-center p-3 rounded-md border border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-200 font-medium hover:bg-gray-50 dark:hover:bg-slate-700 transition-colors"
              >
                Hospital Partner
                <span className="ml-auto text-xl">→</span>
              </Link>
              <button
                className="w-full flex items-center justify-between p-3 rounded-md border border-gray-300 dark:border-gray-600 text-gray-400 dark:text-gray-500 font-medium cursor-not-allowed bg-gray-50 dark:bg-gray-800/50"
                disabled
              >
                Doctor
                <span className="ml-auto text-sm">Coming Soon</span>
              </button>
            </div>

            <p className="text-xs sm:text-sm text-gray-600 dark:text-gray-400 text-center mt-6">
              <Link
                className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
                to="/signup"
              >
                New User? Signup!
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;