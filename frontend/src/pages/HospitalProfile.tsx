import { useEffect, useState, useRef } from "react";
import { useParams } from "react-router-dom";
import defaultHospitalImage from "../assets/images/default-hospital.jpg";
import defaultDoctorImage from "../assets/images/default-doctor.jpeg";
import { apiRequest } from "../api";
import AppointmentBooking from "../components/AppointmentBooking";
import { useSelector } from "react-redux";
import type { RootState } from "../store/store";

interface Doctor {
  id: string | number;
  name: string;
  qualification: string;
  specialization: string;
  experience: string;
  biography: string;
  imageUrl?: string;
}

interface Hospital {
  id: string | number;
  clinicId?: string | number;
  name: string;
  address: string;
  city: string;
  phone?: string;
  specializations: string[];
  imageUrl?: string;
  doctors?: Doctor[];
  latitude?: number;
  longitude?: number;
}

interface Review {
  id: number;
  rating: number;
  comment: string;
  createdAt: string;
  userId: number;
  doctorId: number;
}

const HospitalProfile = () => {
  const { id } = useParams<{ id: string }>();
  // Use user from Redux if available, otherwise fallback to 1 for demo
  const { user } = useSelector((state: RootState) => state.auth);
  const currentUserId = user?.id || 1;

  const [hospital, setHospital] = useState<Hospital | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<{ [key: string]: string }>({});
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [selectedDoctorId, setSelectedDoctorId] = useState<string | number | null>(null);
  const tabRefs = useRef<{ [key: string]: HTMLDivElement | null }>({});
  const [selectedSpecialization, setSelectedSpecialization] = useState<string>("All");
  const [reviews, setReviews] = useState<Review[]>([]);

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        const data = await apiRequest<Review[]>(`/api/reviews/hospital/${id}`, "GET");
        setReviews(data);
      } catch (err) {
        console.error("Failed to fetch reviews", err);
      }
    };
    if (id) {
      fetchReviews();
    }
  }, [id]);

  const handleDeleteReview = async (reviewId: number) => {
    if (!window.confirm("Are you sure you want to delete this review?")) return;

    try {
      await apiRequest(`/api/reviews/${reviewId}`, "DELETE");
      setReviews(reviews.filter(r => r.id !== reviewId));
    } catch (err) {
      console.error("Failed to delete review", err);
      alert("Failed to delete review");
    }
  };

  // Get unique specializations from doctors
  const uniqueSpecializations = ["All", ...new Set(hospital?.doctors?.map(d => d.specialization || "General Practitioner") || [])];

  // Filter doctors based on selection
  const filteredDoctors = hospital?.doctors?.filter(doc =>
    selectedSpecialization === "All" || (doc.specialization || "General Practitioner") === selectedSpecialization
  );

  // Filter reviews based on selection
  const filteredReviews = reviews.filter(review => {
    if (selectedSpecialization === "All") return true;
    const doctor = hospital?.doctors?.find(d => Number(d.id) === review.doctorId);
    return (doctor?.specialization || "General Practitioner") === selectedSpecialization;
  });

  // Click outside handler to reset to services tab
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;

      // Check if click is outside any doctor's tab section
      const clickedInsideAnyTab = Object.values(tabRefs.current).some(ref =>
        ref && ref.contains(target)
      );

      if (!clickedInsideAnyTab) {
        // Reset all tabs to services
        setActiveTab({});
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    const fetchClinic = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiRequest<Hospital>(
          `/api/clinics/id?id=${id}`,
          "GET"
        );
        // API returns the clinic object directly, not wrapped in .data
        setHospital(response);
      } catch (err) {
        setError((err as Error)?.message || "Failed to load clinic details");
      } finally {
        setLoading(false);
      }
    };

    fetchClinic();
  }, [id]);

  if (loading) {
    return <p>Loading hospital details...</p>;
  }

  if (error) {
    return <p className="text-red-600">{error}</p>;
  }

  if (!hospital) {
    return <p>Hospital not found.</p>;
  }

  // Function to get the correct doctor image URL
  const getDoctorImageUrl = (imageUrl: string | undefined) => {
    if (!imageUrl) return defaultDoctorImage;

    // If it's already an absolute URL, return it as is
    if (imageUrl.startsWith('http')) {
      return imageUrl;
    }

    // If it's a relative path or invalid, use default
    return defaultDoctorImage;
  };

  // Function to get the correct hospital image URL
  const getHospitalImageUrl = (imageUrl: string | undefined) => {
    if (!imageUrl) return defaultHospitalImage;

    // If it's already an absolute URL, return it as is
    if (imageUrl.startsWith('http')) {
      return imageUrl;
    }

    // If it's a relative path or invalid, use default
    return defaultHospitalImage;
  };



  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-900 transition-colors duration-200">
      {/* Hero Section */}
      <div className="relative w-full h-auto min-h-[450px] lg:h-72 bg-gray-900 overflow-hidden flex items-center">
        <img src={getHospitalImageUrl(hospital.imageUrl)} alt={hospital.name} className="absolute inset-0 w-full h-full object-cover opacity-80" />
        <div className="absolute inset-0 bg-gradient-to-b from-gray-900/50 to-gray-900/90" />
        <div className="absolute top-4 right-4 z-10">
          <span className="px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-semibold border border-emerald-500/40 backdrop-blur-sm">Open 24/7</span>
        </div>
        {/* Hospital Info - Stacked on Mobile, Side by Side on Desktop */}
        <div className="relative z-10 w-full py-16 lg:py-0">
          <div className="max-w-7xl mx-auto px-4 w-full">
            <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
              <div className="flex-1 w-full lg:w-auto">
                <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-white mb-2 text-shadow-sm">{hospital.name}</h1>
                <p className="text-sm sm:text-base text-gray-200 mb-3 line-clamp-2">{hospital.specializations.join(", ")}</p>
                <div className="flex flex-wrap gap-2 mb-2">
                  <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-white/10 text-white text-xs border border-white/20 backdrop-blur-sm">
                    📍 {hospital.address}
                  </span>
                  <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-white/10 text-white text-xs border border-white/20 backdrop-blur-sm">
                    Multi-Specialty Hospital
                  </span>
                </div>
                {hospital.phone && (
                  <div className="flex items-center gap-2 mt-2">
                    <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-white/10 text-white text-xs border border-white/20 backdrop-blur-sm">
                      📞 {hospital.phone}
                    </span>
                  </div>
                )}
              </div>
              {/* Map Button - Bottom on Mobile, Right Side on Desktop */}
              {hospital.latitude && hospital.longitude && (
                <div className="relative text-right w-full lg:w-[300px] h-[200px] flex-shrink-0 rounded-xl overflow-hidden shadow-xl border-2 border-blue-500/50">
                  <div className="overflow-hidden bg-none w-full h-full">
                    <iframe
                      className="w-full h-full"
                      frameBorder="0"
                      marginHeight={0}
                      marginWidth={0}
                      src={`https://maps.google.com/maps?width=300&height=200&hl=en&q=${hospital.latitude},${hospital.longitude}&t=&z=15&ie=UTF8&iwloc=B&output=embed`}
                      title="Hospital Location"
                    ></iframe>
                    <a
                      href="https://sprunkiretake.net"
                      style={{
                        fontSize: "2px",
                        color: "gray",
                        position: "absolute",
                        bottom: 0,
                        left: 0,
                        zIndex: 1,
                        maxHeight: "1px",
                        overflow: "hidden"
                      }}
                    >
                      Sprunki
                    </a>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
      {/* Patient Reviews Section - Social Proof immediately after Hero */}
      <div className="max-w-7xl mx-auto px-4 py-10">
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white">What Patients Say</h2>
              <p className="text-sm text-gray-500 dark:text-slate-400">Real experiences from our patients</p>
            </div>
          </div>
        </div>

        {filteredReviews.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {filteredReviews.map((review, index) => {
              const reviewDoctor = hospital?.doctors?.find(d => Number(d.id) === review.doctorId);

              return (
                <div
                  key={review.id}
                  className="relative bg-white dark:bg-slate-800 p-6 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 hover:shadow-lg hover:border-blue-200 dark:hover:border-blue-800 transition-all duration-300 group"
                >
                  {/* Delete Button - Only show if current user owns the review */}
                  {(Number(review.userId) === Number(currentUserId) || currentUserId === 1) && (
                    <button
                      onClick={() => handleDeleteReview(review.id)}
                      className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-red-600 bg-red-50 hover:bg-red-100 dark:text-red-400 dark:bg-red-900/20 dark:hover:bg-red-900/40 rounded-lg transition-all z-10"
                      title="Delete Review"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M3 6h18"></path>
                        <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
                        <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
                      </svg>
                    </button>
                  )}

                  {/* Large Quote Icon */}
                  <div className="absolute -top-3 -left-2 text-blue-100 dark:text-slate-700">
                    <svg className="w-12 h-12" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M14.017 21v-7.391c0-5.704 3.731-9.57 8.983-10.609l.995 2.151c-2.432.917-3.995 3.638-3.995 5.849h4v10h-9.983zm-14.017 0v-7.391c0-5.704 3.748-9.57 9-10.609l.996 2.151c-2.433.917-3.996 3.638-3.996 5.849h3.983v10h-9.983z" />
                    </svg>
                  </div>

                  {/* Review Content */}
                  <div className="relative pt-4">
                    <p className="text-gray-700 dark:text-slate-300 text-base leading-relaxed mb-4 min-h-[80px]">
                      "{review.comment}"
                    </p>

                    {/* Reviewer Info */}
                    <div className="flex items-center gap-3 pt-4 border-t border-gray-100 dark:border-slate-700">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-emerald-400 to-teal-500 flex items-center justify-center text-white font-bold text-sm shadow-md">
                        {String.fromCharCode(65 + (index % 26))}
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-gray-900 dark:text-slate-100 text-sm">
                          Verified Patient
                        </p>
                        <p className="text-xs text-gray-500 dark:text-slate-400">
                          {new Date(review.createdAt).toLocaleDateString('en-US', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}
                        </p>
                      </div>
                    </div>

                    {/* Doctor Tag */}
                    {reviewDoctor && (
                      <div className="mt-3 inline-flex items-center gap-2 px-3 py-1.5 bg-blue-50 dark:bg-blue-900/20 rounded-full">
                        <svg className="w-4 h-4 text-blue-500 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                        </svg>
                        <span className="text-xs font-medium text-blue-700 dark:text-blue-300">
                          {reviewDoctor.name}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          /* Graceful Empty State */
          <div className="text-center py-16 bg-gradient-to-br from-gray-50 to-blue-50/30 dark:from-slate-800/50 dark:to-slate-800 rounded-2xl border border-dashed border-gray-200 dark:border-slate-700">
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <svg className="w-8 h-8 text-blue-500 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              No Reviews Yet
            </h3>
            <p className="text-gray-500 dark:text-slate-400 max-w-md mx-auto text-sm">
              Be the first to share your experience! Your feedback helps other patients make informed decisions about their healthcare.
            </p>
          </div>
        )}
      </div>

      {/* Doctors Section - Main Action Area */}
      <div className="bg-gradient-to-b from-white to-gray-50 dark:from-slate-900 dark:to-slate-800/50 py-12">
        <div className="max-w-7xl mx-auto px-4">
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-emerald-500 to-teal-500 flex items-center justify-center">
                <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Our Expert Doctors</h2>
                <p className="text-sm text-gray-500 dark:text-slate-400">Book an appointment with our specialists</p>
              </div>
            </div>

            {/* Specialization Filters */}
            <div className="flex flex-wrap gap-2 mt-6">
              {uniqueSpecializations.map((spec) => (
                <button
                  key={spec}
                  onClick={() => setSelectedSpecialization(spec)}
                  className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 ${selectedSpecialization === spec
                    ? "bg-gradient-to-r from-blue-600 to-blue-500 text-white shadow-lg shadow-blue-500/30 scale-105"
                    : "bg-white dark:bg-slate-800 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-slate-700 border border-gray-200 dark:border-slate-700"
                    }`}
                >
                  {spec}
                </button>
              ))}
            </div>
          </div>

          {/* Doctors List */}
          {filteredDoctors && filteredDoctors.length > 0 ? (
            <div className="space-y-4">
              {filteredDoctors.map((doctor) => (
                <div
                  key={doctor.id}
                  ref={(el) => { tabRefs.current[doctor.id] = el; }}
                  className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-gray-100 dark:border-slate-700 p-4 sm:p-6 hover:shadow-xl hover:border-blue-200 dark:hover:border-blue-800 transition-all duration-300"
                >
                  <div className="flex flex-col lg:flex-row gap-4 lg:gap-6">
                    {/* Left Section - Doctor Image & Info */}
                    <div className="flex gap-4 items-start">
                      <div className="flex-shrink-0 relative">
                        <img
                          src={getDoctorImageUrl(doctor.imageUrl)}
                          alt={doctor.name}
                          className="w-20 h-20 sm:w-24 sm:h-24 object-cover rounded-xl shadow-md"
                        />
                        <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-emerald-500 rounded-full flex items-center justify-center border-2 border-white dark:border-slate-800">
                          <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                          </svg>
                        </div>
                      </div>
                      <div className="flex-1">
                        <h3 className="text-lg font-bold text-gray-900 dark:text-white">{doctor.name}</h3>
                        <p className="text-sm text-blue-600 dark:text-blue-400 font-medium mb-1">
                          {doctor.specialization || "General Practitioner"}
                        </p>
                        <p className="text-xs text-gray-500 dark:text-slate-400 mb-3">
                          15 years exp • {doctor.qualification || "MD"}
                        </p>
                        <div className="flex items-center gap-2">
                          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-900/20 px-2.5 py-1 rounded-full">
                            <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
                            Available Today
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Middle Section - Services & Timings Tabs */}
                    <div className="flex-1 lg:border-l border-gray-200 dark:border-slate-700 lg:pl-6">
                      <div className="flex gap-4 mb-4">
                        <button
                          onClick={() => setActiveTab({ ...activeTab, [doctor.id]: 'services' })}
                          className={`pb-2 text-sm font-medium transition-colors ${activeTab[doctor.id] !== 'timings'
                            ? 'text-blue-600 dark:text-blue-400 border-b-2 border-blue-600 dark:border-blue-400'
                            : 'text-gray-500 dark:text-slate-400 hover:text-gray-700 dark:hover:text-slate-300'
                            }`}
                        >
                          Services
                        </button>
                        <button
                          onClick={() => setActiveTab({ ...activeTab, [doctor.id]: 'timings' })}
                          className={`pb-2 text-sm font-medium transition-colors ${activeTab[doctor.id] === 'timings'
                            ? 'text-blue-600 dark:text-blue-400 border-b-2 border-blue-600 dark:border-blue-400'
                            : 'text-gray-500 dark:text-slate-400 hover:text-gray-700 dark:hover:text-slate-300'
                            }`}
                        >
                          Timings
                        </button>
                      </div>
                      <div className="space-y-2">
                        {activeTab[doctor.id] === 'timings' ? (
                          <div className="space-y-3">
                            <div className="bg-blue-50 dark:bg-blue-900/20 border-l-4 border-blue-400 rounded-lg p-4">
                              <p className="text-xs font-semibold text-blue-600 dark:text-blue-300 uppercase mb-2">Weekdays & Saturday</p>
                              <div className="flex flex-wrap gap-2">
                                <span className="bg-blue-600 text-white text-xs font-bold px-3 py-1.5 rounded-full">
                                  MON–SAT
                                </span>
                                <div className="flex items-center gap-2 flex-wrap">
                                  <span className="text-sm font-semibold text-gray-700 dark:text-slate-200">09:00 AM – 01:00 PM</span>
                                  <span className="text-gray-400 dark:text-slate-500">&</span>
                                  <span className="text-sm font-semibold text-gray-700 dark:text-slate-200">02:00 PM – 08:00 PM</span>
                                </div>
                              </div>
                            </div>
                            <div className="bg-orange-50 dark:bg-orange-900/20 border-l-4 border-orange-400 rounded-lg p-4">
                              <p className="text-xs font-semibold text-orange-600 dark:text-orange-300 uppercase mb-2">Sunday</p>
                              <div className="flex flex-wrap gap-2">
                                <span className="bg-orange-500 text-white text-xs font-bold px-3 py-1.5 rounded-full">
                                  SUN
                                </span>
                                <div className="flex items-center gap-2 flex-wrap">
                                  <span className="text-sm font-semibold text-gray-700 dark:text-slate-200">09:00 AM – 01:00 PM</span>
                                  <span className="text-gray-400 dark:text-slate-500">&</span>
                                  <span className="text-sm font-semibold text-gray-700 dark:text-slate-200">02:00 PM – 06:00 PM</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div>
                            <p className="text-base font-bold text-gray-900 dark:text-slate-100">
                              {doctor.specialization || 'General Practitioner'}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-slate-300 leading-relaxed mt-1">
                              {doctor.biography || 'Providing comprehensive healthcare services with personalized attention to every patient.'}
                            </p>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Right Section - Book Button */}
                    <div className="flex flex-col gap-3 items-stretch lg:items-end justify-center flex-shrink-0 lg:border-l border-gray-200 dark:border-slate-700 lg:pl-6 pt-4 lg:pt-0">
                      <button
                        onClick={() => {
                          setActiveTab({});
                          setSelectedDoctorId(doctor.id);
                          setShowBookingModal(true);
                        }}
                        className="px-8 py-3 rounded-xl bg-gradient-to-r from-blue-600 to-blue-500 hover:from-blue-700 hover:to-blue-600 text-white font-semibold text-sm shadow-lg shadow-blue-500/30 hover:shadow-xl hover:shadow-blue-500/40 transition-all duration-300 transform hover:scale-105"
                      >
                        Book Appointment
                      </button>
                      <p className="text-xs text-gray-500 dark:text-slate-400 text-center lg:text-right">
                        Next available: Today
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-2xl p-12 text-center">
              <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gray-100 dark:bg-slate-700 flex items-center justify-center">
                <svg className="w-8 h-8 text-gray-400 dark:text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <p className="text-gray-500 dark:text-slate-400 font-medium">No doctors available for this specialization</p>
              <p className="text-sm text-gray-400 dark:text-slate-500 mt-1">Try selecting a different specialty filter</p>
            </div>
          )}
        </div>
      </div>

      {/* Appointment Booking Modal */}
      {showBookingModal && selectedDoctorId && (
        <AppointmentBooking
          hospitalId={id || ''}
          doctorId={selectedDoctorId}
          doctorName={hospital?.doctors?.find(d => d.id === selectedDoctorId)?.name}
          specialization={hospital?.doctors?.find(d => d.id === selectedDoctorId)?.specialization}
          onClose={() => {
            setShowBookingModal(false);
            setSelectedDoctorId(null);
          }}
        />
      )}
    </div>
  );
}

export default HospitalProfile;