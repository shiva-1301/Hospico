import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Calendar, Clock, User as UserIcon, Phone, Mail, Stethoscope, X } from "lucide-react";
import type { RootState } from "../store/store";
import { apiRequest } from "../api";
import ReviewModal from "../components/ReviewModal";

type Appointment = {
  id: string;
  appointmentTime: string;
  status: string;
  patientName: string;
  patientAge: number;
  patientGender: string;
  patientEmail?: string;
  patientPhone?: string;
  clinicId: string;
  clinicName: string;
  doctorId: string;
  doctorName: string;
  doctorSpecialization?: string;
  userName: string;
  reason?: string;
};

export default function MyAppointments() {
  const { user } = useSelector((state: RootState) => state.auth);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [reviewModal, setReviewModal] = useState<{ isOpen: boolean; appointment: Appointment | null }>({ isOpen: false, appointment: null });

  const handleReviewSubmit = async (rating: number, comment: string) => {
    if (!reviewModal.appointment) return;

    try {
      await apiRequest("/api/reviews", "POST", {
        rating,
        comment,
        userId: user?.id,
        hospitalId: Number(reviewModal.appointment.clinicId),
        doctorId: Number(reviewModal.appointment.doctorId),
      });

      alert("Review submitted successfully!");
      fetchAppointments();
      setReviewModal({ isOpen: false, appointment: null });
    } catch (error: unknown) {
      console.error(error);
      const errorMessage = error instanceof Error ? error.message : "Failed to submit review";
      alert(errorMessage);
    }
  };

  // Add Review type locally or import if available
  interface Review {
    id: number;
    doctorId: number;
    clinicId: number;
    userId: number;
    // ... other fields
  }
  const [userReviews, setUserReviews] = useState<Review[]>([]);

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch appointments
      const appointmentsData = await apiRequest<Appointment[]>(`/api/appointments/user/${user?.id}`, "GET");
      const sortedAppointments = appointmentsData.sort((a, b) => new Date(a.appointmentTime).getTime() - new Date(b.appointmentTime).getTime());
      setAppointments(sortedAppointments);
      
      // Try to fetch reviews, but don't fail if it doesn't work
      try {
        const reviewsData = await apiRequest<Review[]>(`/api/reviews/user/${user?.id}`, "GET");
        setUserReviews(reviewsData);
      } catch (reviewErr) {
        console.warn("Could not fetch reviews:", reviewErr);
        setUserReviews([]);
      }
    } catch (err) {
      console.error("Error fetching appointments:", err);
      setError((err as Error)?.message || "Failed to load appointments");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.id) {
      fetchAppointments();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const handleCancelAppointment = async (appointmentId: string) => {
    if (!window.confirm("Are you sure you want to cancel this appointment?")) return;
    try {
      setCancellingId(appointmentId);
      await apiRequest(`/api/appointments/${appointmentId}`, "DELETE");
      setAppointments((prev) => prev.filter((apt) => apt.id !== appointmentId));
    } catch (err) {
      alert((err as Error)?.message || "Failed to cancel appointment");
    } finally {
      setCancellingId(null);
    }
  };

  const formatDate = (dateString: string) => new Date(dateString).toLocaleDateString("en-US", { weekday: "short", year: "numeric", month: "short", day: "numeric" });
  const formatTime = (dateString: string) => new Date(dateString).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: true });

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case "BOOKED":
        return "bg-green-600 text-white border-green-700 dark:bg-green-600/30 dark:text-green-300 dark:border-green-600";
      case "CANCELLED":
        return "bg-red-100 dark:bg-red-600/30 text-red-800 dark:text-red-300 border-red-200 dark:border-red-600";
      case "COMPLETED":
        return "bg-blue-100 dark:bg-blue-600/30 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-600";
      default:
        return "bg-gray-100 dark:bg-slate-600/30 text-gray-800 dark:text-slate-300 border-gray-200 dark:border-slate-600";
    }
  };

  const isUpcoming = (dateString: string) => new Date(dateString) > new Date();

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-64px)]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-64px)]">
        <div className="bg-red-600/20 border border-red-600 rounded-lg p-6 max-w-md">
          <p className="text-red-300 text-center">{error}</p>
          <button onClick={() => window.location.reload()} className="mt-4 w-full px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors">
            Retry
          </button>
        </div>
      </div>
    );
  }

  const upcomingAppointments = appointments.filter((apt) => isUpcoming(apt.appointmentTime) && apt.status === "BOOKED");
  const pastAppointments = appointments.filter((apt) => !isUpcoming(apt.appointmentTime) || apt.status !== "BOOKED").reverse();

  return (
    <div className="min-h-[calc(100vh-64px)] bg-gray-50 dark:bg-slate-900 py-10 transition-colors duration-200">
      <div className="max-w-6xl mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-slate-100">My Appointments</h1>
          <p className="text-gray-500 dark:text-slate-400 mt-2">Manage and view all your medical appointments</p>
        </div>

        {appointments.length === 0 ? (
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-lg border border-gray-200 dark:border-slate-600 p-12 text-center transition-colors duration-200">
            <Calendar className="h-16 w-16 text-gray-400 dark:text-slate-600 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 dark:text-slate-100 mb-2">No Appointments Yet</h3>
            <p className="text-gray-500 dark:text-slate-400 mb-6">You haven't booked any appointments. Start by finding a hospital near you.</p>
            <a href="/find-hospitals" className="inline-block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium">
              Find Hospitals
            </a>
          </div>
        ) : (
          <div className="space-y-8">
            {upcomingAppointments.length > 0 && (
              <div>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-slate-100 mb-4 flex items-center gap-2">
                  <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                  Upcoming Appointments ({upcomingAppointments.length})
                </h2>
                <div className="space-y-4">
                  {upcomingAppointments.map((appointment) => (
                    <div key={appointment.id} className="bg-white dark:bg-slate-800 rounded-xl shadow-md dark:shadow-lg border border-gray-200 dark:border-slate-600 p-6 hover:shadow-lg dark:hover:shadow-xl transition-shadow">
                      <div className="flex flex-col lg:flex-row gap-6">
                        <div className="flex-shrink-0">
                          <div className="bg-blue-50 dark:bg-blue-600/20 rounded-lg p-4 text-center border-2 border-blue-200 dark:border-blue-600">
                            <Calendar className="h-6 w-6 text-blue-600 dark:text-blue-400 mx-auto mb-2" />
                            <p className="text-sm font-semibold text-blue-800 dark:text-blue-300">{formatDate(appointment.appointmentTime)}</p>
                            <p className="text-lg font-bold text-blue-900 dark:text-blue-300 mt-1">{formatTime(appointment.appointmentTime)}</p>
                            <span className={`inline-block mt-2 px-3 py-1 rounded-full text-xs font-semibold border ${getStatusColor(appointment.status)}`}>
                              {appointment.status}
                            </span>
                          </div>
                        </div>

                        <div className="flex-1 space-y-4">
                          <div className="border-b border-gray-200 dark:border-slate-700 pb-4">
                            <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 mb-2">{appointment.clinicName}</h3>
                          </div>

                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <p className="text-xs text-gray-500 dark:text-slate-400 uppercase font-semibold mb-2">Doctor</p>
                              <div className="flex items-start gap-2">
                                <Stethoscope className="h-4 w-4 text-blue-600 dark:text-blue-400 mt-1" />
                                <div className="flex-1">
                                  <p className="font-semibold text-gray-900 dark:text-slate-100">
                                    {appointment.doctorName}
                                    {appointment.doctorSpecialization && <span className="text-gray-500 dark:text-slate-400 font-normal"> - {appointment.doctorSpecialization}</span>}
                                  </p>
                                  {appointment.reason && (
                                    <p className="text-xs text-gray-600 dark:text-slate-300 mt-2 bg-gray-100 dark:bg-slate-700 rounded px-2 py-1 border border-gray-200 dark:border-slate-600">{appointment.reason}</p>
                                  )}
                                </div>
                              </div>
                            </div>

                            <div>
                              <p className="text-xs text-gray-500 dark:text-slate-400 uppercase font-semibold mb-2">Patient</p>
                              <div className="space-y-1 text-sm">
                                <div className="flex items-center gap-2">
                                  <UserIcon className="h-4 w-4 text-gray-500" />
                                  <span className="text-gray-900 dark:text-slate-100">{appointment.patientName}, {appointment.patientAge}yrs, {appointment.patientGender}</span>
                                </div>
                                {appointment.patientPhone && (
                                  <div className="flex items-center gap-2">
                                    <Phone className="h-4 w-4 text-gray-500" />
                                    <span className="text-gray-600 dark:text-slate-400">{appointment.patientPhone}</span>
                                  </div>
                                )}
                                {appointment.patientEmail && (
                                  <div className="flex items-center gap-2">
                                    <Mail className="h-4 w-4 text-gray-500" />
                                    <span className="text-gray-600 dark:text-slate-400">{appointment.patientEmail}</span>
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="flex-shrink-0 flex items-start">
                          <button
                            onClick={() => handleCancelAppointment(appointment.id)}
                            disabled={cancellingId === appointment.id}
                            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-white dark:bg-slate-700 text-red-600 dark:text-red-300 border border-red-200 dark:border-red-600 hover:bg-red-50 dark:hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                          >
                            {cancellingId === appointment.id ? (
                              <>
                                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-red-600" />
                                <span>Cancelling...</span>
                              </>
                            ) : (
                              <>
                                <X className="h-4 w-4" />
                                <span>Cancel</span>
                              </>
                            )}
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {pastAppointments.length > 0 && (
              <div>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-slate-100 mb-4 flex items-center gap-2">
                  <Calendar className="h-5 w-5 text-gray-500" />
                  Past Appointments ({pastAppointments.length})
                </h2>
                <div className="space-y-4">
                  {pastAppointments.map((appointment) => (
                    <div key={appointment.id} className="bg-white dark:bg-slate-800 rounded-xl shadow-md dark:shadow-lg border border-gray-200 dark:border-slate-600 p-6 opacity-75 hover:opacity-100 transition-opacity duration-200">
                      <div className="flex flex-col lg:flex-row gap-6">
                        <div className="flex-shrink-0">
                          <div className="bg-gray-100 dark:bg-slate-700 rounded-lg p-4 text-center border-2 border-gray-200 dark:border-slate-600">
                            <Calendar className="h-6 w-6 text-gray-500 mx-auto mb-2" />
                            <p className="text-sm font-semibold text-gray-500 dark:text-slate-400">{formatDate(appointment.appointmentTime)}</p>
                            <p className="text-lg font-bold text-gray-600 dark:text-slate-400 mt-1">{formatTime(appointment.appointmentTime)}</p>
                            <span className={`inline-block mt-2 px-3 py-1 rounded-full text-xs font-semibold border ${getStatusColor(appointment.status)}`}>
                              {appointment.status}
                            </span>
                          </div>
                        </div>

                        <div className="flex-1 space-y-4">
                          <div className="border-b border-gray-200 dark:border-slate-700 pb-4">
                            <h3 className="text-lg font-bold text-gray-900 dark:text-slate-100 mb-2">{appointment.clinicName}</h3>
                          </div>

                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                              <p className="text-xs text-gray-500 dark:text-slate-400 uppercase font-semibold mb-2">Doctor</p>
                              <div className="flex items-start gap-2">
                                <Stethoscope className="h-4 w-4 text-gray-500 mt-1" />
                                <div className="flex-1">
                                  <p className="font-semibold text-gray-900 dark:text-slate-100">
                                    {appointment.doctorName}
                                    {appointment.doctorSpecialization && <span className="text-gray-500 dark:text-slate-400 font-normal"> - {appointment.doctorSpecialization}</span>}
                                  </p>
                                  {appointment.reason && (
                                    <p className="text-xs text-gray-600 dark:text-slate-400 mt-2 bg-gray-100 dark:bg-slate-700 rounded px-2 py-1 border border-gray-200 dark:border-slate-600">{appointment.reason}</p>
                                  )}
                                </div>
                              </div>
                            </div>

                            <div>
                              <p className="text-xs text-gray-500 dark:text-slate-400 uppercase font-semibold mb-2">Patient</p>
                              <div className="space-y-1 text-sm">
                                <div className="flex items-center gap-2">
                                  <UserIcon className="h-4 w-4 text-gray-500" />
                                  <span className="text-gray-900 dark:text-slate-100">{appointment.patientName}, {appointment.patientAge}yrs, {appointment.patientGender}</span>
                                </div>
                                {appointment.patientPhone && (
                                  <div className="flex items-center gap-2">
                                    <Phone className="h-4 w-4 text-gray-500" />
                                    <span className="text-gray-600 dark:text-slate-400">{appointment.patientPhone}</span>
                                  </div>
                                )}
                                {appointment.patientEmail && (
                                  <div className="flex items-center gap-2">
                                    <Mail className="h-4 w-4 text-gray-500" />
                                    <span className="text-gray-600 dark:text-slate-400">{appointment.patientEmail}</span>
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>

                        <div className="flex-shrink-0 flex items-start">
                          {/* Use a separate component or logic to check if reviewed. 
                      Since we are mapping, we can check easily if we have the list of reviews. */
                          }
                          {(() => {
                            const hasReviewed = userReviews.some(r => r.doctorId === Number(appointment.doctorId));
                            return (
                              <button
                                className={`px-4 py-2 rounded-lg text-white text-sm font-semibold transition-colors shadow-sm ${hasReviewed
                                  ? "bg-gray-400 cursor-not-allowed"
                                  : "bg-blue-600 hover:bg-blue-700"
                                  }`}
                                onClick={() => !hasReviewed && setReviewModal({ isOpen: true, appointment })}
                                disabled={hasReviewed}
                              >
                                {hasReviewed ? "Reviewed" : "Rate & Review"}
                              </button>
                            );
                          })()}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Review Modal */}
        <ReviewModal
          isOpen={reviewModal.isOpen}
          onClose={() => setReviewModal({ isOpen: false, appointment: null })}
          onSubmit={handleReviewSubmit}
          title={reviewModal.appointment?.clinicName || "Write a Review"}
          subtitle={`For ${reviewModal.appointment?.doctorName}`}
        />
      </div>
    </div>
  );
}
