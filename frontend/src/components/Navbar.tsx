import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Phone, User, Menu, X, ChevronDown } from "lucide-react";
import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { useAppDispatch } from "../store/store";
import { logout } from "../features/auth/authSlice";
import ThemeToggle from "./ThemeToggle";
import logo from "../assets/images/just-logo.png";
import LanguageModal from "./LanguageModal";
import { Globe } from "lucide-react";

export default function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isResourcesOpen, setIsResourcesOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isLanguageModalOpen, setIsLanguageModalOpen] = useState(false);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const userMenuRef = useRef<HTMLDivElement | null>(null);

  const { isAuthenticated, user } = useSelector((s: RootState) => s.auth);
  const isDoctor = user?.role?.toUpperCase() === "DOCTOR";

  const mainLinks = [
    { name: "Find Hospitals", to: "/find-hospitals" },
    { name: "Emergency", to: "/emergency" },
  ];

  const resourceLinks = [
    { name: "Health Resources", to: "/resources" },
    { name: "Blog", to: "/blog" },
    { name: "FAQs", to: "/faqs" },
  ];

  const userLinks = isDoctor
    ? []
    : [
        { name: "My Profile", to: "/profile" },
        { name: "My Appointments", to: "/my-appointments" },
        { name: "Medical Reports", to: "/reports" },
      ];

  // Close dropdown on outside click
  useEffect(() => {
    if (!isUserMenuOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isUserMenuOpen]);

  const handleSignOut = async () => {
    await dispatch(logout());
    navigate("/");
  };

  return (
    <nav className="bg-white dark:bg-slate-900 shadow-md sticky top-0 z-50 border-b dark:border-slate-800 transition-colors duration-200 pt-[env(safe-area-inset-top)]">
      <div className="container mx-auto px-4">
        <div className="flex items-center h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-1.5">
            <img
              src={logo}
              alt="HospiiCo Logo"
              className="w-auto object-contain"
              style={{ height: '40px', maxHeight: '40px' }}
            />
            <span className="text-3xl font-bold tracking-tight flex items-center pb-1">
              <span className="text-slate-800 dark:text-slate-100">Hospii</span>
              <span className="text-blue-500">Co</span>
            </span>
          </Link>

          {/* Desktop Menu */}
          <div className="hidden lg:flex items-center space-x-8 ml-12">
            {!isDoctor && (
              <>
                {mainLinks.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className="text-gray-700 dark:text-slate-200 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                  >
                    {link.name}
                  </Link>
                ))}

                {/* Resources Dropdown (hover-based) */}
                <div
                  className="relative"
                  onMouseEnter={() => setIsResourcesOpen(true)}
                  onMouseLeave={() => setIsResourcesOpen(false)}
                >
                  <button className="flex items-center text-gray-700 dark:text-slate-200 hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                    Resources <ChevronDown className="h-4 w-4 ml-1" />
                  </button>
                  {isResourcesOpen && (
                    <div className="absolute top-full left-0 w-48 bg-white dark:bg-slate-800 rounded-lg shadow-lg py-2 border dark:border-slate-700">
                      {resourceLinks.map((link) => (
                        <Link
                          key={link.to}
                          to={link.to}
                          className="block px-4 py-2 text-gray-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-slate-700"
                        >
                          {link.name}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              </>
            )}
          </div>

          {/* Desktop Right Menu */}
          <div className="hidden lg:flex items-center space-x-6 ml-auto">
            {/* Theme Toggle */}
            <div className="flex items-center gap-4">
              <button
                onClick={() => setIsLanguageModalOpen(true)}
                className="p-2 mr-2 text-gray-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-full transition-colors"
                title="Select Language"
              >
                <Globe className="h-5 w-5" />
              </button>
              <ThemeToggle />
            </div>

            <a
              href="tel:1234567890"
              className="flex items-center space-x-2 text-gray-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400"
            >
              <Phone className="h-5 w-5" />
              <span>1-234-567-890</span>
            </a>

            {/* Account Dropdown (click-based) */}
            {isAuthenticated ? (
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center space-x-2 text-gray-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400"
                >
                  <User className="h-5 w-5" />
                  <span>Account</span>
                  <ChevronDown className="h-4 w-4" />
                </button>

                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-slate-800 rounded-lg shadow-lg py-2 border dark:border-slate-700">
                    {userLinks.map((link) => (
                      <Link
                        key={link.to}
                        to={link.to}
                        onClick={() => setIsUserMenuOpen(false)}
                        className="block px-4 py-2 text-gray-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-slate-700"
                      >
                        {link.name}
                      </Link>
                    ))}
                    <hr className="my-2 border-gray-200 dark:border-slate-700" />
                    <button
                      className="block w-full text-left px-4 py-2 text-gray-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-slate-700"
                      onClick={(e) => {
                        e.preventDefault();
                        setIsUserMenuOpen(false);
                        handleSignOut();
                      }}
                    >
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <></>
            )}

            {!isDoctor && (
              <button className="px-4 py-2 bg-gradient-to-r from-blue-600 to-blue-500 text-white text-sm font-medium rounded-lg hover:from-blue-700 hover:to-blue-600 transition-all duration-200 shadow-md hover:shadow-lg">
                <Link
                  to="/find-hospitals"
                >
                  Book Appointment
                </Link>
              </button>
            )}
          </div>

          <LanguageModal
            isOpen={isLanguageModalOpen}
            onClose={() => setIsLanguageModalOpen(false)}
          />

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="lg:hidden ml-auto p-2 text-gray-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
          >
            {isMenuOpen ? (
              <X className="h-6 w-6" />
            ) : (
              <Menu className="h-6 w-6" />
            )}
          </button>
        </div>

        {/* Mobile Menu */}
        {isMenuOpen && (
          <div className="lg:hidden py-4 border-t border-gray-100 dark:border-slate-800 space-y-4">
            {/* Mobile Language Toggle */}
            <div className="flex items-center justify-between px-2">
              <span className="text-gray-700 dark:text-slate-200 font-medium">Language</span>
              <button
                onClick={() => setIsLanguageModalOpen(true)}
                className="p-2 text-gray-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-slate-800 rounded-full transition-colors"
                title="Select Language"
              >
                <Globe className="h-5 w-5" />
              </button>
            </div>

            {/* Mobile Theme Toggle */}
            <div className="flex items-center justify-between px-2">
              <span className="text-gray-700 dark:text-slate-200 font-medium">Theme</span>
              <ThemeToggle />
            </div>

            {!isDoctor && [...mainLinks, ...resourceLinks].map((link) => (
              <Link
                key={link.to}
                to={link.to}
                onClick={() => setIsMenuOpen(false)}
                className="block py-2 text-gray-700 dark:text-slate-200 hover:text-blue-600 dark:hover:text-blue-400"
              >
                {link.name}
              </Link>
            ))}

            {userLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                onClick={() => setIsMenuOpen(false)}
                className="block py-2 text-gray-700 dark:text-slate-200 hover:text-blue-600 dark:hover:text-blue-400"
              >
                {link.name}
              </Link>
            ))}

            {isAuthenticated && (
              <button
                onClick={() => {
                  handleSignOut();
                  setIsMenuOpen(false);
                }}
                className="block w-full text-left py-2 text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 font-medium transition-colors"
              >
                Sign Out
              </button>
            )}

            {!isDoctor && (
              <Link
                to="/find-hospitals"
                onClick={() => setIsMenuOpen(false)}
                className="block w-full text-center mt-4 px-6 py-2.5 bg-gradient-to-r from-blue-600 to-blue-500 text-white rounded-lg hover:from-blue-700 hover:to-blue-600 transition-all duration-200 shadow-md"
              >
                Book Appointment
              </Link>
            )}
          </div>
        )}
      </div>
    </nav>
  );
}
