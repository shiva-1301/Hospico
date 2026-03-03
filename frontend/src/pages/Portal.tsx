import {
  User,
  Calendar,
  FileText,
  MessageSquare,
  Settings,
  Bell,
  Clock,
} from "lucide-react";

export default function Portal() {
  return (
    <div className="max-w-7xl mx-auto px-4">
      {/* Dashboard Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Patient Portal</h1>
          <p className="text-gray-600">Welcome back, John Doe</p>
        </div>
        <div className="mt-4 md:mt-0 flex items-center gap-4">
          <button className="p-2 text-gray-600 hover:text-blue-600 relative">
            <Bell className="h-6 w-6" />
            <span className="absolute top-0 right-0 h-3 w-3 bg-red-500 rounded-full"></span>
          </button>
          <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
            <Calendar className="h-5 w-5" />
            Book Appointment
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid md:grid-cols-4 gap-6 mb-8">
        {[
          { label: "Upcoming Appointments", value: "2", icon: Calendar },
          { label: "Recent Reports", value: "3", icon: FileText },
          { label: "Unread Messages", value: "5", icon: MessageSquare },
          { label: "Next Check-up", value: "15 Days", icon: Clock },
        ].map((stat) => (
          <div
            key={stat.label}
            className="bg-white p-6 rounded-xl shadow-sm border border-gray-100"
          >
            <div className="flex items-center gap-4">
              <div className="bg-blue-50 p-3 rounded-full">
                <stat.icon className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-600">{stat.label}</p>
                <p className="text-2xl font-semibold">{stat.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Main Content */}
      <div className="grid lg:grid-cols-3 gap-8">
        {/* Appointments */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-xl font-semibold mb-4">
              Upcoming Appointments
            </h2>
            <div className="space-y-4">
              {[
                {
                  doctor: "Dr. Sarah Chen",
                  specialty: "Cardiology",
                  date: "March 15, 2024",
                  time: "10:00 AM",
                },
                {
                  doctor: "Dr. Michael Rodriguez",
                  specialty: "General Check-up",
                  date: "March 20, 2024",
                  time: "2:30 PM",
                },
              ].map((appointment) => (
                <div
                  key={appointment.date}
                  className="flex items-center justify-between p-4 border border-gray-100 rounded-lg"
                >
                  <div>
                    <p className="font-medium">{appointment.doctor}</p>
                    <p className="text-sm text-gray-600">
                      {appointment.specialty}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">{appointment.date}</p>
                    <p className="text-sm text-gray-600">{appointment.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-xl font-semibold mb-4">
              Recent Medical Reports
            </h2>
            <div className="space-y-4">
              {[
                {
                  title: "Blood Test Results",
                  date: "March 1, 2024",
                  doctor: "Dr. Emily White",
                },
                {
                  title: "X-Ray Report",
                  date: "February 25, 2024",
                  doctor: "Dr. James Wilson",
                },
                {
                  title: "Annual Check-up Summary",
                  date: "February 15, 2024",
                  doctor: "Dr. Sarah Chen",
                },
              ].map((report) => (
                <div
                  key={report.title}
                  className="flex items-center justify-between p-4 border border-gray-100 rounded-lg"
                >
                  <div className="flex items-center gap-3">
                    <FileText className="h-5 w-5 text-blue-600" />
                    <div>
                      <p className="font-medium">{report.title}</p>
                      <p className="text-sm text-gray-600">
                        By {report.doctor}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-gray-600">{report.date}</p>
                    <button className="text-blue-600 text-sm hover:text-blue-700">
                      View
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-xl font-semibold mb-4">Profile Summary</h2>
            <div className="flex items-center gap-4 mb-6">
              <div className="bg-blue-100 p-4 rounded-full">
                <User className="h-8 w-8 text-blue-600" />
              </div>
              <div>
                <p className="font-medium">John Doe</p>
                <p className="text-sm text-gray-600">Patient ID: 123456</p>
              </div>
            </div>
            <button className="w-full py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors">
              View Full Profile
            </button>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
            <div className="space-y-3">
              {[
                { label: "Message Doctor", icon: MessageSquare },
                { label: "View Medical History", icon: FileText },
                { label: "Update Information", icon: Settings },
              ].map((action) => (
                <button
                  key={action.label}
                  className="w-full py-2 px-4 text-left border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors flex items-center gap-3"
                >
                  <action.icon className="h-5 w-5" />
                  {action.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
