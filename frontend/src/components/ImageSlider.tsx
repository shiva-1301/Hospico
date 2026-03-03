import { useState, useEffect, useCallback } from "react";
import { ChevronLeft, ChevronRight, Share2, Image } from "lucide-react";

const images = [
  {
    url: "https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=1920&q=80",
    title: "State-of-the-art Facilities",
    description:
      "Experience world-class healthcare in modern, well-equipped facilities",
  },
  {
    url: "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=1920&q=80",
    title: "Professional Medical Staff",
    description:
      "Our experienced healthcare professionals are dedicated to your well-being",
  },
  {
    url: "https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=1920&q=80",
    title: "Advanced Medical Technology",
    description:
      "Access to the latest medical technology for accurate diagnosis and treatment",
  },
  {
    url: "https://images.unsplash.com/photo-1538108149393-fbbd81895907?auto=format&fit=crop&w=1920&q=80",
    title: "Patient-Centered Care",
    description:
      "Committed to providing compassionate and personalized healthcare services",
  },
];

export default function ImageSlider() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);

  const nextSlide = useCallback(() => {
    setCurrentIndex((prevIndex) => (prevIndex + 1) % images.length);
  }, []);

  const prevSlide = () => {
    setCurrentIndex(
      (prevIndex) => (prevIndex - 1 + images.length) % images.length
    );
  };

  const goToSlide = (index: number) => {
    setCurrentIndex(index);
  };

  useEffect(() => {
    let intervalId: number;

    if (isAutoPlaying) {
      intervalId = window.setInterval(nextSlide, 5000);
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [isAutoPlaying, nextSlide]);

  return (
    <div
      className="relative h-[600px] overflow-hidden group"
      onMouseEnter={() => setIsAutoPlaying(false)}
      onMouseLeave={() => setIsAutoPlaying(true)}
    >
      {/* Images */}
      <div className="relative h-full">
        {images.map((image, index) => (
          <div
            key={index}
            className={`absolute inset-0 transition-opacity duration-1000 ${
              index === currentIndex ? "opacity-100" : "opacity-0"
            }`}
          >
            <img
              src={image.url}
              alt={image.title}
              className="w-full h-full object-cover"
            />
            {/* Gradient Overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />

            {/* Text Overlay */}
            <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
              <h2 className="text-3xl font-bold mb-2">{image.title}</h2>
              <p className="text-lg text-gray-200">{image.description}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Navigation Arrows */}
      <button
        onClick={prevSlide}
        className="absolute left-4 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/30 text-white opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:bg-black/50 cursor-pointer"
      >
        <ChevronLeft className="h-6 w-6" />
      </button>
      <button
        onClick={nextSlide}
        className="absolute right-4 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/30 text-white opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:bg-black/50 cursor-pointer"
      >
        <ChevronRight className="h-6 w-6" />
      </button>

      {/* Dot Indicators */}
      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex space-x-2">
        {images.map((_, index) => (
          <button
            key={index}
            onClick={() => goToSlide(index)}
            className={`w-2.5 h-2.5 rounded-full transition-all duration-300 ${
              index === currentIndex
                ? "bg-white w-8"
                : "bg-white/50 hover:bg-white/75"
            }`}
          />
        ))}
      </div>

      {/* Top Right Controls */}
      <div className="absolute top-4 right-4 flex space-x-2">
        <button className="p-2 rounded-full bg-black/30 text-white hover:bg-black/50 transition-colors duration-300">
          <Share2 className="h-5 w-5" />
        </button>
        <button className="p-2 rounded-full bg-black/30 text-white hover:bg-black/50 transition-colors duration-300 flex items-center space-x-2">
          <Image className="h-5 w-5" />
          <span className="text-sm">View All Photos</span>
        </button>
      </div>
    </div>
  );
}
