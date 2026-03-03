import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Edit, File, FileText, Image as ImageIcon, Mail, Phone, Save, User, X } from "lucide-react";
import { useAppDispatch, type RootState } from "../store/store";
import { fetchUserRecords } from "../features/medicalRecords/medicalRecordsSlice";
import { setUser } from "../features/auth/authSlice";
import { apiRequest } from "../api";

type UserProfile = {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  age?: number;
  gender?: string;
};

type UpdateProfilePayload = {
  name?: string;
  phone?: string;
  password?: string;
  age?: number;
  gender?: string;
};

type Appointment = {
  id: number;
  appointmentTime: string;
  status: string;
  clinicName: string;
  doctorName: string;
  reason?: string;
};

export default function Profile() {
  const { user: authUser, isAuthenticated } = useSelector((state: RootState) => state.auth);
  const { files: healthRecords } = useSelector((state: RootState) => state.medicalRecords);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previousVisits, setPreviousVisits] = useState<Appointment[]>([]);

  const [editData, setEditData] = useState({
    name: "",
    phone: "",
    age: "",
    gender: "",
    newPassword: "",
    confirmPassword: ""
  });

  useEffect(() => {
    fetchCurrentUserProfile();
    fetchRecentAppointments();
    if (authUser?.id) {
      dispatch(fetchUserRecords(Number(authUser.id)));
    }
  }, [authUser, isAuthenticated, dispatch]);

  const fetchCurrentUserProfile = async () => {
    try {
      setLoading(true);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const authUserData = await apiRequest<any>("/api/users/me", "GET");

      if (authUserData && authUserData.id) {
        const userProfile: UserProfile = {
          id: authUserData.id,
          name: authUserData.name || "",
          email: authUserData.email || "",
          phone: authUserData.phone || "",
          role: authUserData.role || "",
          age: authUserData.age ?? undefined,
          gender: authUserData.gender ?? undefined
        };

        setProfile(userProfile);
        setEditData({
          name: userProfile.name || "",
          phone: userProfile.phone || "",
          age: userProfile.age ? String(userProfile.age) : "",
          gender: userProfile.gender || "",
          newPassword: "",
          confirmPassword: ""
        });
      } else {
        setError("Unable to fetch user profile");
      }
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      let errorMessage = "Failed to fetch profile data. Please try logging in again.";
      if (err.response) {
        if (err.response.status === 401) {
          errorMessage = "Session expired: Please log in again";
        } else if (err.response.status === 403) {
          errorMessage = "Access denied: Insufficient permissions";
        }
      }
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const fetchRecentAppointments = async () => {
    try {
      if (authUser?.id) {
        const appointments = await apiRequest<Appointment[]>(`/api/appointments/user/${authUser.id}`, "GET");

        // Sort by date (most recent first) and take only 2
        const sortedAppointments = appointments
          .sort((a, b) => new Date(b.appointmentTime).getTime() - new Date(a.appointmentTime).getTime())
          .slice(0, 2);

        setPreviousVisits(sortedAppointments);
      }
    } catch (err) {
      console.error("Failed to fetch appointments:", err);
    }
  };


  const handleSave = async () => {
    if (editData.newPassword !== editData.confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const updateData: UpdateProfilePayload = {};
      if (editData.name !== profile?.name) updateData.name = editData.name;
      if (editData.phone !== profile?.phone) updateData.phone = editData.phone;
      if (editData.age !== (profile?.age ? String(profile.age) : "") && editData.age !== "") {
        updateData.age = Number(editData.age);
      }
      if (editData.gender !== (profile?.gender || "")) {
        updateData.gender = editData.gender || undefined;
      }
      if (editData.newPassword) updateData.password = editData.newPassword;

      const updatedProfile = await apiRequest<UserProfile>("/api/users/me", "PATCH", updateData);

      setProfile(updatedProfile);
      // Keep Redux auth state in sync so ChatWidget etc. see updated profile
      dispatch(setUser({
        name: updatedProfile.name || undefined,
        phone: updatedProfile.phone || undefined,
        age: updatedProfile.age ?? undefined,
        gender: updatedProfile.gender || undefined,
      }));
      setIsEditing(false);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      let errorMessage = "Failed to update profile";
      if (err.response) {
        if (err.response.status === 400) {
          errorMessage = "Invalid input data";
        } else if (err.response.status === 401) {
          errorMessage = "Session expired: Please log in again";
        } else if (err.response.status === 403) {
          errorMessage = "Access denied: Cannot update this profile";
        } else if (err.response.status === 404) {
          errorMessage = "User not found";
        }
      }
      setError(errorMessage);
    } finally {
      setSaving(false);
    }
  };

  const getRecordIcon = (name?: string) => {
    const lower = (name || "").toLowerCase();
    if (lower.endsWith(".pdf")) {
      return <FileText size={20} className="text-red-500 dark:text-red-400" />;
    }
    if (lower.match(/\.(png|jpg|jpeg|gif|webp|bmp|svg)$/)) {
      return <ImageIcon size={20} className="text-blue-500 dark:text-blue-400" />;
    }
    return <File size={20} className="text-blue-500 dark:text-blue-400" />;
  };

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
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md">
          <p className="text-red-800 text-center">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 w-full px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-64px)]">
        <div className="text-center">
          <p className="text-gray-600">No profile data available</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          >
            Reload
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-gray-100 dark:bg-slate-950 text-gray-900 dark:text-white transition-colors duration-200 min-h-[calc(100vh-64px)] flex items-center justify-center p-4 lg:p-8">
      <div className="w-full max-w-6xl bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-gray-200 dark:border-slate-800 p-6 lg:p-8 space-y-6">
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-xl bg-gray-100 dark:bg-slate-700 flex items-center justify-center text-gray-500 dark:text-gray-300 border border-gray-200 dark:border-slate-600">
              <User size={28} />
            </div>
            <div className="min-w-0 notranslate">
              {isEditing ? (
                <input
                  type="text"
                  value={editData.name || ""}
                  onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                  className="w-full max-w-[260px] text-2xl font-bold text-gray-900 dark:text-white rounded-md px-2 py-1 border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none"
                />
              ) : (
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white truncate">{profile.name}</h1>
              )}
              <div className="flex flex-wrap items-center gap-3 mt-1 text-sm text-gray-500 dark:text-slate-300">
                <div className="flex items-center gap-1">
                  <Mail size={16} />
                  <span className="truncate">{profile.email}</span>
                </div>
                <div className="flex items-center gap-1">
                  <Phone size={16} />
                  {isEditing ? (
                    <input
                      type="text"
                      value={editData.phone || ""}
                      onChange={(e) => setEditData({ ...editData, phone: e.target.value })}
                      className="border-b border-gray-300 dark:border-slate-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none bg-transparent pb-0.5 text-gray-900 dark:text-white"
                    />
                  ) : (
                    <span>{profile.phone || "No phone"}</span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {!isEditing ? (
            <button
              onClick={() => setIsEditing(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg flex items-center gap-2 text-sm font-medium transition-colors shadow-lg shadow-blue-500/20"
            >
              <Edit size={18} />
              Edit Profile
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                onClick={handleSave}
                disabled={saving}
                className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg flex items-center gap-2 text-sm font-medium transition-colors shadow-lg shadow-blue-500/20 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {saving ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save size={18} />
                    Save
                  </>
                )}
              </button>
              <button
                onClick={() => {
                  setIsEditing(false);
                  setEditData({
                    name: profile.name || "",
                    phone: profile.phone || "",
                    age: profile.age ? String(profile.age) : "",
                    gender: profile.gender || "",
                    newPassword: "",
                    confirmPassword: ""
                  });
                  setError(null);
                }}
                className="bg-gray-200 dark:bg-slate-700 text-gray-700 dark:text-slate-200 px-5 py-2.5 rounded-lg flex items-center gap-2 text-sm font-medium border border-gray-300 dark:border-slate-600 hover:bg-gray-300 dark:hover:bg-slate-600 transition-colors"
              >
                <X size={18} />
                Cancel
              </button>
            </div>
          )}
        </header>

        {error && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="border border-gray-200 dark:border-slate-700 rounded-xl p-6 flex flex-col h-full bg-gray-50/50 dark:bg-transparent">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-300">Contact & Identity</h2>
              <span className="px-3 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs font-semibold rounded-full border border-blue-200 dark:border-blue-800">
                {(profile.role || "User").toLowerCase()}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-y-6 gap-x-4 notranslate">
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Full Name</p>
                {isEditing ? (
                  <input
                    type="text"
                    value={editData.name || ""}
                    onChange={(e) => setEditData({ ...editData, name: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  />
                ) : (
                  <p className="font-medium text-gray-900 dark:text-white">{profile.name}</p>
                )}
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Email</p>
                <p className="font-medium text-gray-900 dark:text-white truncate">{profile.email}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Phone</p>
                {isEditing ? (
                  <input
                    type="text"
                    value={editData.phone || ""}
                    onChange={(e) => setEditData({ ...editData, phone: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  />
                ) : (
                  <p className="font-medium text-gray-900 dark:text-white">{profile.phone || "No phone"}</p>
                )}
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Age</p>
                {isEditing ? (
                  <input
                    type="number"
                    min={0}
                    value={editData.age || ""}
                    onChange={(e) => setEditData({ ...editData, age: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  />
                ) : (
                  <p className="font-medium text-gray-900 dark:text-white">{profile.age ?? "Not set"}</p>
                )}
              </div>
              <div className="col-span-2">
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Gender</p>
                {isEditing ? (
                  <select
                    value={editData.gender || ""}
                    onChange={(e) => setEditData({ ...editData, gender: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  >
                    <option value="">Select</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                ) : (
                  <p className="font-medium text-gray-900 dark:text-white">{profile.gender || "Not set"}</p>
                )}
              </div>
            </div>
          </div>

          <div className="border border-gray-200 dark:border-slate-700 rounded-xl p-6 flex flex-col h-full bg-gray-50/50 dark:bg-transparent">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-300">Previous Visits</h2>
              <span className="text-xs text-gray-500 dark:text-slate-400">Recent 2</span>
            </div>
            <div className="space-y-3">
              {previousVisits.length > 0 ? (
                previousVisits.map((visit) => (
                  <div key={visit.id} className="bg-white dark:bg-slate-800/80 p-4 rounded-lg border border-gray-200 dark:border-slate-700/50 shadow-sm relative">
                    <div className="absolute top-4 right-4 text-xs text-gray-500 dark:text-gray-400 font-mono text-right leading-tight">
                      {new Date(visit.appointmentTime).toLocaleDateString("en-US", {
                        month: "short",
                        day: "numeric",
                        year: "numeric"
                      })}
                    </div>
                    <h3 className="font-semibold text-gray-900 dark:text-white pr-12 text-sm">{visit.clinicName}</h3>
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{visit.doctorName}</p>
                    {visit.reason && (
                      <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">{visit.reason}</p>
                    )}
                    <div className="mt-3">
                      <span className={`inline-block px-2 py-0.5 text-[10px] font-bold rounded border tracking-wide ${visit.status === "BOOKED"
                        ? "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 border-green-200 dark:border-green-800"
                        : visit.status === "CANCELLED"
                          ? "bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 border-red-200 dark:border-red-800"
                          : "bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 border-blue-200 dark:border-blue-800"
                        }`}>
                        {visit.status}
                      </span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-6 text-gray-500 dark:text-slate-400">
                  <p className="text-sm">No appointments yet</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {isEditing && (
          <div className="border border-gray-200 dark:border-slate-700 rounded-xl p-6 bg-gray-50/50 dark:bg-transparent">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-300">Security</h2>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">New Password</p>
                <input
                  type="password"
                  id="newPassword"
                  value={editData.newPassword || ""}
                  onChange={(e) => setEditData({ ...editData, newPassword: e.target.value })}
                  className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  placeholder="Enter new password"
                />
              </div>
              <div>
                <p className="text-xs text-gray-500 dark:text-slate-400 mb-1">Confirm Password</p>
                <input
                  type="password"
                  id="confirmPassword"
                  value={editData.confirmPassword || ""}
                  onChange={(e) => setEditData({ ...editData, confirmPassword: e.target.value })}
                  className="w-full rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none"
                  placeholder="Confirm new password"
                />
              </div>
            </div>
          </div>
        )}

        <div className="border border-gray-200 dark:border-slate-700 rounded-xl p-6 bg-gray-50/50 dark:bg-transparent">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-300">Health Records</h2>
            <button
              onClick={() => navigate('/reports')}
              className="text-blue-600 hover:text-blue-700 text-sm font-semibold transition-colors"
            >
              Upload
            </button>
          </div>
          <div className="space-y-3">
            {healthRecords.length > 0 ? (
              healthRecords.slice(0, 5).map((record) => (
                <div key={record.id} className="flex flex-col sm:flex-row sm:items-center justify-between bg-white dark:bg-slate-800/80 p-4 rounded-lg border border-gray-200 dark:border-slate-700/50 hover:bg-gray-50 dark:hover:bg-slate-800/50 transition-colors">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="p-2 bg-blue-50 dark:bg-slate-700 rounded">
                      {getRecordIcon(record.name)}
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-200 truncate" title={record.name}>{record.name}</p>
                      <p className="text-[10px] text-gray-500 dark:text-gray-400">{record.date}</p>
                    </div>
                  </div>
                  <div className="mt-2 sm:mt-0">
                    <span className="inline-block px-3 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 text-xs rounded-full border border-blue-100 dark:border-blue-900/50">
                      {record.category}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-4 text-gray-500 dark:text-slate-400 text-sm">
                No health records found.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}