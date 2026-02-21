import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { apiRequest } from "../api";

interface Specialization {
  id: number;
  specialization: string;
}

const HospitalSearch = () => {
  const navigate = useNavigate();
  const [searchText, setSearchText] = useState("");
  const [selectedLocation, setSelectedLocation] = useState("");
  const [isGettingLocation, setIsGettingLocation] = useState(true);

  // Detect user's location on mount
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          try {
            const response = await fetch(
              `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`
            );
            const data = await response.json();
            if (data.city) setSelectedLocation(data.city);
            else if (data.locality) setSelectedLocation(data.locality);
            else if (data.principalSubdivision) setSelectedLocation(data.principalSubdivision);
            else setSelectedLocation("Vijayawada");
          } catch (e) {
            console.error("Error getting location name:", e);
            setSelectedLocation("Vijayawada");
          } finally {
            setIsGettingLocation(false);
          }
        },
        (error) => {
          console.error("Error getting location:", error);
          setSelectedLocation("Vijayawada");
          setIsGettingLocation(false);
        }
      );
    } else {
      setSelectedLocation("Vijayawada");
      setIsGettingLocation(false);
    }
  }, []);

  const handleSearch = () => {
    const params = new URLSearchParams({
      q: encodeURIComponent(searchText),
      loc: encodeURIComponent(selectedLocation),
    });
    navigate(`/find-hospitals?${params.toString()}`);
  };

  const getCurrentLocation = () => {
    setIsGettingLocation(true);
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          try {
            const response = await fetch(
              `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`
            );
            const data = await response.json();
            if (data.city) setSelectedLocation(data.city);
            else if (data.locality) setSelectedLocation(data.locality);
            else setSelectedLocation(`${data.principalSubdivision} (${latitude.toFixed(4)}, ${longitude.toFixed(4)})`);
          } catch (e) {
            console.error("Error getting location name:", e);
            setSelectedLocation(`(${latitude.toFixed(4)}, ${longitude.toFixed(4)})`);
          } finally {
            setIsGettingLocation(false);
            const params = new URLSearchParams({
              lat: latitude.toString(),
              lng: longitude.toString(),
              loc: selectedLocation,
            });
            navigate(`/find-hospitals?${params.toString()}`);
          }
        },
        (error) => {
          console.error("Error getting location:", error);
          alert("Unable to get your location. Please enter manually.");
          setIsGettingLocation(false);
        }
      );
    } else {
      alert("Geolocation is not supported by this browser.");
      setIsGettingLocation(false);
    }
  };

  return (
    <div className="bg-white/90 dark:bg-slate-800/50 backdrop-blur-xl border border-gray-200 dark:border-white/10 rounded-2xl p-4 sm:p-5 shadow-lg transition-colors duration-200">
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row gap-2 sm:gap-3">
          <div className="relative flex-1 sm:flex-[0.80]">
            <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none text-gray-400 dark:text-slate-300">
              <svg
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
            <input
              className="block w-full pl-10 pr-4 py-2 sm:py-3 rounded-xl bg-gray-50 dark:bg-slate-900/50 border border-gray-300 dark:border-slate-600 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-slate-400 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 shadow-sm transition-colors duration-200"
              placeholder="Search hospitals, specialties…"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
          <div className="relative flex-1 sm:flex-[0.20]">
            <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none text-gray-400 dark:text-slate-300">
              <svg
                className="h-5 w-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
            </div>
            <select
              className="block w-full pl-10 pr-20 py-2 sm:py-3 rounded-xl bg-gray-50 dark:bg-slate-900/50 border border-gray-300 dark:border-slate-600 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 appearance-none shadow-sm transition-colors duration-200"
              value={selectedLocation}
              onChange={(e) => setSelectedLocation(e.target.value)}
            >
              <option value="">Select City</option>
              <option value="Vijayawada">Vijayawada</option>
              <option value="Guntur">Guntur</option>
              <option value="Visakhapatnam">Visakhapatnam</option>
              <option value="Hyderabad">Hyderabad</option>
              <option value="Bangalore">Bangalore</option>
            </select>
            <div className="absolute inset-y-0 right-14 flex items-center pr-2 pointer-events-none text-gray-400 dark:text-slate-400">
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
              </svg>
            </div>
            <button
              className="absolute inset-y-0 right-0 m-1 px-3 py-1 text-xs font-semibold text-blue-100 bg-blue-600 hover:bg-blue-500 rounded-lg flex items-center justify-center shadow-lg border border-blue-500/30"
              onClick={getCurrentLocation}
              disabled={isGettingLocation}
            >
              {isGettingLocation ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              ) : (
                <>
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="8" strokeWidth="2" />
                    <circle cx="12" cy="12" r="3" strokeWidth="2" />
                    <line x1="12" y1="4" x2="12" y2="2" strokeWidth="2" />
                    <line x1="12" y1="22" x2="12" y2="20" strokeWidth="2" />
                    <line x1="4" y1="12" x2="2" y2="12" strokeWidth="2" />
                    <line x1="22" y1="12" x2="20" y2="12" strokeWidth="2" />
                  </svg>
                </>
              )}
            </button>
          </div>
          <button
            className="w-full sm:w-auto px-5 py-2 sm:py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-xl border border-blue-500/50 transition-colors"
            onClick={handleSearch}
          >
            Search
          </button>
        </div>
      </div>
      <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-slate-200 mt-3">
        <span className="whitespace-nowrap">Filter by specialty:</span>
      </div>
      <SpecialtyFilters
        searchText={searchText}
        selectedLocation={selectedLocation}
      />
    </div>
  );
};

function SpecialtyFilters({ searchText, selectedLocation }: {
  searchText: string;
  selectedLocation: string;
}) {
  const [specialties, setSpecialties] = useState<Specialization[]>([]);
  const [selectedSpecializations, setSelectedSpecializations] = useState<string[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchSpecialties = async () => {
      try {
        const data = await apiRequest<Specialization[]>(
          "/api/specializations",
          "GET"
        );
        setSpecialties(data);
      } catch (error) {
        console.error("Failed to fetch specializations:", error);
        // Fallback to hardcoded list if API fails
        setSpecialties([
          { id: 1, specialization: "Dermatologist" },
          { id: 2, specialization: "Dentist" },
          { id: 3, specialization: "Cardiologist" },
          { id: 4, specialization: "ENT Specialist" },
          { id: 5, specialization: "General Physician" },
          { id: 6, specialization: "Pediatrician" },
          { id: 7, specialization: "Gynecologist" },
          { id: 8, specialization: "Neurologist" },
        ]);
      }
    };

    fetchSpecialties();
  }, []);

  const handleSpecializationClick = (specialization: string) => {
    // Toggle specialization in array
    const exists = selectedSpecializations.includes(specialization);
    const nextSelection = exists
      ? selectedSpecializations.filter((s) => s !== specialization)
      : [...selectedSpecializations, specialization];
    setSelectedSpecializations(nextSelection);

    // Build params for navigation using current search text and selected location
    const params = new URLSearchParams({
      q: encodeURIComponent(searchText),
      loc: encodeURIComponent(selectedLocation),
    });

    // Append all selected specs as repeated params
    nextSelection.forEach((spec) => params.append("spec", encodeURIComponent(spec)));

    navigate(`/find-hospitals?${params.toString()}`);
  };

  const scrollLeft = () => {
    document.getElementById("specialties-container")?.scrollBy({ left: -120 });
  };

  const scrollRight = () => {
    document.getElementById("specialties-container")?.scrollBy({ left: 120 });
  };

  return (
    <div className="mt-2 flex items-center gap-1">
      <button
        className="flex-shrink-0 p-1 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-400 dark:text-slate-200"
        onClick={scrollLeft}
      >
        <svg
          className="h-4 w-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M15 19l-7-7 7-7"
          />
        </svg>
      </button>
      <div
        id="specialties-container"
        className="flex flex-1 gap-2 overflow-x-auto pb-2"
      >
        {specialties
          .filter((s, i, arr) => arr.findIndex(x => x.specialization === s.specialization) === i)
          .map((s, index) => (
          <button
            key={`${s.id}-${index}`}
            className={`flex-shrink-0 rounded-full px-3 py-1 text-xs font-semibold transition-colors border ${selectedSpecializations.includes(s.specialization)
              ? "bg-blue-500 text-white border-blue-400 shadow-lg"
              : "bg-white text-gray-700 border-gray-200 hover:border-blue-500 hover:text-blue-600 dark:bg-slate-800/80 dark:text-slate-100 dark:border-slate-700 dark:hover:border-blue-500 dark:hover:text-white"
              }`}
            onClick={() => handleSpecializationClick(s.specialization)}
          >
            {s.specialization === "ENT" || s.specialization === "ENT Specialist"
              ? "Ear, Nose & Throat"
              : s.specialization}
          </button>
        ))}
      </div>
      <button
        className="flex-shrink-0 p-1 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 text-gray-400 dark:text-slate-200"
        onClick={scrollRight}
      >
        <svg
          className="h-4 w-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M9 5l7 7-7 7"
          />
        </svg>
      </button>
    </div>
  );
}

export default HospitalSearch;