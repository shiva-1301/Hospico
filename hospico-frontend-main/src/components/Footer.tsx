import { Link } from "react-router-dom";
import {
  Globe2,
  ChevronDown,
  Building2,
} from "lucide-react";
import SocialIcons from "./SocialIcons";
import { useState } from "react";

const footerLinks = {
  about: {
    title: "About Us",
    links: [
      { label: "About", href: "/about" },
      { label: "Our Mission", href: "/mission" },
      { label: "Join Our Team", href: "/careers" },
      { label: "Media Center", href: "/media" },
      { label: "Contact", href: "/contact" },
      { label: "News & Updates", href: "/news" },
    ],
  },
  patients: {
    title: "For Patients",
    links: [
      { label: "Find Hospitals", href: "/hospitals" },
      { label: "Book Appointments", href: "/appointments" },
      { label: "Health Resources", href: "/resources" },
      { label: "Patient Portal", href: "/portal" },
      { label: "Emergency Services", href: "/emergency" },
      { label: "Medical Records", href: "/records" },
      { label: "Download App", href: "/app" },
    ],
  },
  providers: {
    title: "For Healthcare Providers",
    links: [
      { label: "Register Hospital", href: "/register" },
      { label: "Provider Portal", href: "/provider-portal" },
      { label: "Hospital Dashboard", href: "/dashboard" },
      { label: "Quality Metrics", href: "/metrics" },
      { label: "Training Resources", href: "/training" },
      { label: "Partnership Programs", href: "/partnership" },
    ],
  },
  institutions: {
    title: "For Institutions",
    links: [
      { label: "Enterprise Solutions", href: "/enterprise" },
      { label: "Hospital Management System", href: "/hms" },
      { label: "Analytics Dashboard", href: "/analytics" },
      { label: "Marketing Tools", href: "/marketing" },
      { label: "Certification Program", href: "/certification" },
    ],
  },
  legal: {
    title: "Support & Legal",
    links: [
      { label: "Help Center", href: "/help" },
      { label: "Privacy Policy", href: "/privacy" },
      { label: "Terms of Service", href: "/terms" },
      { label: "User Guidelines", href: "/guidelines" },
      { label: "Cookie Policy", href: "/cookies" },
      { label: "FAQs", href: "/faqs" },
    ],
  },
};



const regions = [
  "United States (English)",
  "Canada (English)",
  "United Kingdom (English)",
  "Australia (English)",
  "India (English)",
  "Deutschland (Deutsch)",
  "France (Français)",
  "España (Español)",
];

export default function Footer() {
  const [isRegionOpen, setIsRegionOpen] = useState(false);
  const [selectedRegion, setSelectedRegion] = useState(regions[0]);

  return (
    <footer className="bg-gradient-to-br from-blue-900 to-blue-800 text-white mt-16">
      <div className="max-w-7xl mx-auto px-4 py-12">
        {/* Logo and Description */}
        <div className="mb-12">
          <Link to="/" className="flex items-center space-x-2 mb-4">
            <div className="bg-white/10 p-2 rounded-lg">
              <Building2 className="h-6 w-6 text-white" />
            </div>
            <span className="text-xl font-bold">HospiiCo</span>
          </Link>
          <p className="text-blue-100 max-w-md">
            Revolutionizing healthcare accessibility by connecting patients with
            healthcare providers through innovative technology.
          </p>
        </div>

        {/* Main Links Grid */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-8 mb-12">
          {Object.values(footerLinks).map((section) => (
            <div key={section.title} className="col-span-1">
              <h3 className="font-semibold mb-4 text-lg">{section.title}</h3>
              <ul className="space-y-3">
                {section.links.map((link) => (
                  <li key={link.label}>
                    <Link
                      to={link.href}
                      className="text-blue-100 hover:text-white transition-colors duration-200 text-sm block"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Social Links */}
        <div className="flex flex-col md:flex-row justify-between items-center border-t border-blue-700 pt-8">
          <SocialIcons />

          {/* Region Selector */}
          <div className="relative">
            <button
              onClick={() => setIsRegionOpen(!isRegionOpen)}
              className="flex items-center space-x-2 bg-blue-800/50 hover:bg-blue-700 transition-colors duration-200 px-4 py-2 rounded-lg text-sm"
            >
              <Globe2 className="h-4 w-4" />
              <span>{selectedRegion}</span>
              <ChevronDown
                className={`h-4 w-4 transition-transform duration-200 ${isRegionOpen ? "rotate-180" : ""}`}
              />
            </button>

            {isRegionOpen && (
              <div className="absolute bottom-full mb-2 right-0 bg-white text-gray-900 rounded-lg shadow-lg py-2 min-w-[240px]">
                {regions.map((region) => (
                  <button
                    key={region}
                    onClick={() => {
                      setSelectedRegion(region);
                      setIsRegionOpen(false);
                    }}
                    className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition-colors duration-200"
                  >
                    {region}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Copyright */}
        <div className="text-center mt-8 text-sm text-blue-100">
          <p>&copy; {new Date().getFullYear()} HospiiCo. All rights reserved.</p>
        </div>
      </div>
    </footer >
  );
}
