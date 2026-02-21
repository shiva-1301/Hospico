import { useNavigate } from "react-router-dom";
import { Star, MapPin, Navigation, Clock } from "lucide-react";
import { motion } from "framer-motion";
import defaultHospitalImage from "../assets/images/default-hospital.jpg";

export interface Hospital {
    id: string | number;
    clinicId?: string | number; // Support both naming conventions
    name: string;
    address?: string;
    city?: string;
    imageUrl?: string;
    imageurl?: string; // Support both naming conventions
    rating?: number;
    distance?: number;
    distanceKm?: number;
    estimatedTime?: number;
    estimatedWaitMinutes?: number;
    specialties?: string[];
    specializations?: string[];
    latitude?: number;
    longitude?: number;
}

interface HospitalCardProps {
    hospital: Hospital;
    theme?: "light" | "dark";
    showDistance?: boolean;
}

const HospitalCard = ({
    hospital,
    theme = "light",
    showDistance = true
}: HospitalCardProps) => {
    const navigate = useNavigate();

    // Normalize fields
    const id = hospital.clinicId || hospital.id;
    const imageUrl = hospital.imageurl || hospital.imageUrl || defaultHospitalImage;
    const specializations = hospital.specializations || hospital.specialties || [];
    const distance = hospital.distanceKm ?? hospital.distance;
    const time = hospital.estimatedWaitMinutes ?? hospital.estimatedTime;

    const handleCardClick = () => {
        navigate(`/find-hospital/${id}`);
    };

    const handleMapClick = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (hospital.latitude && hospital.longitude) {
            window.open(`https://www.google.com/maps/search/?api=1&query=${hospital.latitude},${hospital.longitude}`, '_blank');
        } else {
            window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(hospital.name + " " + (hospital.address || hospital.city || ""))}`, '_blank');
        }
    };

    const formatDistance = (d: number): string => {
        if (d < 1) return `${Math.round(d * 1000)}m`;
        return `${d.toFixed(1)}km`;
    };

    return (
        <motion.div
            whileHover={{ scale: 1.02, translateY: -4 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleCardClick}
            className={`group relative flex flex-col h-full rounded-2xl overflow-hidden cursor-pointer transition-all duration-300 border ${theme === "dark"
                ? "bg-slate-800/50 border-slate-700 hover:border-blue-500/50 hover:bg-slate-800"
                : "bg-white border-gray-100 hover:border-blue-200 hover:shadow-xl shadow-sm"
                }`}
        >
            {/* Image Section */}
            <div className="relative h-48 w-full overflow-hidden bg-gray-100 dark:bg-slate-700">
                <img
                    src={imageUrl}
                    alt={hospital.name}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                    onError={(e) => {
                        (e.target as HTMLImageElement).src = defaultHospitalImage;
                    }}
                />
                {/* Rating Badge */}
                {hospital.rating && hospital.rating > 0 && (
                    <div className="absolute top-3 left-3 px-2 py-1 bg-white/90 dark:bg-slate-800/90 backdrop-blur-md rounded-lg flex items-center gap-1 shadow-sm">
                        <Star size={14} className="text-yellow-400 fill-yellow-400" />
                        <span className="text-xs font-bold text-gray-900 dark:text-white">{hospital.rating.toFixed(1)}</span>
                    </div>
                )}

                {/* Distance Overlay (if desired to show on image) */}
                {showDistance && distance !== undefined && (
                    <div className="absolute bottom-3 right-3 px-2.5 py-1 bg-blue-600 text-white rounded-lg flex items-center gap-1 text-xs font-bold shadow-lg">
                        <MapPin size={12} />
                        {formatDistance(distance)}
                    </div>
                )}
            </div>

            {/* Content Section */}
            <div className="p-4 sm:p-5 flex flex-col flex-1">
                <h3 className={`text-lg font-bold mb-1 line-clamp-1 transition-colors ${theme === "dark" ? "text-white group-hover:text-blue-400" : "text-gray-900 group-hover:text-blue-600"
                    }`}>
                    {hospital.name}
                </h3>

                <p className={`text-xs mb-3 flex items-start gap-1 line-clamp-2 ${theme === "dark" ? "text-slate-400" : "text-gray-500"
                    }`}>
                    <MapPin size={12} className="mt-0.5 shrink-0" />
                    {hospital.address || hospital.city}
                </p>

                {/* Proximity Info (Optional secondary location display) */}
                {time !== undefined && (
                    <div className={`flex items-center gap-3 mb-4 text-[10px] font-medium uppercase tracking-wider ${theme === "dark" ? "text-slate-500" : "text-gray-400"
                        }`}>
                        <span className="flex items-center gap-1">
                            <Clock size={10} />
                            {time} min wait
                        </span>
                    </div>
                )}

                {/* Specializations */}
                <div className="flex flex-wrap gap-1.5 mb-4">
                    {specializations.slice(0, 3).map((spec) => (
                        <span
                            key={spec}
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${theme === "dark"
                                ? "bg-blue-500/10 text-blue-400 border border-blue-500/20"
                                : "bg-blue-50 text-blue-600 border border-blue-100"
                                }`}
                        >
                            {spec}
                        </span>
                    ))}
                    {specializations.length > 3 && (
                        <span className={`text-[10px] font-medium mt-0.5 ${theme === "dark" ? "text-slate-500" : "text-gray-400"}`}>
                            +{specializations.length - 3} more
                        </span>
                    )}
                </div>

                {/* Action Button */}
                <div className="mt-auto flex items-center gap-2">
                    <button
                        className={`flex-1 py-2 px-4 rounded-xl text-sm font-bold transition-all ${theme === "dark"
                            ? "bg-blue-600 hover:bg-blue-500 text-white shadow-soft-blue"
                            : "bg-blue-600 hover:bg-blue-700 text-white"
                            }`}
                    >
                        View Details
                    </button>

                    <button
                        onClick={handleMapClick}
                        className={`p-2 rounded-xl border transition-all ${theme === "dark"
                            ? "border-slate-700 hover:border-blue-500 text-blue-400 hover:bg-blue-500/10"
                            : "border-gray-200 hover:border-blue-300 text-blue-600 hover:bg-blue-50"
                            }`}
                        title="Navigate on Google Maps"
                    >
                        <Navigation size={18} fill="currentColor" />
                    </button>
                </div>
            </div>
        </motion.div>
    );
};

export default HospitalCard;
