export interface Hospital {
    id?: string;
    clinicId?: string;
    name: string;
    latitude: number;
    longitude: number;
    address?: string;
    phone?: string;
    distance?: number;
    specializations?: string[];
}

export interface Message {
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
}

export interface ChatWidgetProps {
    autoOpen?: boolean;
    embedMode?: boolean;
}

export interface ChatApiResponse {
    reply?: string;
    type?: string;
    hospitals?: Hospital[];
    symptom?: string;
    inferredIssue?: string;
    specializations?: string[];
    confidence?: string;
    disclaimer?: string;
}

export interface ThemeContextType {
    theme: 'light' | 'dark';
    setTheme?: (theme: 'light' | 'dark') => void;
}

export interface ReduxAuthState {
    isAuthenticated: boolean;
    user?: any;
}

export interface RootState {
    auth: ReduxAuthState;
    [key: string]: any;
}
