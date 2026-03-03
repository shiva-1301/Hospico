import { useState } from "react";
import { Star, X } from "lucide-react";

interface ReviewModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (rating: number, comment: string) => Promise<void>;
    title: string;
    subtitle?: string;
}

export default function ReviewModal({ isOpen, onClose, onSubmit, title, subtitle }: ReviewModalProps) {
    const [rating, setRating] = useState(0);
    const [comment, setComment] = useState("");
    const [hoveredRating, setHoveredRating] = useState(0);
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async () => {
        if (rating === 0) {
            alert("Please select a rating");
            return;
        }
        setIsSubmitting(true);
        try {
            await onSubmit(rating, comment);
            // Reset form after successful submission
            setRating(0);
            setComment("");
            onClose();
        } catch (error) {
            console.error("Failed to submit review", error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
            <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all scale-100 opacity-100">
                {/* Header */}
                <div className="relative p-6 border-b border-gray-100 dark:border-slate-700 bg-gray-50 dark:bg-slate-800/50">
                    <button
                        onClick={onClose}
                        className="absolute top-4 right-4 p-1 rounded-full hover:bg-gray-200 dark:hover:bg-slate-700 text-gray-400 hover:text-gray-600 dark:text-slate-500 dark:hover:text-slate-300 transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                    <h3 className="text-xl font-bold text-gray-900 dark:text-slate-100 pr-8">{title}</h3>
                    {subtitle && <p className="text-sm text-gray-500 dark:text-slate-400 mt-1">{subtitle}</p>}
                </div>

                {/* Body */}
                <div className="p-6 space-y-6">
                    {/* Star Rating */}
                    <div className="flex flex-col items-center justify-center space-y-2">
                        <label className="text-sm font-semibold text-gray-600 dark:text-slate-300 uppercase tracking-wide">
                            Rate your experience
                        </label>
                        <div className="flex gap-2">
                            {[1, 2, 3, 4, 5].map((star) => (
                                <button
                                    key={star}
                                    onMouseEnter={() => setHoveredRating(star)}
                                    onMouseLeave={() => setHoveredRating(0)}
                                    onClick={() => setRating(star)}
                                    className="focus:outline-none transition-transform hover:scale-110 duration-200"
                                >
                                    <Star
                                        className={`w-10 h-10 ${star <= (hoveredRating || rating)
                                                ? "fill-yellow-400 text-yellow-400 drop-shadow-md"
                                                : "fill-gray-100 dark:fill-slate-700 text-gray-300 dark:text-slate-600"
                                            }`}
                                    />
                                </button>
                            ))}
                        </div>
                        <p className="text-sm font-medium text-blue-600 dark:text-blue-400 min-h-[1.25rem]">
                            {hoveredRating === 1 ? "Poor" :
                                hoveredRating === 2 ? "Fair" :
                                    hoveredRating === 3 ? "Good" :
                                        hoveredRating === 4 ? "Very Good" :
                                            hoveredRating === 5 ? "Excellent!" :
                                                rating > 0 && rating <= 5 ? ["Poor", "Fair", "Good", "Very Good", "Excellent!"][rating - 1] : ""}
                        </p>
                    </div>

                    {/* Comment Area */}
                    <div className="space-y-2">
                        <label className="text-sm font-semibold text-gray-700 dark:text-slate-300">
                            Share your feedback (Optional)
                        </label>
                        <textarea
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            placeholder="Tell us about the doctor, facilities, and care provided..."
                            rows={4}
                            className="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-slate-600 bg-gray-50 dark:bg-slate-700/50 text-gray-900 dark:text-slate-100 placeholder-gray-400 dark:placeholder-slate-500 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all resize-none"
                        />
                    </div>
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-gray-100 dark:border-slate-700 flex gap-3">
                    <button
                        onClick={onClose}
                        className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 dark:border-slate-600 text-gray-700 dark:text-slate-300 font-medium hover:bg-gray-50 dark:hover:bg-slate-700 transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting || rating === 0}
                        className="flex-1 px-4 py-2.5 rounded-xl bg-blue-600 text-white font-medium hover:bg-blue-700 active:bg-blue-800 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg shadow-blue-500/30"
                    >
                        {isSubmitting ? "Submitting..." : "Submit Review"}
                    </button>
                </div>
            </div>
        </div>
    );
}
