import { Link } from "react-router-dom";

export default function CTASection() {
  return (
    <section className="bg-gray-50 dark:bg-slate-900 py-16 transition-colors duration-200">
      <div className="max-w-4xl mx-auto text-center px-4">
        <h2 className="text-3xl font-bold mb-4 dark:text-white">Ready to Get Started?</h2>
        <p className="text-gray-600 dark:text-gray-300 mb-8">
          Join thousands of patients who trust HospiiCo for their healthcare
          needs
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            to="/hospitals"
            className="px-8 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Find a Hospital
          </Link>
          <Link
            to="/register"
            className="px-8 py-3 border-2 border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors"
          >
            Register Your Hospital
          </Link>
        </div>
      </div>
    </section>
  );
}
