import React from 'react';
import { WifiOff } from 'lucide-react'; // Assuming lucide-react is used, or I'll use a standard SVG

const OfflineScreen: React.FC<{ onRetry: () => void }> = ({ onRetry }) => {
    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-white dark:bg-slate-900 px-6 text-center">
            <div className="bg-blue-50 dark:bg-slate-800 p-6 rounded-full mb-6 animate-pulse">
                <WifiOff size={64} className="text-blue-500 dark:text-blue-400" />
            </div>

            <h2 className="text-2xl font-bold text-slate-800 dark:text-white mb-3">
                No Internet Connection
            </h2>

            <p className="text-slate-500 dark:text-slate-400 mb-8 max-w-xs mx-auto">
                Your Internet connection was lost, please reconnect and try again
            </p>

            <button
                onClick={onRetry}
                className="w-full max-w-xs bg-blue-500 hover:bg-blue-600 active:scale-95 text-white font-semibold py-3.5 px-6 rounded-xl shadow-lg transition-all duration-200"
            >
                Try again
            </button>

            <button
                onClick={() => window.location.reload()}
                className="mt-4 text-sm text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors"
            >
                Cancel
            </button>
        </div>
    );
};

export default OfflineScreen;
