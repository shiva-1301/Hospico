import HospitalSearch from "../components/HospitalSearch";
import CTASection from "../components/CTASection";
import ImageSlider from "../components/ImageSlider";
import { Link } from "react-router-dom";
import {
  Search,
  Calendar,
  Bell,
  Clock,
  BarChart as ChartBar,
  Lock,
} from "lucide-react";
import Footer from "../components/Footer";

const Dashboard = () => {
  return (
    <div className="bg-gray-50 dark:bg-slate-900 transition-colors duration-200">
      {/* HERO SECTION */}
      <div className="bg-gradient-to-r from-indigo-500 to-sky-500 dark:from-indigo-600 dark:to-blue-900 mx-3 px-4 sm:py-8 lg:py-12 transition-all duration-500">
        <div className="sm:px-15 lg:px-20">
          <h1 className="text-2xl sm:text-4xl lg:text-6xl font-bold mb-2 sm:mb-4 text-white">
            India's Healthcare Search Engine
          </h1>
          <p className="text-sm sm:text-base text-white mb-6">
            Connect with top rated hospitals and specialties in your area
          </p>
          <div className="mx-auto px-4 pt-6 sm:py-8 lg:py-12 -mt-8">
            <HospitalSearch />
          </div>
        </div>
      </div>

      {/* ACTION BUTTONS */}
      <div className="max-w-7xl mx-auto px-4 mt-16">
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            to="/find-hospitals"
            className="px-8 py-4 bg-gradient-to-r from-blue-600 to-blue-500 text-white rounded-xl text-lg font-semibold hover:from-blue-700 hover:to-blue-600 transition-all duration-300 text-center shadow-soft hover:shadow-hover dark:shadow-blue-500/20 transform hover:scale-[1.02]"
          >
            Book an Appointment
          </Link>
          <Link
            to="/partner-login"
            className="px-8 py-4 border-2 border-blue-600 text-blue-600 dark:text-blue-400 dark:border-blue-400 rounded-xl text-lg font-semibold hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-all duration-300 text-center shadow-soft hover:shadow-hover transform hover:scale-[1.02]"
          >
            Partner with Us
          </Link>
        </div>
      </div>

      {/* VALUE PROPOSITION SECTION */}
      <section className="bg-gradient-to-b from-blue-50/50 to-white dark:from-slate-900 dark:to-slate-800 py-16 mt-12 transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4">
          <div className="grid md:grid-cols-2 gap-12 lg:gap-20">
            {/* For Patients */}
            <div className="space-y-8">
              <h2 className="text-3xl font-bold text-gray-900 dark:text-white">For Patients</h2>
              <div className="space-y-12">
                <Feature
                  icon={<Search className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Find the Best, Instantly"
                  desc="Discover hospitals that match your exact needs — verified, rated, ready."
                />
                <Feature
                  icon={<Calendar className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Book In Seconds"
                  desc="Confirm appointments with your preferred provider, instantly — no calls, no queues."
                />
                <Feature
                  icon={<Bell className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Stay Informed, Always"
                  desc="Get real-time confirmations and reminders across Email, SMS, and WhatsApp."
                />
              </div>
            </div>

            {/* For Hospitals */}
            <div className="space-y-8">
              <h2 className="text-3xl font-bold text-gray-900 dark:text-white">
                For Hospitals
              </h2>
              <div className="space-y-12">
                <Feature
                  icon={<Clock className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Fill Your Slots, Effortlessly"
                  desc="Smart appointment automation to maximize your OPD and IPD flows."
                />
                <Feature
                  icon={<ChartBar className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Know. Grow. Repeat."
                  desc="Real-time dashboards to track bookings, patient behavior, and optimize operations."
                />
                <Feature
                  icon={<Lock className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                  title="Built for Scale, Built for Security"
                  desc="Fully encrypted, hospital-grade tech. Integrates smoothly with your HMS/EHR."
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* IMAGE SLIDER */}
      <div>
        <ImageSlider />
      </div>

      {/* CTA SECTION */}
      <div>
        <CTASection />
      </div>

      {/* Footer Section */}
      <div>
        <Footer />
      </div>
    </div>
  );
};

// Helper subcomponent for icons + text
const Feature = ({
  icon,
  title,
  desc,
}: {
  icon: React.ReactNode;
  title: string;
  desc: string;
}) => (
  <div className="flex items-start gap-4">
    <div className="bg-blue-50 dark:bg-slate-800 p-3 rounded-full flex-shrink-0 transition-colors duration-200">{icon}</div>
    <div>
      <h3 className="text-xl font-semibold mb-2 text-gray-900 dark:text-white">{title}</h3>
      <p className="text-gray-600 dark:text-gray-400">{desc}</p>
    </div>
  </div>
);

export default Dashboard;
