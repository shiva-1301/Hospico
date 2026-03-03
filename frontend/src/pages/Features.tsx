import { Calendar, Clock, Bell, Shield, Building2, BarChart as ChartBar, MessageSquare, Smartphone, Users, Database, Zap, LineChart } from 'lucide-react';

export default function Features() {
  return (
    <div className="space-y-16">
      {/* Hero Section */}
      <section className="text-center space-y-4 py-16 bg-gradient-to-b from-blue-50 to-white rounded-3xl">
        <h1 className="text-4xl font-bold text-gray-900">Platform Features</h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed px-4">
          A comprehensive solution for both patients and healthcare providers
        </p>
      </section>

      {/* For Patients */}
      <section className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold text-gray-900 mb-8">For Patients</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {[
            {
              icon: Building2,
              title: "Hospital Discovery",
              description: "Find the perfect healthcare facility based on location, specialties, and patient reviews"
            },
            {
              icon: Calendar,
              title: "Easy Booking",
              description: "Book appointments instantly with your preferred healthcare provider"
            },
            {
              icon: MessageSquare,
              title: "WhatsApp Integration",
              description: "Book and receive confirmations directly through WhatsApp"
            },
            {
              icon: Bell,
              title: "Smart Reminders",
              description: "Receive timely notifications about upcoming appointments"
            },
            {
              icon: Smartphone,
              title: "Mobile Access",
              description: "Access your healthcare information from any device"
            },
            {
              icon: Shield,
              title: "Secure Records",
              description: "Your medical data is protected with enterprise-grade security"
            }
          ].map((feature) => (
            <div key={feature.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
                <feature.icon className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="font-semibold text-lg mb-2">{feature.title}</h3>
              <p className="text-gray-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* For Hospitals */}
      <section className="max-w-6xl mx-auto">
        <h2 className="text-3xl font-bold text-gray-900 mb-8">For Hospitals</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {[
            {
              icon: ChartBar,
              title: "Analytics Dashboard",
              description: "Track patient flow, revenue, and operational metrics in real-time"
            },
            {
              icon: Database,
              title: "HMS Integration",
              description: "Seamlessly integrate with your existing hospital management system"
            },
            {
              icon: Clock,
              title: "Automated Scheduling",
              description: "Reduce administrative workload with smart scheduling"
            },
            {
              icon: Zap,
              title: "Resource Optimization",
              description: "Optimize staff and resource allocation based on demand"
            },
            {
              icon: LineChart,
              title: "Performance Insights",
              description: "Make data-driven decisions with comprehensive analytics"
            },
            {
              icon: Users,
              title: "Patient Management",
              description: "Streamline patient communication and follow-ups"
            }
          ].map((feature) => (
            <div key={feature.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
                <feature.icon className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="font-semibold text-lg mb-2">{feature.title}</h3>
              <p className="text-gray-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Integration Section */}
      <section className="bg-gradient-to-r from-blue-600 to-blue-500 text-white py-16 rounded-3xl">
        <div className="max-w-4xl mx-auto text-center px-4">
          <h2 className="text-3xl font-bold mb-4">Seamless Integration</h2>
          <p className="text-blue-100 mb-8">
            HospitalConnect integrates with your existing systems, making the transition smooth and efficient. Our platform supports major HMS/EHR systems and provides comprehensive API access.
          </p>
          <button className="px-8 py-3 bg-white text-blue-600 rounded-lg hover:bg-blue-50 transition-all duration-200 font-medium">
            View Integration Guide
          </button>
        </div>
      </section>
    </div>
  );
}