import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { Stethoscope } from "lucide-react";
import { useAppDispatch } from "../store/store";
import { doctorLogin } from "../features/auth/authSlice";
import type { RootState } from "../store/store";

const DoctorLogin = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, isAuthenticated, error, user } = useSelector((s: RootState) => s.auth);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  useEffect(() => {
    if (isAuthenticated && user?.role?.toUpperCase() === "DOCTOR") {
      navigate(`/doctor-dashboard/${user.id}`);
    }
  }, [isAuthenticated, navigate, user]);

  const handleDoctorLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const resultAction = await dispatch(doctorLogin({ email, password }));

    if (doctorLogin.fulfilled.match(resultAction)) {
      navigate(`/doctor-dashboard/${resultAction.payload.id}`);
    }
  };

  return (
    <div className="min-h-[calc(100vh-5rem)] flex items-center justify-center px-4 bg-gray-100 dark:bg-gray-900 transition-colors duration-200">
      <div className="w-full max-w-md bg-white dark:bg-slate-800 rounded-lg shadow-xl p-8 transition-colors duration-200">
        <div className="flex justify-center mb-3">
          <Stethoscope className="w-10 h-10 text-blue-600 dark:text-blue-400" />
        </div>

        <h2 className="text-2xl font-semibold mb-2 text-gray-900 dark:text-white text-center">
          Doctor Login
        </h2>
        <p className="text-gray-600 dark:text-gray-400 mb-6 text-center text-sm">
          Sign in with your doctor account credentials
        </p>

        {error && (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
            <p className="text-red-800 dark:text-red-300 text-sm">{error}</p>
          </div>
        )}

        <form onSubmit={handleDoctorLogin} className="space-y-4">
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
            className="w-full px-4 py-3 rounded-md bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium transition-colors"
            disabled={!email || !password || status === "loading"}
          >
            {status === "loading" ? "Signing in..." : "Login as Doctor"}
          </button>
        </form>

        <p className="text-xs sm:text-sm text-gray-600 dark:text-gray-400 text-center mt-6">
          <Link
            className="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
            to="/login"
          >
            Back to patient login
          </Link>
        </p>
      </div>
    </div>
  );
};

export default DoctorLogin;
