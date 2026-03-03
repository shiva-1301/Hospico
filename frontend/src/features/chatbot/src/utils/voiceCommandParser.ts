type VoiceCommandType = 'number' | 'date' | 'time' | 'confirmation' | 'cancellation' | 'unknown';

interface VoiceCommand {
    type: VoiceCommandType;
    value?: any;
}

const numberWords: Record<string, number> = {
    one: 1,
    two: 2,
    three: 3,
    four: 4,
    five: 5,
    six: 6,
    seven: 7,
    eight: 8,
    nine: 9,
    ten: 10,
    eleven: 11,
    twelve: 12
};

export const parseVoiceCommand = (transcript: string, step: string): VoiceCommand => {
    const text = transcript.toLowerCase().trim();

    if (text.includes('cancel') || text.includes('stop') || text.includes('never mind') || text.includes('nevermind')) {
        return { type: 'cancellation' };
    }

    if (text.includes('yes') || text.includes('confirm') || text.includes('book') || text.includes('proceed')) {
        return { type: 'confirmation' };
    }

    const numberMatch = text.match(/\b(\d+)\b/);
    if (numberMatch) {
        return { type: 'number', value: parseInt(numberMatch[1], 10) };
    }

    for (const [word, value] of Object.entries(numberWords)) {
        if (text.includes(word)) {
            return { type: 'number', value };
        }
    }

    if (step === 'date_selection') {
        if (text.includes('today')) {
            return { type: 'date', value: new Date() };
        }
        if (text.includes('tomorrow')) {
            const date = new Date();
            date.setDate(date.getDate() + 1);
            return { type: 'date', value: date };
        }

        const isoMatch = text.match(/\b(\d{4})-(\d{2})-(\d{2})\b/);
        if (isoMatch) {
            const date = new Date(`${isoMatch[1]}-${isoMatch[2]}-${isoMatch[3]}`);
            if (!Number.isNaN(date.getTime())) {
                return { type: 'date', value: date };
            }
        }

        const slashMatch = text.match(/\b(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})\b/);
        if (slashMatch) {
            const day = parseInt(slashMatch[1], 10);
            const month = parseInt(slashMatch[2], 10) - 1;
            const year = parseInt(slashMatch[3], 10);
            const date = new Date(year, month, day);
            if (!Number.isNaN(date.getTime())) {
                return { type: 'date', value: date };
            }
        }
    }

    if (step === 'time_selection') {
        const timeMatch = text.match(/\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b/);
        if (timeMatch) {
            let hour = parseInt(timeMatch[1], 10);
            const minute = timeMatch[2] ? parseInt(timeMatch[2], 10) : 0;
            const meridiem = timeMatch[3];

            if (meridiem) {
                if (meridiem === 'pm' && hour < 12) {
                    hour += 12;
                }
                if (meridiem === 'am' && hour === 12) {
                    hour = 0;
                }
            }

            const normalized = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
            return { type: 'time', value: normalized };
        }
    }

    return { type: 'unknown' };
};

export const formatDateForInput = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};
