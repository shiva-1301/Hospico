import { X, Globe } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface Language {
    code: string;
    name: string;
    nativeName: string;
}

const languages: Language[] = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी' },
    { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ' },
    { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ' },
    { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી' },
    { code: 'mr', name: 'Marathi', nativeName: 'મરાઠી' },
    { code: 'te', name: 'Telugu', nativeName: 'తెలుగు' },
    { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்' },
    { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া' },
    { code: 'bn', name: 'Bengali', nativeName: 'বাংলা' },
    { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം' },
    { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ' },
    { code: 'kok', name: 'Konkani', nativeName: 'कोंकणी' },
    { code: 'ks', name: 'Kashmiri', nativeName: 'کأشُر' },
    { code: 'mni', name: 'Manipuri', nativeName: 'মৈতৈলোন্' },
    { code: 'ur', name: 'Urdu', nativeName: 'اردو' },
];

interface LanguageModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const LanguageModal = ({ isOpen, onClose }: LanguageModalProps) => {
    const handleLanguageSelect = (langCode: string) => {
        // Find the Google Translate select element
        const select = document.querySelector('.goog-te-combo') as HTMLSelectElement;
        if (select) {
            select.value = langCode;
            select.dispatchEvent(new Event('change'));
        }
        onClose();
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
                    />
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 20 }}
                        className="relative w-full max-w-lg bg-white dark:bg-slate-800 rounded-2xl shadow-xl overflow-hidden"
                    >
                        {/* Header */}
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 dark:border-slate-700">
                            <div className="flex items-center gap-2 text-slate-800 dark:text-white">
                                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-full text-blue-600 dark:text-blue-400">
                                    <Globe size={20} />
                                </div>
                                <h2 className="text-lg font-semibold">Select Language</h2>
                            </div>
                            <button
                                onClick={onClose}
                                className="p-2 hover:bg-gray-100 dark:hover:bg-slate-700 rounded-full transition-colors text-gray-500 dark:text-slate-400"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        {/* Language Grid */}
                        <div className="p-4 grid grid-cols-2 gap-3 max-h-[60vh] overflow-y-auto scrollbar-thin">
                            {languages.map((lang) => (
                                <button
                                    key={lang.code}
                                    onClick={() => handleLanguageSelect(lang.code)}
                                    className="flex flex-col items-start p-3 rounded-xl border border-gray-100 dark:border-slate-700 hover:border-blue-500 dark:hover:border-blue-500 hover:bg-blue-50 dark:hover:bg-slate-700/50 transition-all duration-200 group text-left"
                                >
                                    <span className="text-sm font-medium text-slate-800 dark:text-slate-200 group-hover:text-blue-700 dark:group-hover:text-blue-400">
                                        {lang.name}
                                    </span>
                                    <span className="text-xs text-gray-500 dark:text-slate-400 mt-0.5">
                                        {lang.nativeName}
                                    </span>
                                </button>
                            ))}
                        </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
};

export default LanguageModal;
