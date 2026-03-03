import { Calendar, Clock, Bell, Shield, Building2, BarChart as ChartBar, Users, Smartphone } from 'lucide-react';

export default function Services() {
  return (
    <div className="space-y-16">
      {/* Hero Section */}
      <section className="text-center space-y-4 py-16 bg-gradient-to-b from-blue-50 to-white rounded-3xl">
        <h1 className="text-4xl font-bold text-gray-900">Our Services</h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed px-4">
          Comprehensive solutions for both patients and healthcare providers
        </p>
      </section>

      {/* For Patients */}
      <section className="max-w-6xl mx-auto px-4">
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
              title: "Online Booking",
              description: "Book appointments instantly with your preferred healthcare provider"
            },
            {
              icon: Bell,
              title: "Smart Reminders",
              description: "Receive timely notifications about upcoming appointments and follow-ups"
            },
            {
              icon: Shield,
              title: "Secure Records",
              description: "Access your medical history and reports securely from anywhere"
            },
            {
              icon: Smartphone,
              title: "Mobile Access",
              description: "Manage your healthcare journey from our mobile-friendly platform"
            },
            {
              icon: Users,
              title: "Family Management",
              description: "Manage healthcare for your entire family from a single account"
            }
          ].map((service) => (
            <div key={service.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
                <service.icon className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="font-semibold text-lg mb-2">{service.title}</h3>
              <p className="text-gray-600">{service.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* For Hospitals */}
      <section className="max-w-6xl mx-auto px-4">
        <h2 className="text-3xl font-bold text-gray-900 mb-8">For Hospitals</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          {[
            {
              icon: Calendar,
              title: "Appointment Management",
              description: "Streamline your appointment scheduling with our automated system"
            },
            {
              icon: ChartBar,
              title: "Analytics Dashboard",
              description: "Track patient flow, revenue, and other key metrics in real-time"
            },
            {
              icon: Clock,
              title: "Automated Reminders",
              description: "Reduce no-shows with automated patient reminders"
            }
          ].map((service) => (
            <div key={service.title} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="bg-blue-50 p-3 rounded-full w-fit mb-4">
                <service.icon className="h-6 w-6 text-blue-600" />
              </div>
              <h3 className="font-semibold text-lg mb-2">{service.title}</h3>
              <p className="text-gray-600">{service.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-blue-50 rounded-3xl p-8 max-w-6xl mx-auto">
        <div className="text-center max-w-3xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Ready to Transform Your Healthcare Experience?</h2>
          <p className="text-gray-600 mb-6">Join thousands of patients and healthcare providers who trust HospitalConnect</p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button className="px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 text-white rounded-lg hover:from-blue-700 hover:to-blue-600">
              Find a Hospital
            </button>
            <button className="px-6 py-3 border border-blue-600 text-blue-600 rounded-lg hover:bg-blue-50">
              Partner with Us
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}