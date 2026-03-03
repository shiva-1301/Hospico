import { useState, useEffect, useCallback } from "react";
import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { apiRequest } from "../api";

interface UserProfile {
  name?: string;
  email?: string;
  phone?: string;
  age?: number;
  gender?: string;
}

interface Doctor {
  id: string | number;
  name: string;
  specialization: string;
  qualifications: string;
}

interface TimeSlot {
  time: string;
  isBooked: boolean;
}

interface AppointmentResponse {
  id: string;
  userId: string;
  appointmentTime: string;
  patientName: string;
  patientAge: number;
  status: string;
}

interface AppointmentBookingProps {
  hospitalId: string | number;
  doctorId?: string | number;
  doctorName?: string;
  specialization?: string;
  onClose: () => void;
}

const AppointmentBooking = ({ hospitalId, doctorId, doctorName, specialization, onClose }: AppointmentBookingProps) => {
  const { user } = useSelector((state: RootState) => state.auth);

  const [selectedDoctor, setSelectedDoctor] = useState<string | number>("");
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [availableSlots, setAvailableSlots] = useState<TimeSlot[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<string>("");
  const [problem, setProblem] = useState<string>("");
  const [patientName, setPatientName] = useState<string>("");
  const [patientAge, setPatientAge] = useState<string>("");
  const [patientGender, setPatientGender] = useState<string>("");
  const [patientPhone, setPatientPhone] = useState<string>("");
  const [patientEmail, setPatientEmail] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const [bookingLoading, setBookingLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selfBooking, setSelfBooking] = useState<boolean>(true);
  const [cachedProfile, setCachedProfile] = useState<UserProfile | null>(null);

  // Apply cached profile (or fallback user) to patient fields
  const applyProfileToFields = useCallback(() => {
    const source: UserProfile | null = cachedProfile || (user
      ? {
        name: user.name,
        email: user.email,
        phone: user.phone,
        age: user.age,
        gender: user.gender,
      }
      : null);

    if (!source) return;

    setPatientName(source.name || source.email?.split("@")[0] || "");
    setPatientEmail(source.email || "");
    setPatientPhone(source.phone || "");
    setPatientAge(source.age ? source.age.toString() : "");
    setPatientGender(source.gender || "");
  }, [cachedProfile, user]);

  // Fetch profile to auto-populate patient details
  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;
      try {
        const profile = await apiRequest<UserProfile>("/api/users/me", "GET");
        setCachedProfile(profile);
      } catch (err) {
        console.error("Failed to fetch profile", err);
      }
    };

    fetchProfile();
  }, [user]);

  // Load doctors for the selected hospital
  useEffect(() => {
    const loadDoctors = async () => {
      setLoading(true);
      setError(null);
      try {
        await apiRequest<Doctor[]>(
          `/api/clinics/${hospitalId}/doctors`,
          "GET"
        );

        // Pre-fill doctor if provided from props
        if (doctorId) {
          setSelectedDoctor(String(doctorId));
        }

      } catch (err) {
        setError("Failed to load doctors for this hospital");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadDoctors();
  }, [hospitalId, doctorId, user]);

  // Auto-populate when self booking is enabled or when profile changes
  useEffect(() => {
    if (!selfBooking) return;
    applyProfileToFields();
  }, [selfBooking, cachedProfile, user, applyProfileToFields]);

  // Generate time slots when date or doctor changes
  useEffect(() => {
    if (!selectedDoctor || !selectedDate) {
      setAvailableSlots([]);
      return;
    }

    const generateSlots = async () => {
      // Check if selected date is today
      const today = new Date();
      const selectedDateObj = new Date(selectedDate);
      const isToday = selectedDateObj.toDateString() === today.toDateString();

      // Get current time
      const currentHour = today.getHours();
      const currentMinute = today.getMinutes();

      // Check if selected date is Sunday
      const selectedDay = new Date(selectedDate).getDay();
      const isSunday = selectedDay === 0;

      // Fetch booked appointments for this doctor on this date
      let bookedTimes: string[] = [];
      try {
        console.log(`🔍 Fetching booked appointments for doctor ${selectedDoctor} on date ${selectedDate}`);
        // Use apiRequest helper instead of raw fetch
        const appointments = await apiRequest<AppointmentResponse[]>(
          `/api/appointments/doctor/${selectedDoctor}/date/${selectedDate}`,
          "GET"
        );

        console.log("📦 Raw appointments data:", appointments);

        if (Array.isArray(appointments)) {
          console.log(`✅ Found ${appointments.length} appointment(s)`);

          bookedTimes = appointments
            .filter(apt => apt.status === "BOOKED")
            .map((apt) => {
              const time = apt.appointmentTime?.substring(11, 16);
              console.log(`  ✅ Extracted booked time: ${time}`);
              return time;
            })
            .filter((time): time is string => time !== undefined && time.length === 5);

          console.log("✅ Final booked times array:", bookedTimes);
        }
      } catch (err) {
        console.error("❌ Failed to fetch booked appointments:", err);
      }

      // Generate time slots based on doctor's timings
      const slots: TimeSlot[] = [];

      // Morning session: 9:00 AM - 1:00 PM
      for (let hour = 9; hour <= 13; hour++) {
        for (let minute = 0; minute < 60; minute += 30) {
          if (hour === 13 && minute > 0) break; // Stop at 1:00 PM

          // Skip past times if today is selected
          if (isToday && (hour < currentHour || (hour === currentHour && minute <= currentMinute))) {
            continue;
          }

          const timeString = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
          const isBooked = bookedTimes.includes(timeString);

          slots.push({
            time: timeString,
            isBooked
          });

          if (isBooked) {
            console.log("Marked as BOOKED:", timeString);
          }
        }
      }

      // Afternoon session: 2:00 PM - 8:00 PM (weekdays) or 2:00 PM - 6:00 PM (Sunday)
      const afternoonEndHour = isSunday ? 18 : 20;
      for (let hour = 14; hour <= afternoonEndHour; hour++) {
        for (let minute = 0; minute < 60; minute += 30) {
          if (hour === afternoonEndHour && minute > 0) break;

          // Skip past times if today is selected
          if (isToday && (hour < currentHour || (hour === currentHour && minute <= currentMinute))) {
            continue;
          }

          const timeString = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
          const isBooked = bookedTimes.includes(timeString);

          slots.push({
            time: timeString,
            isBooked
          });

          if (isBooked) {
            console.log("Marked as BOOKED:", timeString);
          }
        }
      }

      console.log("All generated slots:", slots);
      setAvailableSlots(slots);
      setSelectedSlot("");
    };

    generateSlots();
  }, [selectedDoctor, selectedDate]);

  const handleBookAppointment = async () => {
    if (!selectedDoctor || !selectedDate || !selectedSlot || !patientName || !patientAge || !patientGender || !problem) {
      setError("Please fill in all required fields");
      return;
    }

    // Check if selected slot is actually available (not booked)
    const selectedSlotObj = availableSlots.find(slot => slot.time === selectedSlot);
    console.log("Selected slot object:", selectedSlotObj);
    console.log("Is booked:", selectedSlotObj?.isBooked);

    if (selectedSlotObj?.isBooked) {
      setError("❌ This slot is already booked and cannot be selected. Please choose a different time.");
      setSelectedSlot("");
      console.warn("Attempted to book already booked slot:", selectedSlot);
      return;
    }

    setBookingLoading(true);
    setError(null);
    setSuccess(null);

    try {
      // Combine date and time
      const appointmentDateTime = `${selectedDate}T${selectedSlot}:00`;

      const appointmentData = {
        userId: user?.id ? String(user.id) : null,
        clinicId: String(hospitalId),
        doctorId: String(selectedDoctor),
        appointmentTime: appointmentDateTime,
        patientName,
        patientAge: parseInt(patientAge),
        patientGender,
        patientPhone,
        patientEmail,
        reason: problem  // Adding the problem/reason field
      };

      const response = await apiRequest<unknown>(
        "/api/appointments",
        "POST",
        appointmentData
      );

      if (response) {
        // Show success message
        setSuccess("Appointment booked successfully! We will confirm details shortly.");
        // Close modal after delay
        setTimeout(() => {
          onClose();
        }, 2000);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to book appointment";
      setError(message);
      console.error(err);
    } finally {
      setBookingLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-colors duration-200">
      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-slate-700 transition-colors duration-200">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Book Appointment</h2>
            <button
              onClick={onClose}
              className="text-gray-500 hover:text-gray-700 dark:text-slate-400 dark:hover:text-slate-200 transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {error && (
            <div className="mb-6 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 transition-colors">
              <p className="text-red-800 dark:text-red-300">{error}</p>
            </div>
          )}

          {success && (
            <div className="mb-6 bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 rounded-lg p-4 transition-colors">
              <p className="text-green-800 dark:text-green-300">{success}</p>
            </div>
          )}

          {loading ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 dark:border-blue-400"></div>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Doctor Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                  Selected Doctor *
                </label>
                {doctorName && specialization && (
                  <div className="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-md border border-blue-200 dark:border-blue-800 transition-colors">
                    <p className="text-sm font-medium text-blue-900 dark:text-blue-200">
                      {doctorName} - {specialization}
                    </p>
                  </div>
                )}
              </div>

              {/* Problem/Reason */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                  Problem/Reason *
                </label>
                <textarea
                  value={problem}
                  onChange={(e) => setProblem(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:focus:border-blue-500 transition-colors"
                  placeholder="Describe your problem or reason for visit"
                />
              </div>

              {/* Date Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                  Select Date *
                </label>
                <input
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white focus:outline-none focus:ring-blue-500 focus:border-blue-500 dark:focus:border-blue-500 transition-colors"
                />
              </div>

              {/* Time Slot Selection */}
              {selectedDate && selectedDoctor && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                    Select Time Slot *
                  </label>
                  <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
                    {availableSlots.length > 0 ? (
                      availableSlots.map((slot) =>
                        slot.isBooked ? (
                          <div
                            key={slot.time}
                            className="w-full px-3 py-2 text-sm font-medium rounded-md border-2 border-gray-300 dark:border-slate-600 bg-gray-300 dark:bg-slate-700 text-gray-600 dark:text-slate-400 cursor-not-allowed pointer-events-none transition-colors"
                            title="This slot is already booked"
                          >
                            {slot.time}
                          </div>
                        ) : (
                          <button
                            key={slot.time}
                            type="button"
                            onClick={() => setSelectedSlot(slot.time)}
                            className={`w-full px-3 py-2 text-sm font-medium rounded-md border-2 transition-all ${selectedSlot === slot.time
                              ? "border-green-600 dark:border-green-500 bg-green-50 dark:bg-green-900/30 text-green-900 dark:text-green-400 shadow-lg"
                              : "border-blue-400 dark:border-blue-600 bg-blue-50 dark:bg-slate-700 text-blue-900 dark:text-blue-200 hover:bg-blue-100 dark:hover:bg-slate-600 cursor-pointer"
                              }`}
                          >
                            {slot.time}
                          </button>
                        )
                      )
                    ) : (
                      <p className="text-gray-500 dark:text-slate-400 text-sm col-span-full">No available slots for this date</p>
                    )}
                  </div>
                </div>
              )}

              {/* Patient Details */}
              <div className="border-t border-gray-200 dark:border-slate-700 pt-6">
                <div className="flex items-center gap-3 mb-4">
                  <input
                    type="checkbox"
                    id="selfCheckbox"
                    checked={selfBooking}
                    onChange={(e) => {
                      const checked = e.target.checked;
                      setSelfBooking(checked);

                      if (!checked) {
                        // Clear all fields when user opts out
                        setPatientName("");
                        setPatientPhone("");
                        setPatientAge("");
                        setPatientGender("");
                        setPatientEmail("");
                      } else {
                        // Re-apply cached profile when opting back in
                        applyProfileToFields();
                      }
                    }}
                    className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500 dark:bg-slate-700 dark:border-slate-600 cursor-pointer"
                  />
                  <label htmlFor="selfCheckbox" className="text-sm font-medium text-gray-700 dark:text-slate-300 cursor-pointer">
                    Booking for myself
                  </label>
                </div>

                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">Patient Details</h3>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                      Full Name *
                    </label>
                    <input
                      type="text"
                      value={patientName}
                      onChange={(e) => setPatientName(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors"
                      placeholder="Enter full name"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                      Age *
                    </label>
                    <input
                      type="number"
                      value={patientAge}
                      onChange={(e) => setPatientAge(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors"
                      placeholder="Enter age"
                      min="1"
                      max="120"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                      Gender *
                    </label>
                    <select
                      value={patientGender}
                      onChange={(e) => setPatientGender(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors"
                    >
                      <option value="">Select gender</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                      Phone Number
                    </label>
                    <input
                      type="tel"
                      value={patientPhone}
                      onChange={(e) => setPatientPhone(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors"
                      placeholder="Enter phone number"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-slate-300 mb-2">
                      Email Address
                    </label>
                    <input
                      type="email"
                      value={patientEmail}
                      onChange={(e) => setPatientEmail(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm bg-white dark:bg-slate-900 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-slate-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors"
                      placeholder="Enter email address"
                    />
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200 dark:border-slate-700">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 border border-gray-300 dark:border-slate-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleBookAppointment}
                  disabled={bookingLoading || !selectedDoctor || !selectedDate || !selectedSlot || !patientName || !patientAge || !patientGender || !problem}
                  className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {bookingLoading ? "Booking..." : "Book Appointment"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AppointmentBooking;
