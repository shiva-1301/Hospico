const FullScreenLoader = () => {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-blue-50 bg-opacity-70"
      style={{ pointerEvents: "all" }} // blocks clicks
    >
      <div className="flex flex-col items-center space-y-4">
        {/* Friendly Pulsating Circle */}
        <div className="relative">
          <div className="absolute inset-0 rounded-full bg-blue-200 opacity-50 animate-ping"></div>
          <div className="h-16 w-16 rounded-full bg-blue-400 flex items-center justify-center shadow-lg">
            {/* Optional icon, like a heart */}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-8 w-8 text-white animate-pulse"
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 
                       4.42 3 7.5 3c1.74 0 3.41 0.81 4.5 2.09 
                       C13.09 3.81 14.76 3 16.5 3 
                       19.58 3 22 5.42 22 8.5c0 3.78-3.4 
                       6.86-8.55 11.54L12 21.35z"
              />
            </svg>
          </div>
        </div>

        {/* Friendly Message */}
        <p className="text-blue-700 text-lg font-medium text-center">
          Please wait, we are preparing your information...
        </p>
      </div>
    </div>
  );
};

export default FullScreenLoader;
