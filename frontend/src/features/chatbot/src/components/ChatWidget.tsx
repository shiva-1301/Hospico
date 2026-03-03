import { useState, useRef, useEffect } from 'react';
import { MessageCircle, X, Send, Bot, User, Loader2, Mic, MicOff, Volume2 } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { motion, AnimatePresence } from 'framer-motion';
import { apiRequest } from '../api';
import { useSelector } from "react-redux";
import type { RootState } from "../store/store";
import { Link } from 'react-router-dom';
import SharedHospitalCard, { type Hospital as HospitalType } from './HospitalCard';
import { parseVoiceCommand, formatDateForInput } from '../utils/voiceCommandParser';

interface Hospital extends HospitalType { }

interface Message {
    role: 'system' | 'user' | 'assistant' | 'bot';
    content: string;
    hospitals?: Hospital[];
    symptomMatch?: {
        symptom: string;
        inferredIssue: string;
        specializations: string[];
        confidence: string;
        disclaimer: string;
    };
    sessionId?: string;
    step?: string;
    doctors?: Array<{
        id: string;
        name: string;
        specialization: string;
        qualifications: string;
        experience?: string;
        imageUrl?: string;
    }>;
    availableSlots?: string[];
    appointmentDetails?: {
        hospital: string;
        doctor: string;
        date: string;
        time: string;
        patient?: string;
    };
}

interface ChatWidgetProps {
    autoOpen?: boolean;
    embedMode?: boolean;
}

const ChatWidget = ({ autoOpen = false, embedMode = false }: ChatWidgetProps) => {
    const { theme } = useTheme();
    const [isOpen, setIsOpen] = useState(false);
    const [showAuthPrompt, setShowAuthPrompt] = useState(false);

    // Get auth state from Redux store
    const { isAuthenticated, user: authUser } = useSelector((state: RootState) => state.auth);

    // Handle auth state changes
    useEffect(() => {
        if (isAuthenticated) {
            setShowAuthPrompt(false);
        } else {
            // User logged out
            setIsOpen(false);
            setCurrentSessionId(null);
            setMessages([
                { role: 'system', content: "Hi! I'm your health assistant. I can provide general symptom information, but please note that I may not be fully accurate. For a proper diagnosis, please always consult a doctor." }
            ]);
        }
    }, [isAuthenticated]);

    useEffect(() => {
        if (autoOpen || embedMode || (typeof window !== 'undefined' && window.self !== window.top)) {
            if (embedMode || autoOpen) {
                if (isAuthenticated) {
                    setIsOpen(true);
                }
            }
        }
    }, [autoOpen, embedMode, isAuthenticated]);

    useEffect(() => {
        if (typeof window !== 'undefined' && window.parent) {
            window.parent.postMessage({ type: 'CHATBOT_STATE', isOpen }, '*');
        }
    }, [isOpen]);

    const [messages, setMessages] = useState<Message[]>([
        { role: 'system', content: "Hi! I'm your health assistant. I can provide general symptom information, but please note that I may not be fully accurate. For a proper diagnosis, please always consult a doctor." }
    ]);
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isListening, setIsListening] = useState(false);
    const [speakingMessageId, setSpeakingMessageId] = useState<number | null>(null);
    const [userLocation, setUserLocation] = useState<{ latitude: number; longitude: number } | null>(null);
    const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
    const [bookingReason, setBookingReason] = useState('');

    // Voice booking automation state
    const [voiceBookingActive, setVoiceBookingActive] = useState(false);
    const [currentBookingStep, setCurrentBookingStep] = useState<string | null>(null);
    const [lastVoiceCommand, setLastVoiceCommand] = useState<string>('');

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const recognitionRef = useRef<any>(null);
    
    // Refs to hold current values for speech recognition callbacks
    const voiceBookingActiveRef = useRef(voiceBookingActive);
    const currentBookingStepRef = useRef(currentBookingStep);
    
    // Update refs when state changes
    useEffect(() => {
        voiceBookingActiveRef.current = voiceBookingActive;
    }, [voiceBookingActive]);
    
    useEffect(() => {
        currentBookingStepRef.current = currentBookingStep;
    }, [currentBookingStep]);

    // Get user location on mount
    useEffect(() => {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    setUserLocation({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    });
                },
                (error) => {
                    console.log('Geolocation error:', error.message);
                },
                { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
            );
        }
    }, []);

    // Initialize Speech Recognition
    useEffect(() => {
        if (typeof window !== 'undefined') {
            const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
            if (SpeechRecognition) {
                recognitionRef.current = new SpeechRecognition();
                recognitionRef.current.continuous = false;
                recognitionRef.current.interimResults = true;
                recognitionRef.current.lang = 'en-US';

                recognitionRef.current.onresult = (event: any) => {
                    let finalTranscript = '';
                    let interimTranscript = '';

                    for (let i = 0; i < event.results.length; i++) {
                        const transcript = event.results[i][0].transcript;
                        if (event.results[i].isFinal) {
                            finalTranscript += transcript + ' ';
                        } else {
                            interimTranscript += transcript;
                        }
                    }

                    // Show live transcription in input field
                    if (interimTranscript && !voiceBookingActiveRef.current && !currentBookingStepRef.current) {
                        setInput(prev => {
                            const baseInput = prev.replace(/\[Listening...\].*$/, '').trim();
                            return baseInput + (baseInput ? ' ' : '') + '[Listening...] ' + interimTranscript;
                        });
                    }

                    // Handle final transcript
                    if (finalTranscript) {
                        console.log('[Speech Recognition] Final:', finalTranscript.trim());

                        if (voiceBookingActiveRef.current || currentBookingStepRef.current) {
                            handleVoiceBookingCommand(finalTranscript.trim());
                            // Keep listening in booking mode
                            setTimeout(() => {
                                if (voiceBookingActiveRef.current && recognitionRef.current && !isListening) {
                                    try {
                                        recognitionRef.current.start();
                                        setIsListening(true);
                                    } catch (e) {
                                        console.error('[Voice] Failed to restart:', e);
                                    }
                                }
                            }, 500);
                        } else {
                            // Normal mode: set final transcript to input field
                            setInput(prev => {
                                const baseInput = prev.replace(/\[Listening...\].*$/, '').trim();
                                return baseInput + (baseInput ? ' ' : '') + finalTranscript.trim();
                            });
                        }
                    }
                };

                recognitionRef.current.onerror = (event: any) => {
                    console.log('[Voice] Error:', event.error);
                    if (event.error === 'aborted') return;
                    setIsListening(false);
                    if (!voiceBookingActiveRef.current && !currentBookingStepRef.current) {
                        setInput(prev => prev.replace(/\[Listening...\].*$/, '').trim());
                    }
                };
                
                recognitionRef.current.onend = () => {
                    console.log('[Voice] Recognition ended');
                    setIsListening(false);
                    if (!voiceBookingActiveRef.current && !currentBookingStepRef.current) {
                        setInput(prev => prev.replace(/\[Listening...\].*$/, '').trim());
                    }
                };
            }
        }

        return () => {
            if (recognitionRef.current) {
                recognitionRef.current.abort();
            }
            window.speechSynthesis?.cancel();
        };
    }, []); // Empty deps - only initialize once

    const toggleListening = () => {
        if (!recognitionRef.current) {
            alert('Speech recognition is not supported in your browser.');
            return;
        }

        if (isListening) {
            console.log('[Voice] Stopping...');
            recognitionRef.current.stop();
            setIsListening(false);
            setInput(prev => prev.replace(/\[Listening...\].*$/, '').trim());
        } else {
            console.log('[Voice] Starting...');
            try {
                // Clear any previous listening indicator
                setInput(prev => prev.replace(/\[Listening...\].*$/, '').trim());
                recognitionRef.current.start();
                setIsListening(true);
            } catch (e: any) {
                console.error('[Voice] Failed to start:', e);
                // If already started, stop and try again
                if (e.name === 'InvalidStateError') {
                    try {
                        recognitionRef.current.stop();
                        setTimeout(() => {
                            try {
                                recognitionRef.current.start();
                                setIsListening(true);
                            } catch (retryError) {
                                console.error('[Voice] Retry failed:', retryError);
                                alert('Voice recognition error. Please try again.');
                            }
                        }, 300);
                    } catch (stopError) {
                        console.error('[Voice] Stop failed:', stopError);
                    }
                }
            }
        }
    };

    const getCurrentLanguage = (): string => {
        const select = document.querySelector('.goog-te-combo') as HTMLSelectElement;
        if (select && select.value) return select.value;
        const htmlLang = document.documentElement.lang;
        if (htmlLang && htmlLang !== 'en') return htmlLang.split('-')[0];
        return 'en';
    };

    const getVoiceLocale = (langCode: string): string => {
        const localeMap: { [key: string]: string } = {
            'en': 'en-US', 'hi': 'hi-IN', 'te': 'te-IN', 'ta': 'ta-IN', 'kn': 'kn-IN',
            'ml': 'ml-IN', 'mr': 'mr-IN', 'gu': 'gu-IN', 'bn': 'bn-IN', 'pa': 'pa-IN',
            'or': 'or-IN', 'as': 'as-IN', 'ur': 'ur-IN'
        };
        return localeMap[langCode] || 'en-US';
    };

    const speakMessage = (text: string, messageIndex: number) => {
        if (!window.speechSynthesis) return;
        if (speakingMessageId === messageIndex) {
            window.speechSynthesis.cancel();
            setSpeakingMessageId(null);
            return;
        }
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = getVoiceLocale(getCurrentLanguage());
        utterance.onstart = () => setSpeakingMessageId(messageIndex);
        utterance.onend = () => setSpeakingMessageId(null);
        utterance.onerror = () => setSpeakingMessageId(null);
        window.speechSynthesis.speak(utterance);
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, isOpen]);

    // Track booking steps and enable voice mode
    useEffect(() => {
        if (messages.length > 0) {
            const lastMessage = messages[messages.length - 1];

            if (lastMessage.role === 'bot' && lastMessage.step) {
                setCurrentBookingStep(lastMessage.step);

                // Voice booking disabled - appointment booking feature removed
                setVoiceBookingActive(false);
                setIsListening(false);
                if (recognitionRef.current) {
                    try {
                        recognitionRef.current.stop();
                    } catch (e) {
                        // ignore
                    }
                }
            }
        }
    }, [messages, isListening]);

    const handleSendMessage = async (messageText: string) => {
        if (!messageText.trim()) return;
        const userMessage: Message = { role: 'user', content: messageText };
        setMessages(prev => [...prev, userMessage]);
        setIsLoading(true);

        try {
            const history = messages
                .filter(msg => msg.role !== 'system')
                .map(msg => ({
                    role: msg.role === 'bot' ? 'assistant' : msg.role,
                    content: msg.content
                }));
            history.push({ role: 'user', content: userMessage.content });

            const response = await apiRequest<{
                reply?: string;
                type?: string;
                hospitals?: Hospital[];
                symptom?: string;
                inferredIssue?: string;
                specializations?: string[];
                confidence?: string;
                disclaimer?: string;
                sessionId?: string;
                step?: string;
            }, any>(
                '/api/chat',
                'POST',
                {
                    messages: history,
                    language: getCurrentLanguage(),
                    latitude: userLocation?.latitude,
                    longitude: userLocation?.longitude,
                    sessionId: currentSessionId
                }
            );

            if (response.sessionId) {
                setCurrentSessionId(response.sessionId);
            }

            if (response.hospitals) {
                setMessages(prev => [...prev, {
                    role: 'bot',
                    content: response.reply || "Based on your symptoms, here are some recommended hospitals:",
                    hospitals: response.hospitals,
                    sessionId: response.sessionId,
                    step: response.step,
                    symptomMatch: response.type === 'specialization_match' ? {
                        symptom: response.symptom || '',
                        inferredIssue: response.inferredIssue || '',
                        specializations: response.specializations || [],
                        confidence: response.confidence || 'medium',
                        disclaimer: response.disclaimer || 'This is not a medical diagnosis.'
                    } : undefined
                }]);
            } else {
                setMessages(prev => [...prev, {
                    role: 'bot',
                    content: response.reply || "I'm sorry, I couldn't process that.",
                    sessionId: response.sessionId,
                    step: response.step
                }]);
            }
        } catch (error) {
            const errMsg = error instanceof Error ? error.message : "Sorry, I'm having trouble connecting to the server.";
            setMessages(prev => [...prev, { role: 'bot', content: errMsg }]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSend = async () => {
        if (!input.trim()) return;
        const messageText = input;
        setInput('');
        await handleSendMessage(messageText);
    };

    // Handle voice commands during booking flow
    const handleVoiceBookingCommand = (transcript: string) => {
        if (!currentBookingStep) {
            setInput(prev => prev + ' ' + transcript);
            return;
        }

        const command = parseVoiceCommand(transcript, currentBookingStep);
        setLastVoiceCommand(transcript);

        if (command.type === 'cancellation') {
            setVoiceBookingActive(false);
            setCurrentBookingStep(null);
            setMessages(prev => [...prev, {
                role: 'bot',
                content: 'Booking cancelled. How else can I help you?'
            }]);
            return;
        }

        const lastMessage = messages[messages.length - 1];

        switch (currentBookingStep) {
            case 'hospital_selection':
                if (command.type === 'number' && lastMessage.hospitals) {
                    const index = command.value - 1;
                    if (index >= 0 && index < lastMessage.hospitals.length) {
                        const hospital = lastMessage.hospitals[index];
                        speakMessage(`Selecting ${hospital.name}`, -1);
                        handleBookingAction('select_hospital', String(hospital.id || hospital.clinicId));
                    } else {
                        speakMessage(`Invalid choice. Please say a number between 1 and ${lastMessage.hospitals.length}`, -1);
                    }
                }
                break;

            case 'doctor_selection':
                if (command.type === 'number' && lastMessage.doctors) {
                    const index = command.value - 1;
                    if (index >= 0 && index < lastMessage.doctors.length) {
                        const doctor = lastMessage.doctors[index];
                        speakMessage(`Selecting Dr. ${doctor.name}`, -1);
                        handleBookingAction('select_doctor', doctor.id);
                    } else {
                        speakMessage(`Invalid choice. Please say a number between 1 and ${lastMessage.doctors.length}`, -1);
                    }
                }
                break;

            case 'date_selection':
                if (command.type === 'date') {
                    const dateStr = formatDateForInput(command.value);
                    speakMessage(`Selecting date ${command.value.toLocaleDateString()}`, -1);
                    handleBookingAction('select_date', dateStr);
                } else if (command.type === 'number') {
                    const date = new Date();
                    date.setDate(date.getDate() + command.value);
                    const dateStr = formatDateForInput(date);
                    speakMessage(`Selecting ${command.value} days from today`, -1);
                    handleBookingAction('select_date', dateStr);
                }
                break;

            case 'time_selection':
                if (command.type === 'number' && lastMessage.availableSlots) {
                    const index = command.value - 1;
                    if (index >= 0 && index < lastMessage.availableSlots.length) {
                        const slot = lastMessage.availableSlots[index];
                        speakMessage(`Selecting ${slot}`, -1);
                        handleBookingAction('select_time', slot);
                    } else {
                        speakMessage(`Invalid choice. Please say a number between 1 and ${lastMessage.availableSlots.length}`, -1);
                    }
                } else if (command.type === 'time' && lastMessage.availableSlots) {
                    const matchedSlot = lastMessage.availableSlots.find(slot =>
                        slot.toLowerCase().includes(command.value.toLowerCase())
                    );
                    if (matchedSlot) {
                        speakMessage(`Selecting ${matchedSlot}`, -1);
                        handleBookingAction('select_time', matchedSlot);
                    } else {
                        speakMessage('Time not available. Please choose from the available slots.', -1);
                    }
                }
                break;

            case 'patient_details':
                if (command.type === 'confirmation') {
                    const authUserData = authUser;
                    if (authUserData && authUserData.id) {
                        handleBookingAction('confirm_booking', authUserData.id, {
                            patientName: authUserData.name,
                            patientAge: authUserData.age,
                            patientGender: authUserData.gender,
                            patientPhone: authUserData.phone,
                            patientEmail: authUserData.email,
                            reason: bookingReason || 'General consultation'
                        });
                    }
                }
                break;
        }
    };

    const handleBookingAction = async (action: string, value: string, patientData?: any) => {
        if (!currentSessionId) {
            console.error('No active session');
            return;
        }

        setIsLoading(true);
        try {
            const response = await apiRequest<{
                step: string;
                message: string;
                doctors?: any[];
                availableSlots?: string[];
                appointmentDetails?: any;
                details?: any;
                sessionId?: string;
            }, any>(
                '/api/chat/action',
                'POST',
                {
                    sessionId: currentSessionId,
                    action,
                    value,
                    ...patientData
                }
            );

            setMessages(prev => [...prev, {
                role: 'bot',
                content: response.message,
                step: response.step,
                doctors: response.doctors,
                availableSlots: response.availableSlots,
                appointmentDetails: response.appointmentDetails || response.details,
                sessionId: response.sessionId || currentSessionId
            }]);

            if (response.step === 'booking_confirmed') {
                setBookingReason('');
                setCurrentSessionId(null);
            }

        } catch (error) {
            const errMsg = error instanceof Error ? error.message : "Failed to process your selection.";
            setMessages(prev => [...prev, { role: 'bot', content: errMsg }]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="fixed z-50 pointer-events-none">
            {/* Blurred Backdrop - Only when Open */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 bg-black/40 backdrop-blur-sm pointer-events-auto z-40"
                        onClick={() => setIsOpen(false)}
                    />
                )}
            </AnimatePresence>

            {/* Centered Chatbot Popup */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: -20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: -20 }}
                        className="fixed inset-0 flex items-center justify-center pointer-events-auto z-50"
                    >
                        <div className={`w-11/12 h-5/6 max-w-2xl max-h-[99vh] ${theme === 'dark' ? 'bg-gray-900 border-gray-700' : 'bg-white border-gray-200'} shadow-2xl border rounded-2xl flex flex-col overflow-hidden pointer-events-auto`}>
                            {/* Header */}
                            <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-4 flex items-center justify-between text-white shadow-md">
                                <div className="flex items-center gap-3">
                                    <Bot size={20} />
                                    <h3 className="font-semibold text-lg">HealthMate Bot</h3>
                                </div>
                                {!embedMode && (
                                    <button onClick={() => setIsOpen(false)} className="p-2 hover:bg-white/20 rounded-full transition-colors">
                                        <X size={20} />
                                    </button>
                                )}
                            </div>

                            {/* Messages Area */}
                            <div className={`flex-1 overflow-y-auto p-4 space-y-4 ${theme === 'dark' ? 'bg-gray-900' : 'bg-gray-50'}`}>
                                {messages.map((msg, idx) => (
                                    <div key={idx} className={`flex flex-col gap-2 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                                        <div className={`flex items-start gap-2 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                                            <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${msg.role === 'user' ? 'bg-indigo-600' : 'bg-blue-600'} text-white`}>
                                                {msg.role === 'user' ? <User size={16} /> : <Bot size={16} />}
                                            </div>
                                            <div className={`max-w-[85%] p-3 rounded-2xl text-sm ${msg.role === 'user' ? 'bg-indigo-600 text-white' : (theme === 'dark' ? 'bg-gray-800 text-gray-100' : 'bg-white text-gray-700')} shadow-sm border ${theme === 'dark' ? 'border-gray-700' : 'border-gray-100'} overflow-hidden`}>
                                                <div className="whitespace-pre-wrap break-words font-medium">{msg.content}</div>

                                                {msg.symptomMatch && (
                                                    <div className="mt-3 space-y-2">
                                                        <div className="flex flex-wrap gap-1">
                                                            {msg.symptomMatch.specializations.map((spec, i) => (
                                                                <span key={i} className="px-2 py-0.5 bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300 text-[10px] rounded-full">
                                                                    {spec}
                                                                </span>
                                                            ))}
                                                        </div>
                                                        <div className="p-2 bg-amber-50 dark:bg-amber-900/20 text-amber-800 dark:text-amber-200 text-[11px] rounded border border-amber-200/50">
                                                            ⚠️ {msg.symptomMatch.disclaimer}
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        </div>

                                        {/* Symptom Explanation Options */}
                                        {msg.step === 'symptom_explanation' && (
                                            <div className="ml-10 mt-3 flex flex-col gap-2 max-w-[calc(100%-40px)]">
                                                <button
                                                    onClick={() => handleSendMessage('Show nearby hospitals')}
                                                    className={`text-left px-4 py-2.5 rounded-lg border-2 transition-all text-sm font-medium ${theme === 'dark' ? 'bg-blue-600 border-blue-500 hover:bg-blue-700 text-white' : 'bg-blue-500 border-blue-400 hover:bg-blue-600 text-white'}`}
                                                >
                                                    🏥 Show nearby hospitals
                                                </button>
                                                <button
                                                    onClick={() => handleSendMessage('Book an appointment')}
                                                    className={`text-left px-4 py-2.5 rounded-lg border-2 transition-all text-sm font-medium ${theme === 'dark' ? 'bg-green-600 border-green-500 hover:bg-green-700 text-white' : 'bg-green-500 border-green-400 hover:bg-green-600 text-white'}`}
                                                >
                                                    📅 Book an appointment
                                                </button>
                                            </div>
                                        )}

                                        {/* Hospitals scroll outside bubbles for better layout */}
                                        {msg.hospitals && msg.hospitals.length > 0 && msg.step === 'hospital_selection' && (
                                            <div className="ml-10 mt-2 space-y-2 max-w-[calc(100%-40px)] overflow-hidden">
                                                <div className="flex gap-3 overflow-x-auto pb-2 scrollbar-hide snap-x w-full">
                                                    {msg.hospitals.map((h, hIdx) => (
                                                        <div key={`${h.clinicId || h.id}-${hIdx}`} className="min-w-[240px] max-w-[240px] snap-start relative group">
                                                            <SharedHospitalCard hospital={h} theme={theme} showDistance={true} />
                                                            <button
                                                                onClick={() => handleBookingAction('select_hospital', String(h.id || h.clinicId))}
                                                                className="absolute inset-0 bg-blue-600/0 hover:bg-blue-600/10 transition-colors rounded-lg border-2 border-transparent hover:border-blue-500 flex items-end justify-center pb-2 opacity-0 group-hover:opacity-100"
                                                            >
                                                                <span className="bg-blue-600 text-white text-xs px-3 py-1 rounded-full font-semibold shadow-lg">
                                                                    Select Hospital
                                                                </span>
                                                            </button>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}

                                        {/* Doctor Selection - Cards */}
                                        {msg.doctors && msg.doctors.length > 0 && msg.step === 'doctor_selection' && (
                                            <div className="ml-10 mt-2 space-y-2 max-w-[calc(100%-40px)] overflow-hidden">
                                                <div className="flex gap-3 overflow-x-auto pb-2 scrollbar-hide snap-x w-full">
                                                    {msg.doctors.map((doctor, dIdx) => (
                                                        <div key={`${doctor.id}-${dIdx}`} className="min-w-[220px] max-w-[220px] snap-start relative group">
                                                            <div className={`h-full p-4 rounded-lg border-2 transition-all flex flex-col justify-between ${theme === 'dark' ? 'bg-gray-800 border-gray-700 group-hover:border-blue-500 group-hover:bg-gray-750' : 'bg-white border-gray-200 group-hover:border-blue-500 group-hover:bg-blue-50'}`}>
                                                                {doctor.imageUrl && (
                                                                    <div className="mb-3 -mx-4 -mt-4 mb-2">
                                                                        <img
                                                                            src={doctor.imageUrl}
                                                                            alt={doctor.name}
                                                                            className="w-full h-32 object-cover rounded-t-md"
                                                                            onError={(e) => {
                                                                                e.currentTarget.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"%3E%3Crect fill="%23ddd" width="100" height="100"/%3E%3Ctext x="50" y="50" dominant-baseline="middle" text-anchor="middle" font-size="14"%3E%F0%9F%91%A8%E2%80%8D%E2%9A%95%EF%B8%8F%3C/text%3E%3C/svg%3E';
                                                                            }}
                                                                        />
                                                                    </div>
                                                                )}

                                                                <div>
                                                                    <div className="font-bold text-sm text-blue-600 dark:text-blue-400 mb-1">👨‍⚕️ Doctor</div>
                                                                    <div className="font-semibold text-sm mb-2 line-clamp-2">{doctor.name}</div>
                                                                    <div className={`text-xs mb-2 font-medium ${theme === 'dark' ? 'text-gray-300' : 'text-gray-600'}`}>
                                                                        {doctor.specialization}
                                                                    </div>
                                                                    {doctor.qualifications && (
                                                                        <div className={`text-[10px] mb-3 ${theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}`}>
                                                                            {doctor.qualifications}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                                <button
                                                                    onClick={() => handleBookingAction('select_doctor', doctor.id)}
                                                                    className="w-full bg-blue-600 hover:bg-blue-700 text-white text-xs px-3 py-2 rounded-md font-semibold transition-colors"
                                                                >
                                                                    Select
                                                                </button>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}

                                        {/* Date Selection */}
                                        {msg.step === 'date_selection' && (
                                            <div className="ml-10 mt-2 max-w-[calc(100%-40px)]">
                                                <input
                                                    type="date"
                                                    min={new Date().toISOString().split('T')[0]}
                                                    onChange={(e) => {
                                                        if (e.target.value) {
                                                            handleBookingAction('select_date', e.target.value);
                                                        }
                                                    }}
                                                    className={`w-full p-3 rounded-lg border-2 text-sm ${theme === 'dark' ? 'bg-gray-800 border-gray-700 text-white' : 'bg-white border-gray-300'}`}
                                                />
                                            </div>
                                        )}

                                        {/* Time Slot Selection */}
                                        {msg.availableSlots && msg.availableSlots.length > 0 && msg.step === 'time_selection' && (
                                            <div className="ml-10 mt-2 max-w-[calc(100%-40px)]">
                                                <div className="grid grid-cols-3 gap-2">
                                                    {msg.availableSlots.map((slot) => (
                                                        <button
                                                            key={slot}
                                                            onClick={() => handleBookingAction('select_time', slot)}
                                                            className={`p-2 rounded-lg text-xs font-medium transition-all ${theme === 'dark' ? 'bg-gray-800 border border-gray-700 hover:bg-blue-600 hover:border-blue-500' : 'bg-white border border-gray-300 hover:bg-blue-600 hover:text-white'}`}
                                                        >
                                                            {slot}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                        )}

                                        {/* Patient Details Form */}
                                        {msg.step === 'patient_details' && msg.appointmentDetails && authUser && (
                                            <div className="ml-10 mt-2 max-w-[calc(100%-40px)] space-y-2">
                                                <div className={`p-3 rounded-lg text-xs ${theme === 'dark' ? 'bg-gray-800' : 'bg-blue-50'}`}>
                                                    <div><strong>Hospital:</strong> {msg.appointmentDetails.hospital}</div>
                                                    <div><strong>Doctor:</strong> {msg.appointmentDetails.doctor}</div>
                                                    <div><strong>Date:</strong> {msg.appointmentDetails.date}</div>
                                                    <div><strong>Time:</strong> {msg.appointmentDetails.time}</div>
                                                </div>

                                                <div className={`p-3 rounded-lg text-xs ${theme === 'dark' ? 'bg-gray-800' : 'bg-gray-50'}`}>
                                                    <div className="font-semibold mb-2">Your Information:</div>
                                                    <div><strong>Name:</strong> {authUser.name || 'Not set'}</div>
                                                    <div><strong>Age:</strong> {authUser.age || 'Not set'}</div>
                                                    <div><strong>Gender:</strong> {authUser.gender || 'Not set'}</div>
                                                    <div><strong>Phone:</strong> {authUser.phone || 'Not set'}</div>
                                                    <div><strong>Email:</strong> {authUser.email}</div>
                                                </div>

                                                <textarea
                                                    placeholder="Reason for visit / Additional notes (optional)"
                                                    value={bookingReason}
                                                    onChange={(e) => setBookingReason(e.target.value)}
                                                    className={`w-full p-2 rounded text-sm ${theme === 'dark' ? 'bg-gray-700 border-gray-600' : 'bg-white border-gray-300'} border`}
                                                    rows={3}
                                                />
                                                <button
                                                    onClick={() => {
                                                        if (!authUser.name || !authUser.age || !authUser.gender || !authUser.phone) {
                                                            alert('Please complete your profile information first. Go to Profile page to update your details.');
                                                            return;
                                                        }
                                                        handleBookingAction('confirm_booking', authUser.id, {
                                                            patientName: authUser.name,
                                                            patientAge: authUser.age,
                                                            patientGender: authUser.gender,
                                                            patientPhone: authUser.phone,
                                                            patientEmail: authUser.email,
                                                            reason: bookingReason || 'General consultation'
                                                        });
                                                    }}
                                                    className="w-full p-2 rounded bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 transition-colors"
                                                >
                                                    Confirm Booking
                                                </button>
                                                {(!authUser.name || !authUser.age || !authUser.gender || !authUser.phone) && (
                                                    <p className="text-xs text-red-500 text-center">
                                                        Please update your profile details. <Link to="/profile" className="text-blue-500 hover:underline">Go to Profile</Link>
                                                    </p>
                                                )}
                                            </div>
                                        )}

                                        {/* Booking Confirmed */}
                                        {msg.step === 'booking_confirmed' && msg.appointmentDetails && (
                                            <div className="ml-10 mt-2 max-w-[calc(100%-40px)]">
                                                <div className={`p-4 rounded-lg ${theme === 'dark' ? 'bg-green-900/30 border-green-700' : 'bg-green-50 border-green-200'} border-2`}>
                                                    <div className="text-green-700 dark:text-green-300 font-bold mb-2">✅ Booking Confirmed!</div>
                                                    <div className="text-xs space-y-1">
                                                        <div><strong>Hospital:</strong> {msg.appointmentDetails.hospital}</div>
                                                        <div><strong>Doctor:</strong> {msg.appointmentDetails.doctor}</div>
                                                        <div><strong>Date:</strong> {msg.appointmentDetails.date}</div>
                                                        <div><strong>Time:</strong> {msg.appointmentDetails.time}</div>
                                                        <div><strong>Patient:</strong> {msg.appointmentDetails.patient}</div>
                                                    </div>
                                                </div>
                                            </div>
                                        )}

                                        {(msg.role === 'bot' || msg.role === 'system') && (
                                            <button onClick={() => speakMessage(msg.content, idx)} className="ml-10 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
                                                <Volume2 size={14} className={speakingMessageId === idx ? 'text-indigo-500 animate-pulse' : ''} />
                                            </button>
                                        )}
                                    </div>
                                ))}
                                {isLoading && (
                                    <div className="flex items-center gap-2 text-gray-400 text-xs ml-10">
                                        <Loader2 size={14} className="animate-spin" />
                                        <span>Thinking...</span>
                                    </div>
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            {/* Voice Booking Mode Indicator */}
                            {voiceBookingActive && (
                                <div className={`px-4 py-2 border-t ${theme === 'dark' ? 'bg-blue-900/20 border-gray-800' : 'bg-blue-50 border-gray-100'}`}>
                                    <div className="flex items-center gap-2 text-xs">
                                        <Mic className="animate-pulse text-blue-600" size={16} />
                                        <span className={`font-medium ${theme === 'dark' ? 'text-blue-300' : 'text-blue-700'}`}>
                                            Voice Booking Active
                                        </span>
                                        <span className={`${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`}>
                                            - Say a number, "yes", or "cancel"
                                        </span>
                                        {lastVoiceCommand && (
                                            <span className={`ml-auto ${theme === 'dark' ? 'text-gray-500' : 'text-gray-400'}`}>
                                                Last: "{lastVoiceCommand}"
                                            </span>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Input Area */}
                            <div className={`p-4 border-t ${theme === 'dark' ? 'bg-gray-900 border-gray-800' : 'bg-white border-gray-100'}`}>
                                <div className={`flex items-center border rounded-full px-4 py-2 ${theme === 'dark' ? 'bg-gray-800 border-gray-700' : 'bg-gray-50 border-gray-200'}`}>
                                    <input
                                        type="text"
                                        value={input}
                                        onChange={(e) => setInput(e.target.value)}
                                        onKeyDown={handleKeyPress}
                                        placeholder="Type symptoms..."
                                        className={`flex-1 bg-transparent border-none focus:ring-0 text-sm h-8 ${theme === 'dark' ? 'text-white placeholder-gray-500' : 'text-gray-900 placeholder-gray-400'}`}
                                    />
                                    <button onClick={toggleListening} className={`p-2 rounded-full ${isListening ? 'text-red-500 animate-pulse' : 'text-gray-400'}`}>
                                        {isListening ? <MicOff size={18} /> : <Mic size={18} />}
                                    </button>
                                    <button onClick={handleSend} disabled={!input.trim() || isLoading} className="p-2 text-indigo-600 disabled:opacity-50">
                                        <Send size={18} />
                                    </button>
                                </div>
                                <p className="text-[11px] text-center mt-2 text-gray-400 underline">Not a professional diagnosis. Consult a doctor.</p>
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Floating Toggle Button - Always Visible */}
            <motion.button
                onClick={() => {
                    if (!isAuthenticated) {
                        setShowAuthPrompt(true);
                        setTimeout(() => setShowAuthPrompt(false), 5000);
                    } else {
                        setIsOpen(!isOpen);
                    }
                }}
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.95 }}
                className="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-lg hover:shadow-xl transition-shadow pointer-events-auto flex items-center justify-center z-50"
            >
                <AnimatePresence mode="wait">
                    {isOpen ? (
                        <motion.div key="close" initial={{ rotate: -90 }} animate={{ rotate: 0 }} exit={{ rotate: 90 }}>
                            <X size={24} />
                        </motion.div>
                    ) : (
                        <motion.div key="chat" initial={{ rotate: 90 }} animate={{ rotate: 0 }} exit={{ rotate: -90 }}>
                            <MessageCircle size={24} />
                        </motion.div>
                    )}
                </AnimatePresence>
            </motion.button>

            {showAuthPrompt && !isAuthenticated && (
                <div className={`absolute bottom-20 right-0 w-64 p-4 rounded-xl shadow-2xl border ${theme === 'dark' ? 'bg-gray-800 border-gray-700 text-white' : 'bg-white border-gray-200'} pointer-events-auto`}>
                    <h4 className="font-bold text-sm mb-2 text-blue-500">Login Required</h4>
                    <p className="text-xs text-gray-400 mb-4">Please login to access the chatbot and other features.</p>
                    <div className="flex gap-2">
                        <Link to="/login" className="flex-1 py-2 text-center bg-blue-600 text-white text-xs rounded font-medium">Login</Link>
                        <Link to="/signup" className="flex-1 py-2 text-center border border-gray-600 text-gray-300 text-xs rounded font-medium">Sign Up</Link>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ChatWidget;
