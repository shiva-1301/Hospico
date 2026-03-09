import { Activity, Calendar, CalendarDays, Clock3, Plus, TrendingUp, Users, X } from "lucide-react";
import { useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { useNavigate, useParams } from "react-router-dom";
import { useAppDispatch } from "../store/store";
import { logout } from "../features/auth/authSlice";
import type { RootState } from "../store/store";

type DashboardTab = "appointments" | "slots" | "profile";

type DoctorSlot = {
  id: string;
  date: string;
  startTime: string;
  endTime: string;
};

type DoctorProfile = {
  fullName: string;
  hospital: string;
  specialization: string;
  experienceYears: string;
  bio: string;
};

type AppointmentItem = {
  id: string;
  patientName: string;
  dateTime: string;
  symptoms: string;
  status: "Upcoming" | "Completed";
};

const DoctorDashboard = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { doctorId: routeDoctorId } = useParams();
  const authUser = useSelector((s: RootState) => s.auth.user);
  const doctorName = authUser?.name || "Doctor";
  const effectiveDoctorId = routeDoctorId || authUser?.id || "doctor";
  const profileStorageKey = `doctor_profile_${effectiveDoctorId}`;
  const slotsStorageKey = `doctor_slots_${effectiveDoctorId}`;

  const [activeTab, setActiveTab] = useState<DashboardTab>("appointments");

  const [profile, setProfile] = useState<DoctorProfile>(() => {
    try {
      const saved = localStorage.getItem(profileStorageKey);
      if (saved) {
        return JSON.parse(saved) as DoctorProfile;
      }
    } catch {
    }
    return {
      fullName: doctorName,
      hospital: "HealthFirst Primary Care",
      specialization: "General Medicine",
      experienceYears: "20",
      bio: "Experienced general practitioner providing comprehensive primary care and health screenings.",
    };
  });

  const [selectedDate, setSelectedDate] = useState(() => formatDateForInput(new Date()));

  const [slots, setSlots] = useState<DoctorSlot[]>(() => {
    try {
      const saved = localStorage.getItem(slotsStorageKey);
      if (saved) {
        return JSON.parse(saved) as DoctorSlot[];
      }
    } catch {
    }

    const today = formatDateForInput(new Date());
    return [
      {
        id: crypto.randomUUID(),
        date: today,
        startTime: "18:10",
        endTime: "19:10",
      },
    ];
  });

  const [isSlotModalOpen, setIsSlotModalOpen] = useState(false);
  const [newSlotDate, setNewSlotDate] = useState(selectedDate);
  const [newSlotStartTime, setNewSlotStartTime] = useState("20:00");
  const [newSlotEndTime, setNewSlotEndTime] = useState("21:00");
  const [slotError, setSlotError] = useState("");

  const appointments: AppointmentItem[] = useMemo(
    () => [
      {
        id: "1",
        patientName: "Aadhya Rao",
        dateTime: `${selectedDate} 11:30`,
        symptoms: "Headache",
        status: "Completed",
      },
      {
        id: "2",
        patientName: "Vikram Nair",
        dateTime: `${selectedDate} 18:00`,
        symptoms: "Fever",
        status: "Upcoming",
      },
    ],
    [selectedDate]
  );

  const todayLabel = useMemo(() => {
    const now = new Date();
    return now.toLocaleDateString("en-US", {
      weekday: "long",
      month: "short",
      day: "numeric",
    });
  }, []);

  const daySlots = useMemo(
    () => slots.filter((slot) => slot.date === selectedDate).sort((a, b) => a.startTime.localeCompare(b.startTime)),
    [slots, selectedDate]
  );

  const totalAppointments = appointments.length;
  const completedAppointments = appointments.filter((a) => a.status === "Completed").length;

  const handleOpenSlotModal = () => {
    setNewSlotDate(selectedDate);
    setNewSlotStartTime("20:00");
    setNewSlotEndTime("21:00");
    setSlotError("");
    setIsSlotModalOpen(true);
  };

  const handleAddSlot = () => {
    if (!newSlotDate || !newSlotStartTime || !newSlotEndTime) {
      setSlotError("Date, start time and end time are required.");
      return;
    }

    if (newSlotEndTime <= newSlotStartTime) {
      setSlotError("End time must be after start time.");
      return;
    }

    const hasOverlap = slots.some(
      (slot) =>
        slot.date === newSlotDate &&
        !(newSlotEndTime <= slot.startTime || newSlotStartTime >= slot.endTime)
    );

    if (hasOverlap) {
      setSlotError("This slot overlaps with an existing slot.");
      return;
    }

    const updated = [
      ...slots,
      {
        id: crypto.randomUUID(),
        date: newSlotDate,
        startTime: newSlotStartTime,
        endTime: newSlotEndTime,
      },
    ];

    setSlots(updated);
    localStorage.setItem(slotsStorageKey, JSON.stringify(updated));
    setSelectedDate(newSlotDate);
    setIsSlotModalOpen(false);
  };

  const handleRemoveSlot = (slotId: string) => {
    const updated = slots.filter((slot) => slot.id !== slotId);
    setSlots(updated);
    localStorage.setItem(slotsStorageKey, JSON.stringify(updated));
  };

  const handleProfileSave = () => {
    localStorage.setItem(profileStorageKey, JSON.stringify(profile));
  };

  const handleLogout = async () => {
    await dispatch(logout());
    navigate("/doctor-login");
  };

  return (
    <div className="min-h-[calc(100vh-5rem)] bg-gray-100 dark:bg-slate-900 transition-colors duration-200 px-4 py-8 md:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <div className="bg-emerald-100 dark:bg-emerald-900/30 p-2 rounded-xl">
                <Activity className="h-7 w-7 text-emerald-500" />
              </div>
              <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Doctor Dashboard</h1>
            </div>
            <p className="text-lg text-slate-600 dark:text-slate-300">
              {profile.specialization} · {profile.hospital} · {profile.experienceYears} yrs experience
            </p>
          </div>

          <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl px-4 py-3 shadow-sm w-fit">
            <div className="flex items-center gap-3">
              <CalendarDays className="h-5 w-5 text-blue-500" />
              <div>
                <p className="text-sm text-slate-500 dark:text-slate-400">Today</p>
                <p className="font-semibold text-slate-900 dark:text-white">{todayLabel}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5 mb-8">
          <StatCard title="Today's Appointments" value={String(appointments.length)} icon={<Users className="h-6 w-6 text-emerald-500" />} tint="emerald" />
          <StatCard title="Total Appointments" value={String(totalAppointments)} icon={<CalendarDays className="h-6 w-6 text-blue-500" />} tint="blue" />
          <StatCard title="Active Slots" value={String(daySlots.length)} icon={<Clock3 className="h-6 w-6 text-orange-500" />} tint="orange" />
          <StatCard title="Completed" value={String(completedAppointments)} icon={<TrendingUp className="h-6 w-6 text-purple-500" />} tint="purple" />
        </div>

        <section className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl overflow-hidden shadow-sm">
          <div className="px-6 border-b border-slate-200 dark:border-slate-700">
            <div className="flex items-center gap-8">
              <button
                onClick={() => setActiveTab("appointments")}
                className={`py-4 text-lg font-medium border-b-2 ${
                  activeTab === "appointments"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-slate-500 dark:text-slate-400"
                }`}
              >
                Appointments
              </button>
              <button
                onClick={() => setActiveTab("slots")}
                className={`py-4 text-lg font-medium border-b-2 ${
                  activeTab === "slots"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-slate-500 dark:text-slate-400"
                }`}
              >
                Manage Slots
              </button>
              <button
                onClick={() => setActiveTab("profile")}
                className={`py-4 text-lg font-medium border-b-2 ${
                  activeTab === "profile"
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-slate-500 dark:text-slate-400"
                }`}
              >
                Profile
              </button>
            </div>
          </div>

          {activeTab === "appointments" && (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px]">
                <thead>
                  <tr className="bg-slate-50 dark:bg-slate-900/40">
                    <th className="text-left px-6 py-4 text-xs font-semibold tracking-wider text-slate-500 dark:text-slate-400 uppercase">Patient Name</th>
                    <th className="text-left px-6 py-4 text-xs font-semibold tracking-wider text-slate-500 dark:text-slate-400 uppercase">Date & Time</th>
                    <th className="text-left px-6 py-4 text-xs font-semibold tracking-wider text-slate-500 dark:text-slate-400 uppercase">Symptoms</th>
                    <th className="text-left px-6 py-4 text-xs font-semibold tracking-wider text-slate-500 dark:text-slate-400 uppercase">Status</th>
                    <th className="text-left px-6 py-4 text-xs font-semibold tracking-wider text-slate-500 dark:text-slate-400 uppercase">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((appointment) => (
                    <tr key={appointment.id} className="border-t border-slate-100 dark:border-slate-700">
                      <td className="px-6 py-4 text-slate-800 dark:text-slate-200">{appointment.patientName}</td>
                      <td className="px-6 py-4 text-slate-600 dark:text-slate-300">{appointment.dateTime}</td>
                      <td className="px-6 py-4 text-slate-600 dark:text-slate-300">{appointment.symptoms}</td>
                      <td className="px-6 py-4 text-slate-600 dark:text-slate-300">{appointment.status}</td>
                      <td className="px-6 py-4 text-slate-600 dark:text-slate-300">-</td>
                    </tr>
                  ))}
                  {appointments.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-6 py-10 text-center text-slate-500 dark:text-slate-400 text-lg">
                        No appointments found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {activeTab === "slots" && (
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-4xl font-bold text-slate-900 dark:text-white">Manage Slots</h2>
                <button
                  onClick={handleOpenSlotModal}
                  className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold"
                >
                  <Plus className="h-4 w-4" />
                  Add Slot
                </button>
              </div>

              <div className="max-w-sm mb-5">
                <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Date</label>
                <div className="relative">
                  <input
                    type="date"
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                  <Calendar className="absolute right-3 top-3.5 h-4 w-4 text-slate-400 pointer-events-none" />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {daySlots.map((slot) => (
                  <div key={slot.id} className="relative border border-slate-200 dark:border-slate-700 rounded-xl px-5 py-4 bg-white dark:bg-slate-900">
                    <button
                      onClick={() => handleRemoveSlot(slot.id)}
                      className="absolute right-2 top-2 text-red-500 hover:text-red-600"
                      aria-label="Remove slot"
                    >
                      <X className="h-4 w-4" />
                    </button>
                    <p className="text-2xl font-semibold text-slate-900 dark:text-white">{slot.startTime}</p>
                    <p className="text-slate-500 dark:text-slate-300 mt-1">{slot.endTime}</p>
                    <p className="text-emerald-600 font-medium mt-2">Available</p>
                  </div>
                ))}
                {daySlots.length === 0 && (
                  <p className="text-slate-500 dark:text-slate-400">No slots for selected date.</p>
                )}
              </div>
            </div>
          )}

          {activeTab === "profile" && (
            <div className="p-6 max-w-2xl mx-auto">
              <h2 className="text-4xl font-bold text-slate-900 dark:text-white mb-5">Edit Profile</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Full Name</label>
                  <input
                    value={profile.fullName}
                    onChange={(e) => setProfile((prev) => ({ ...prev, fullName: e.target.value }))}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Hospital</label>
                  <input
                    value={profile.hospital}
                    onChange={(e) => setProfile((prev) => ({ ...prev, hospital: e.target.value }))}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Specialization</label>
                  <input
                    value={profile.specialization}
                    onChange={(e) => setProfile((prev) => ({ ...prev, specialization: e.target.value }))}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Experience (years)</label>
                  <input
                    value={profile.experienceYears}
                    onChange={(e) => setProfile((prev) => ({ ...prev, experienceYears: e.target.value }))}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Bio</label>
                  <textarea
                    value={profile.bio}
                    onChange={(e) => setProfile((prev) => ({ ...prev, bio: e.target.value }))}
                    rows={4}
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 text-slate-900 dark:text-white"
                  />
                </div>

                <button
                  onClick={handleProfileSave}
                  className="w-full px-5 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold"
                >
                  Save Changes
                </button>
              </div>
            </div>
          )}
        </section>

        <div className="mt-6 flex justify-end">
          <button
            onClick={handleLogout}
            className="px-5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800"
          >
            Logout
          </button>
        </div>
      </div>

      {isSlotModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="w-full max-w-2xl rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 shadow-2xl">
            <div className="flex items-center justify-between px-6 py-5 border-b border-slate-200 dark:border-slate-700">
              <h3 className="text-3xl font-semibold text-slate-900 dark:text-white">Add Time Slot</h3>
              <button onClick={() => setIsSlotModalOpen(false)}>
                <X className="h-5 w-5 text-slate-500" />
              </button>
            </div>

            <div className="p-6 space-y-5">
              <div>
                <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Date</label>
                <input
                  type="date"
                  value={newSlotDate}
                  onChange={(e) => setNewSlotDate(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3"
                />
              </div>
              <div>
                <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">Start Time</label>
                <input
                  type="time"
                  value={newSlotStartTime}
                  onChange={(e) => setNewSlotStartTime(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3"
                />
              </div>
              <div>
                <label className="block text-sm mb-2 text-slate-600 dark:text-slate-300">End Time</label>
                <input
                  type="time"
                  value={newSlotEndTime}
                  onChange={(e) => setNewSlotEndTime(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3"
                />
              </div>

              {slotError && <p className="text-red-600 text-sm">{slotError}</p>}
            </div>

            <div className="flex justify-end gap-3 px-6 py-5 border-t border-slate-200 dark:border-slate-700">
              <button
                onClick={() => setIsSlotModalOpen(false)}
                className="px-6 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200"
              >
                Cancel
              </button>
              <button
                onClick={handleAddSlot}
                className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold"
              >
                Add Slot
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const formatDateForInput = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const StatCard = ({
  title,
  value,
  icon,
  tint,
}: {
  title: string;
  value: string;
  icon: React.ReactNode;
  tint: "emerald" | "blue" | "orange" | "purple";
}) => {
  const tintMap = {
    emerald: "border-emerald-200 dark:border-emerald-900/40 bg-emerald-50/20 dark:bg-emerald-900/10",
    blue: "border-blue-200 dark:border-blue-900/40 bg-blue-50/20 dark:bg-blue-900/10",
    orange: "border-orange-200 dark:border-orange-900/40 bg-orange-50/20 dark:bg-orange-900/10",
    purple: "border-purple-200 dark:border-purple-900/40 bg-purple-50/20 dark:bg-purple-900/10",
  };

  const iconTintMap = {
    emerald: "bg-emerald-100 dark:bg-emerald-900/40",
    blue: "bg-blue-100 dark:bg-blue-900/40",
    orange: "bg-orange-100 dark:bg-orange-900/40",
    purple: "bg-purple-100 dark:bg-purple-900/40",
  };

  return (
    <article className={`rounded-2xl border p-6 ${tintMap[tint]}`}>
      <div className="flex items-start justify-between mb-3">
        <p className="text-slate-600 dark:text-slate-300 text-lg font-medium">{title}</p>
        <div className={`rounded-xl p-3 ${iconTintMap[tint]}`}>{icon}</div>
      </div>
      <p className="text-4xl font-bold text-slate-900 dark:text-white">{value}</p>
    </article>
  );
};

export default DoctorDashboard;
