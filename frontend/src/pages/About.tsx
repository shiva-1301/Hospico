import { Target, Users, Award } from 'lucide-react';

export default function About() {
  return (
    <div className="space-y-16">
      {/* Mission & Vision */}
      <section className="text-center space-y-4 py-16 bg-gradient-to-b from-blue-50 to-white rounded-3xl">
        <h1 className="text-4xl font-bold text-gray-900">Our Mission</h1>
        <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed px-4">
          To revolutionize healthcare accessibility by creating seamless connections between patients and healthcare providers through innovative technology.
        </p>
        <div className="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto mt-12 px-4">
          <div className="bg-white p-6 rounded-xl shadow-sm">
            <div className="bg-blue-50 p-3 rounded-full w-fit mx-auto mb-4">
              <Target className="h-6 w-6 text-blue-600" />
            </div>
            <h3 className="font-semibold mb-2">Vision</h3>
            <p className="text-gray-600">To be the leading platform for hospital discovery and booking worldwide</p>
          </div>
          <div className="bg-white p-6 rounded-xl shadow-sm">
            <div className="bg-blue-50 p-3 rounded-full w-fit mx-auto mb-4">
              <Users className="h-6 w-6 text-blue-600" />
            </div>
            <h3 className="font-semibold mb-2">Purpose</h3>
            <p className="text-gray-600">Making quality healthcare accessible to everyone through technology</p>
          </div>
          <div className="bg-white p-6 rounded-xl shadow-sm">
            <div className="bg-blue-50 p-3 rounded-full w-fit mx-auto mb-4">
              <Award className="h-6 w-6 text-blue-600" />
            </div>
            <h3 className="font-semibold mb-2">Values</h3>
            <p className="text-gray-600">Trust, Innovation, and Patient-First Approach</p>
          </div>
        </div>
      </section>

      {/* Our Story */}
      <section className="max-w-4xl mx-auto px-4">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Our Story</h2>
        <div className="prose prose-lg max-w-none">
          <p className="text-gray-600 leading-relaxed">
            HospitalConnect was born from a simple observation: finding and booking the right healthcare provider shouldn't be complicated. Our founder experienced firsthand the challenges of navigating the healthcare system and knew there had to be a better way.
          </p>
          <p className="text-gray-600 leading-relaxed mt-4">
            What started as a simple idea has grown into a comprehensive platform that connects patients with healthcare providers, streamlines the booking process, and helps hospitals manage their operations more efficiently.
          </p>
        </div>
      </section>

      {/* Team Section */}
      <section className="max-w-4xl mx-auto px-4">
        <h2 className="text-3xl font-bold text-gray-900 mb-8">Meet Our Team</h2>
        <div className="grid md:grid-cols-3 gap-8">
          {[
            {
              name: "Sarah Johnson",
              role: "Founder & CEO",
              image: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80"
            },
            {
              name: "Michael Chen",
              role: "Chief Technology Officer",
              image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80"
            },
            {
              name: "Emily Rodriguez",
              role: "Head of Operations",
              image: "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&w=300&q=80"
            }
          ].map((member) => (
            <div key={member.name} className="text-center">
              <img
                src={member.image}
                alt={member.name}
                className="w-32 h-32 rounded-full mx-auto mb-4 object-cover"
              />
              <h3 className="font-semibold text-lg">{member.name}</h3>
              <p className="text-gray-600">{member.role}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}