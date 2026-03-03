import { useState, useEffect, useCallback } from "react";
import { Phone, AlertTriangle, Ambulance, ChevronFirst as FirstAid, MapPin, Clock } from 'lucide-react';
import { apiRequest } from "../api";
import HospitalCardComponent, { type Hospital } from "../components/HospitalCard";

type NearbyFacility = Hospital;

export default function Emergency() {
  const [facilities, setFacilities] = useState<NearbyFacility[]>([]);
  const [loadingNearby, setLoadingNearby] = useState<boolean>(false);
  const [errorNearby, setErrorNearby] = useState<string | null>(null);

  const fetchNearbyFacilities = useCallback(() => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setErrorNearby("Geolocation is not supported in this browser.");
      return;
    }

    setLoadingNearby(true);
    setErrorNearby(null);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const { latitude, longitude } = position.coords;
          const data = await apiRequest<NearbyFacility[]>(
            `/api/clinics/nearby?lat=${latitude}&lng=${longitude}`,
            "GET"
          );

          const sorted = data
            .slice()
            .sort((a, b) => (a.distanceKm ?? Number.POSITIVE_INFINITY) - (b.distanceKm ?? Number.POSITIVE_INFINITY));

          setFacilities(sorted.slice(0, 2));
        } catch (err: unknown) {
          const message = err instanceof Error ? err.message : "Failed to load nearby facilities";
          setErrorNearby(message);
        } finally {
          setLoadingNearby(false);
        }
      },
      (geoErr) => {
        setLoadingNearby(false);
        setErrorNearby(geoErr.message || "Unable to get your location");
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }, []);

  useEffect(() => {
    fetchNearbyFacilities();
  }, [fetchNearbyFacilities]);

  return (
    <div className="space-y-16">
      {/* Emergency Banner */}
      <section className="bg-red-600 text-white p-8 rounded-xl">
        <div className="max-w-4xl mx-auto flex items-center gap-4">
          <AlertTriangle className="h-12 w-12" />
          <div>
            <h1 className="text-3xl font-bold mb-2">Emergency Services</h1>
            <p className="text-xl">
              If you're experiencing a medical emergency, call emergency services immediately:
              <a href="tel:911" className="ml-2 font-bold underline">911</a>
            </p>
          </div>
        </div>
      </section>

      {/* Quick Actions */}
      <section className="max-w-6xl mx-auto">
        <div className="grid md:grid-cols-3 gap-8">
          <div className="bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 transition-colors duration-200">
            <div className="bg-red-50 dark:bg-red-900/30 p-3 rounded-full w-fit mb-4">
              <Ambulance className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h3 className="font-semibold text-lg mb-2 text-gray-900 dark:text-gray-100">Ambulance Service</h3>
            <p className="text-gray-600 dark:text-gray-300 mb-4">24/7 emergency medical transportation</p>
            <a href="tel:911" className="text-red-600 dark:text-red-400 font-semibold flex items-center gap-2 hover:text-red-700 dark:hover:text-red-300 transition-colors">
              <Phone className="h-5 w-5" />
              Call Now
            </a>
          </div>

          <div className="bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 transition-colors duration-200">
            <div className="bg-red-50 dark:bg-red-900/30 p-3 rounded-full w-fit mb-4">
              <FirstAid className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h3 className="font-semibold text-lg mb-2 text-gray-900 dark:text-gray-100">Emergency Rooms</h3>
            <p className="text-gray-600 dark:text-gray-300 mb-4">Find nearest emergency facilities</p>
            <button
              onClick={fetchNearbyFacilities}
              className="text-red-600 dark:text-red-400 font-semibold flex items-center gap-2 hover:text-red-700 dark:hover:text-red-300 transition-colors"
            >
              <MapPin className="h-5 w-5" />
              Locate ER
            </button>
          </div>

          <div className="bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 transition-colors duration-200">
            <div className="bg-red-50 dark:bg-red-900/30 p-3 rounded-full w-fit mb-4">
              <Clock className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h3 className="font-semibold text-lg mb-2 text-gray-900 dark:text-gray-100">Wait Times</h3>
            <p className="text-gray-600 dark:text-gray-300 mb-4">Real-time ER wait time information</p>
            <button className="text-red-600 dark:text-red-400 font-semibold flex items-center gap-2 hover:text-red-700 dark:hover:text-red-300 transition-colors">
              Check Times
            </button>
          </div>
        </div>
      </section>

      {/* Emergency Guidelines */}
      <section className="max-w-4xl mx-auto">
        <h2 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">When to Seek Emergency Care</h2>
        <div className="bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 transition-colors duration-200">
          <ul className="space-y-4">
            {[
              "Chest pain or difficulty breathing",
              "Severe abdominal pain",
              "Stroke symptoms (face drooping, arm weakness, speech difficulty)",
              "Severe head injury or loss of consciousness",
              "Uncontrolled bleeding",
              "Poisoning or overdose",
              "Severe allergic reactions",
              "Major burns or injuries"
            ].map((symptom) => (
              <li key={symptom} className="flex items-center gap-3 text-gray-700 dark:text-gray-200">
                <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0" />
                <span>{symptom}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* Nearby Facilities */}
      <section className="max-w-6xl mx-auto pb-10">
        <h2 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Nearby Emergency Facilities</h2>
        <div className="grid md:grid-cols-2 gap-6">
          {loadingNearby && (
            <div className="md:col-span-2 text-gray-600 dark:text-gray-400">Locating nearby emergency rooms...</div>
          )}

          {errorNearby && !loadingNearby && (
            <div className="md:col-span-2 text-red-700 dark:text-red-300 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 transition-colors duration-200">
              {errorNearby}
            </div>
          )}

          {!loadingNearby && !errorNearby && facilities.length === 0 && (
            <div className="md:col-span-2 text-gray-600 dark:text-gray-400 bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 transition-colors duration-200">
              No nearby emergency facilities found.
            </div>
          )}

          {!loadingNearby && !errorNearby && facilities.map((facility) => (
            <HospitalCardComponent
              key={facility.clinicId || facility.id}
              hospital={facility}
              theme={document.documentElement.classList.contains('dark') ? 'dark' : 'light'}
            />
          ))}
        </div>
      </section>
    </div>
  );
}