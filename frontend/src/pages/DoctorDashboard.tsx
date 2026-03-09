import { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import {
  Calendar,
  Clock,
  User,
  Users,
  CheckCircle,
  AlertCircle,
  ChevronDown,
  X,
  Phone,
  Mail,
  Stethoscope,
  PlusCircle,
  ClipboardList,
} from "lucide-react";
import type { RootState } from "../store/store";
import { apiRequest } from "../api";

// ── Types ────────────────────────────────────────────────────────────────────

type Appointment = {
  id: string;
  appointmentTime: string;
  status: string;
  patientName: string;
  patientAge: number;
  patientGender: string;
  patientEmail?: string;
  patientPhone?: string;
  clinicName: string;
  doctorName: string;
  doctorSpecialization?: string;
  reason?: string;
};

type LeaveRequest = {
  id: string;
  doctorId: string;
  startDate: string;
  endDate: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
};

const STATUS_OPTIONS = ["BOOKED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW"];

// ── Helper components ─────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = {
    BOOKED: "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300",
    IN_PROGRESS: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",
    COMPLETED: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300",
    CANCELLED: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300",
    NO_SHOW: "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300",
    PENDING: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",
    APPROVED: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300",
    REJECTED: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300",
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${map[status] ?? "bg-gray-100 text-gray-600"}`}>
      {status}
    </span>
  );
}

function StatCard({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: number;
  color: string;
}) {
  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl p-5 shadow-sm flex items-center gap-4 border border-gray-100 dark:border-slate-700">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>{icon}</div>
      <div>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
      </div>
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────

export default function DoctorDashboard() {
  const { user } = useSelector((s: RootState) => s.auth);

  // Use doctorId from Redux user, fallback for demo
  const doctorId: string = (user as { doctorId?: string; id?: string } | null)?.doctorId
    ?? (user as { id?: string } | null)?.id
    ?? "26566000000096005";

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [leaves, setLeaves] = useState<LeaveRequest[]>([]);
  const [loadingAppts, setLoadingAppts] = useState(true);
  const [loadingLeaves, setLoadingLeaves] = useState(true);
  const [selectedAppt, setSelectedAppt] = useState<Appointment | null>(null);
  const [updatingStatus, setUpdatingStatus] = useState<string | null>(null);

  // Leave form state
  const [leaveForm, setLeaveForm] = useState({ startDate: "", endDate: "", reason: "" });
  const [submittingLeave, setSubmittingLeave] = useState(false);
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const [leaveSuccess, setLeaveSuccess] = useState(false);

  // ── Data fetching ───────────────────────────────────────────────────────────

  async function fetchAppointments() {
    setLoadingAppts(true);
    try {
      const data = await apiRequest<Appointment[]>(
        `/api/appointments/doctor/${doctorId}/date/${new Date().toISOString().split("T")[0]}`,
        "GET"
      );
      setAppointments(Array.isArray(data) ? data : []);
    } catch {
      setAppointments([]);
    } finally {
      setLoadingAppts(false);
    }
  }

  async function fetchLeaves() {
    setLoadingLeaves(true);
    try {
      const data = await apiRequest<LeaveRequest[]>(`/api/doctor-leaves/doctor/${doctorId}`, "GET");
      setLeaves(Array.isArray(data) ? data : []);
    } catch {
      setLeaves([]);
    } finally {
      setLoadingLeaves(false);
    }
  }

  useEffect(() => {
    fetchAppointments();
    fetchLeaves();
  }, [doctorId]);

  // ── Appointment status update ───────────────────────────────────────────────

  async function handleStatusChange(apptId: string, newStatus: string) {
    setUpdatingStatus(apptId);
    try {
      await apiRequest(`/api/appointments/${apptId}/status?status=${newStatus}`, "PUT");
      setAppointments((prev) =>
        prev.map((a) => (a.id === apptId ? { ...a, status: newStatus } : a))
      );
    } catch {
      // silently fail — keep UI consistent
    } finally {
      setUpdatingStatus(null);
    }
  }

  // ── Leave request submit ────────────────────────────────────────────────────

  async function handleLeaveSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!leaveForm.startDate || !leaveForm.endDate || !leaveForm.reason.trim()) {
      setLeaveError("Please fill in all fields.");
      return;
    }
    setSubmittingLeave(true);
    setLeaveError(null);
    try {
      await apiRequest("/api/doctor-leaves/request", "POST", {
        doctorId,
        startDate: leaveForm.startDate,
        endDate: leaveForm.endDate,
        reason: leaveForm.reason,
      });
      setLeaveSuccess(true);
      setLeaveForm({ startDate: "", endDate: "", reason: "" });
      fetchLeaves();
      setTimeout(() => setLeaveSuccess(false), 3000);
    } catch {
      setLeaveError("Failed to submit. Please try again.");
    } finally {
      setSubmittingLeave(false);
    }
  }

  // ── Derived stats ───────────────────────────────────────────────────────────

  const total = appointments.length;
  const completed = appointments.filter((a) => a.status === "COMPLETED").length;
  const upcoming = appointments.filter((a) => a.status === "BOOKED").length;
  const inProgress = appointments.filter((a) => a.status === "IN_PROGRESS").length;

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-900 transition-colors duration-200">
      {/* Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-sky-500 px-4 sm:px-8 py-8">
        <div className="max-w-6xl mx-auto">
          <h1 className="text-2xl sm:text-3xl font-bold text-white">Doctor Dashboard</h1>
          <p className="text-indigo-100 text-sm mt-1">
            Welcome back, Dr. {user?.name ?? "Doctor"} · {new Date().toDateString()}
          </p>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-8 py-8 space-y-8">
        {/* ── Stats Row ─────────────────────────────────────────────────────── */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <StatCard
            icon={<Users className="w-6 h-6 text-blue-600" />}
            label="Total Today"
            value={total}
            color="bg-blue-50 dark:bg-blue-900/30"
          />
          <StatCard
            icon={<Calendar className="w-6 h-6 text-indigo-600" />}
            label="Upcoming"
            value={upcoming}
            color="bg-indigo-50 dark:bg-indigo-900/30"
          />
          <StatCard
            icon={<Clock className="w-6 h-6 text-yellow-600" />}
            label="In Progress"
            value={inProgress}
            color="bg-yellow-50 dark:bg-yellow-900/30"
          />
          <StatCard
            icon={<CheckCircle className="w-6 h-6 text-green-600" />}
            label="Completed"
            value={completed}
            color="bg-green-50 dark:bg-green-900/30"
          />
        </div>

        {/* ── Today's Appointments Table ──────────────────────────────────── */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-100 dark:border-slate-700 flex items-center gap-2">
            <Stethoscope className="w-5 h-5 text-indigo-500" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Today's Appointments</h2>
          </div>

          {loadingAppts ? (
            <div className="p-8 text-center text-gray-400 animate-pulse">Loading appointments…</div>
          ) : appointments.length === 0 ? (
            <div className="p-8 text-center text-gray-400 dark:text-gray-500">
              No appointments scheduled for today.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 dark:bg-slate-700/50">
                  <tr>
                    {["Patient", "Time", "Reason", "Status", "Actions"].map((h) => (
                      <th
                        key={h}
                        className="px-4 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50 dark:divide-slate-700">
                  {appointments.map((appt) => (
                    <tr key={appt.id} className="hover:bg-gray-50 dark:hover:bg-slate-700/30 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">
                        {appt.patientName}
                        {appt.patientAge && (
                          <span className="ml-1 text-xs text-gray-400">({appt.patientAge}y)</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-gray-600 dark:text-gray-300 whitespace-nowrap">
                        {appt.appointmentTime
                          ? new Date(appt.appointmentTime).toLocaleTimeString([], {
                              hour: "2-digit",
                              minute: "2-digit",
                            })
                          : "—"}
                      </td>
                      <td className="px-4 py-3 text-gray-600 dark:text-gray-400 max-w-[160px] truncate">
                        {appt.reason ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={appt.status} />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => setSelectedAppt(appt)}
                            className="text-indigo-600 dark:text-indigo-400 hover:underline text-xs font-medium"
                          >
                            View
                          </button>
                          <div className="relative">
                            <select
                              className="appearance-none text-xs border border-gray-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-700 dark:text-gray-200 rounded-lg px-2 py-1 pr-6 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50"
                              value={appt.status}
                              disabled={updatingStatus === appt.id}
                              onChange={(e) => handleStatusChange(appt.id, e.target.value)}
                            >
                              {STATUS_OPTIONS.map((s) => (
                                <option key={s} value={s}>
                                  {s}
                                </option>
                              ))}
                            </select>
                            <ChevronDown className="w-3 h-3 absolute right-1 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400" />
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* ── Leave Request + History ──────────────────────────────────────── */}
        <div className="grid sm:grid-cols-2 gap-6">
          {/* Request Form */}
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
            <div className="flex items-center gap-2 mb-5">
              <PlusCircle className="w-5 h-5 text-indigo-500" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Request Leave</h2>
            </div>
            <form onSubmit={handleLeaveSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                  Start Date
                </label>
                <input
                  type="date"
                  className="w-full border border-gray-200 dark:border-slate-600 bg-white dark:bg-slate-700 rounded-lg px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-400"
                  value={leaveForm.startDate}
                  min={new Date().toISOString().split("T")[0]}
                  onChange={(e) => setLeaveForm((f) => ({ ...f, startDate: e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                  End Date
                </label>
                <input
                  type="date"
                  className="w-full border border-gray-200 dark:border-slate-600 bg-white dark:bg-slate-700 rounded-lg px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-400"
                  value={leaveForm.endDate}
                  min={leaveForm.startDate || new Date().toISOString().split("T")[0]}
                  onChange={(e) => setLeaveForm((f) => ({ ...f, endDate: e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                  Reason
                </label>
                <textarea
                  rows={3}
                  placeholder="Brief reason for leave…"
                  className="w-full border border-gray-200 dark:border-slate-600 bg-white dark:bg-slate-700 rounded-lg px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-400 resize-none"
                  value={leaveForm.reason}
                  onChange={(e) => setLeaveForm((f) => ({ ...f, reason: e.target.value }))}
                />
              </div>
              {leaveError && (
                <p className="text-red-500 text-xs flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" /> {leaveError}
                </p>
              )}
              {leaveSuccess && (
                <p className="text-green-600 dark:text-green-400 text-xs flex items-center gap-1">
                  <CheckCircle className="w-3 h-3" /> Leave request submitted successfully!
                </p>
              )}
              <button
                type="submit"
                disabled={submittingLeave}
                className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-medium py-2 rounded-xl text-sm transition-colors"
              >
                {submittingLeave ? "Submitting…" : "Submit Request"}
              </button>
            </form>
          </div>

          {/* Leave History */}
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
            <div className="flex items-center gap-2 mb-5">
              <ClipboardList className="w-5 h-5 text-indigo-500" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Leave History</h2>
            </div>
            {loadingLeaves ? (
              <div className="text-center text-gray-400 animate-pulse py-6">Loading…</div>
            ) : leaves.length === 0 ? (
              <div className="text-center text-gray-400 dark:text-gray-500 py-6 text-sm">
                No leave requests yet.
              </div>
            ) : (
              <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
                {leaves.map((l) => (
                  <div
                    key={l.id}
                    className="rounded-xl border border-gray-100 dark:border-slate-700 p-3 flex items-start justify-between gap-2"
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">{l.reason}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                        {l.startDate} → {l.endDate}
                      </p>
                    </div>
                    <StatusBadge status={l.status} />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Patient Details Modal ───────────────────────────────────────────── */}
      {selectedAppt && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={() => setSelectedAppt(null)}
        >
          <div
            className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl w-full max-w-sm p-6 relative"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
              onClick={() => setSelectedAppt(null)}
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Patient Details</h3>
            <div className="space-y-3 text-sm">
              <Row icon={<User className="w-4 h-4 text-indigo-500" />} label="Name" value={selectedAppt.patientName} />
              <Row
                icon={<Calendar className="w-4 h-4 text-indigo-500" />}
                label="Age / Gender"
                value={`${selectedAppt.patientAge ?? "—"} · ${selectedAppt.patientGender ?? "—"}`}
              />
              {selectedAppt.patientPhone && (
                <Row icon={<Phone className="w-4 h-4 text-indigo-500" />} label="Phone" value={selectedAppt.patientPhone} />
              )}
              {selectedAppt.patientEmail && (
                <Row icon={<Mail className="w-4 h-4 text-indigo-500" />} label="Email" value={selectedAppt.patientEmail} />
              )}
              <Row
                icon={<Clock className="w-4 h-4 text-indigo-500" />}
                label="Time"
                value={
                  selectedAppt.appointmentTime
                    ? new Date(selectedAppt.appointmentTime).toLocaleString()
                    : "—"
                }
              />
              {selectedAppt.reason && (
                <Row
                  icon={<Stethoscope className="w-4 h-4 text-indigo-500" />}
                  label="Reason"
                  value={selectedAppt.reason}
                />
              )}
              <div className="pt-2">
                <StatusBadge status={selectedAppt.status} />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Row({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-2">
      <span className="mt-0.5">{icon}</span>
      <div>
        <span className="text-gray-500 dark:text-gray-400">{label}: </span>
        <span className="text-gray-900 dark:text-white font-medium">{value}</span>
      </div>
    </div>
  );
}
