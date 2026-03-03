import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { Geolocation } from '@capacitor/geolocation';
import { Capacitor } from '@capacitor/core';
import HospitalSearch from "../components/HospitalSearch";
import ProtectedRoute from "../components/ProtectedRoute";
import { apiRequest } from "../api";

import NearbyHospitals from "../components/NearbyHospitals";
import HospitalCardComponent, { type Hospital as HospitalType } from "../components/HospitalCard";

interface Hospital extends HospitalType { }

const FindHospitals = () => {
  const location = useLocation();

  // Parse URL params on mount
  const [query, setQuery] = useState("");
  const [selectedLocation, setSelectedLocation] = useState("");
  const [selectedSpecializations, setSelectedSpecializations] = useState<string[]>([]);
  const [userCoordinates, setUserCoordinates] = useState<{ lat: number; lng: number } | null>(null);
  const [currentRealPosition, setCurrentRealPosition] = useState<{ lat: number; lng: number } | null>(null);

  // Data states
  const [hospitals, setHospitals] = useState<Hospital[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const locParam = urlParams.get("loc");

    // If location is in URL params, use it; otherwise it will be detected from geolocation
    if (locParam) {
      setSelectedLocation(decodeURIComponent(locParam));
    } else {
      // Try to detect user's city from geolocation
      const detectLocation = async () => {
        let coords = null;

        if (Capacitor.isNativePlatform()) {
          try {
            const status = await Geolocation.checkPermissions();
            if (status.location !== 'granted') {
              await Geolocation.requestPermissions();
            }
            // Increased timeout to 30s and enabled high accuracy for better results
            const pos = await Geolocation.getCurrentPosition({
              enableHighAccuracy: true,
              timeout: 30000,
              maximumAge: Infinity // Accept any cached position to be faster
            });
            coords = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
          } catch (e) {
            console.error("Native location error:", e);
            // Fallback is handled below (coords remains null)
          }
        } else if (navigator.geolocation) {
          try {
            const pos = await new Promise<GeolocationPosition>((resolve, reject) =>
              navigator.geolocation.getCurrentPosition(resolve, reject)
            );
            coords = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
          } catch (e) { console.error(e); }
        }

        if (coords) {
          try {
            const { latitude, longitude } = coords;
            const response = await fetch(
              `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`
            );
            const data = await response.json();
            if (data.city) {
              setSelectedLocation(data.city);
            } else if (data.locality) {
              setSelectedLocation(data.locality);
            } else if (data.principalSubdivision) {
              setSelectedLocation(data.principalSubdivision);
            } else {
              setSelectedLocation("Vijayawada");
            }
          } catch (error) {
            console.error("Error getting location:", error);
            setSelectedLocation("Vijayawada");
          }
        } else {
          setSelectedLocation("Vijayawada");
        }
      };

      detectLocation();
    }

    // Always try to get real position for distance calculation
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((pos) => {
        setCurrentRealPosition({ lat: pos.coords.latitude, lng: pos.coords.longitude });
      });
    }

    setQuery(urlParams.get("q") || "");
    const specsParam = urlParams.getAll("spec");
    if (specsParam.length > 0) {
      setSelectedSpecializations(specsParam.map((s) => decodeURIComponent(s)));
    } else {
      setSelectedSpecializations([]);
    }

    // Check if we have coordinates in the URL for nearby hospitals
    const lat = urlParams.get("lat");
    const lng = urlParams.get("lng");
    if (lat && lng) {
      setUserCoordinates({ lat: parseFloat(lat), lng: parseFloat(lng) });
    } else {
      setUserCoordinates(null);
    }
  }, [location]);

  useEffect(() => {
    let cancelled = false;

    // Fetch regular hospitals based on location and specialization filters
    const fetchHospitals = async () => {
      setLoading(true);
      setError(null);
      try {
        let data;

        // Build query parameters
        const params = new URLSearchParams();

        // Always add city filter
        if (selectedLocation) {
          params.append("city", selectedLocation);
        }

        // Add specialization filters (multi-select)
        if (selectedSpecializations.length > 0) {
          selectedSpecializations.forEach((spec) => params.append("spec", spec));
        }

        // Add search query if specified
        if (query) {
          params.append("search", query);
        }

        // If current real position is available, always add lat/lng for distance calculation
        const effectiveLat = userCoordinates?.lat || currentRealPosition?.lat;
        const effectiveLng = userCoordinates?.lng || currentRealPosition?.lng;

        if (effectiveLat && effectiveLng) {
          params.append("lat", effectiveLat.toString());
          params.append("lng", effectiveLng.toString());
        }

        const queryString = params.toString();
        // If user coordinates (from search/nearby) are specifically requested, use distance sorting endpoint
        const url = userCoordinates
          ? `/api/clinics/sorted-by-distance${queryString ? `?${queryString}` : ""}`
          : `/api/clinics${queryString ? `?${queryString}` : ""}`;

        data = await apiRequest<Hospital[]>(url, "GET");

        if (!cancelled) {
          setHospitals(data || []);
        }
      } catch (err) {
        if (!cancelled) {
          setError((err as Error)?.message || "Failed to load hospitals");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    fetchHospitals();

    return () => {
      cancelled = true;
    };
  }, [selectedLocation, selectedSpecializations, userCoordinates, query]);

  useEffect(() => {
    console.log("Fetched hospitals:", hospitals);
  }, [hospitals]);

  // No need for client-side filtering anymore since backend handles it
  const filteredHospitals = hospitals;



  return (
    <ProtectedRoute>
      <div className="min-h-[calc(100vh-64px)] bg-gray-50 dark:bg-slate-900 transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4 py-6 sm:py-8">
          <HospitalSearch />

          {/* Results */}
          <div className="mt-8">
            {userCoordinates && (
              <div className="mb-8">
                <h2 className="text-lg sm:text-xl font-semibold text-gray-900 dark:text-slate-100 mb-4">
                  Nearby Hospitals
                </h2>
                <NearbyHospitals latitude={userCoordinates.lat} longitude={userCoordinates.lng} />
              </div>
            )}

            <h2 className="text-lg sm:text-xl font-semibold text-gray-900 dark:text-slate-100 mb-4">
              {userCoordinates ? "Other " : ""}Hospitals{selectedLocation ? ` in ${selectedLocation}` : ""}
              {query && (
                <span className="text-gray-500 dark:text-slate-400 font-normal"> {" "}for "{query}"</span>
              )}
              {selectedSpecializations.length > 0 && (
                <span className="text-gray-500 dark:text-slate-400 font-normal">
                  {" "}specializing in {selectedSpecializations.join(", ")}
                </span>
              )}
            </h2>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="bg-white dark:bg-slate-800 border border-gray-200 dark:border-slate-700 rounded-lg p-4 sm:p-6 shadow-md animate-pulse">
                    <div className="w-full h-44 bg-gray-200 dark:bg-slate-700 rounded-md mb-4" />
                    <div className="h-4 bg-gray-200 dark:bg-slate-700 rounded w-3/4 mb-3" />
                    <div className="h-3 bg-gray-200 dark:bg-slate-700 rounded w-1/2 mb-3" />
                    <div className="flex gap-2 mb-4">
                      <div className="h-6 bg-gray-200 dark:bg-slate-700 rounded-full w-16" />
                      <div className="h-6 bg-gray-200 dark:bg-slate-700 rounded-full w-20" />
                    </div>
                    <div className="mt-auto">
                      <div className="h-10 bg-gray-200 dark:bg-slate-700 rounded-md w-full" />
                    </div>
                  </div>
                ))}
              </div>
            ) : error ? (
              <div className="bg-white dark:bg-slate-800 rounded-lg p-6 text-center border border-gray-200 dark:border-slate-600">
                <p className="text-red-500 dark:text-red-400">{error}</p>
              </div>
            ) : filteredHospitals.length === 0 ? (
              <div className="bg-white dark:bg-slate-800 rounded-lg p-6 text-center border border-gray-200 dark:border-slate-600">
                <p className="text-gray-500 dark:text-slate-300">No hospitals found.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredHospitals.map((hospital) => (
                  <HospitalCardComponent
                    key={hospital.id || hospital.clinicId}
                    hospital={hospital}
                    theme={document.documentElement.classList.contains('dark') ? 'dark' : 'light'}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

      </div>
    </ProtectedRoute>
  );
};

export default FindHospitals;