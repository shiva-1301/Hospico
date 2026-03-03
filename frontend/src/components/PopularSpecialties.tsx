import { Link } from "react-router-dom";
import { ChevronRight, Heart, Brain, Stethoscope } from "lucide-react";

const specialties = [
  { icon: Heart, name: "Cardiology", count: "45 Specialists" },
  { icon: Brain, name: "Neurology", count: "32 Specialists" },
  { icon: Stethoscope, name: "General Medicine", count: "78 Doctors" },
];

export default function PopularSpecialties() {
  return (
    <section className="max-w-7xl mx-auto px-4">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-3xl font-bold">Popular Specialties</h2>
        <Link
          to="/specialties"
          className="text-blue-600 hover:text-blue-700 flex items-center gap-1"
        >
          View All
          <ChevronRight className="h-4 w-4" />
        </Link>
      </div>
      <div className="grid md:grid-cols-3 gap-6">
        {specialties.map((specialty) => (
          <div
            key={specialty.name}
            className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-all duration-200 hover:-translate-y-1"
          >
            <div className="flex items-center gap-4">
              <div className="bg-blue-50 p-3 rounded-full">
                <specialty.icon className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <h3 className="font-semibold text-lg">{specialty.name}</h3>
                <p className="text-gray-600">{specialty.count}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
