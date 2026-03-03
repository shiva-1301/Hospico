import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { apiRequest } from '../../api';

export type ReportCategory = 'Diagnostics' | 'Scanning' | 'Prescriptions' | 'Bills';

export interface MedFile {
    id: string;
    name: string;
    type: string;
    size: number;
    date: string;
    url?: string;
    category: ReportCategory;
    uploadDate?: string;
    data?: string;
}

interface MedicalRecordsState {
    files: MedFile[];
    loading: boolean;
    error: string | null;
}

const initialState: MedicalRecordsState = {
    files: [],
    loading: false,
    error: null,
};

// Helper: Get API URL
const getApiUrl = (path: string) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
    // Remove trailing slash from base if present
    const cleanBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    // Remove leading slash from path if present
    const cleanPath = path.startsWith('/') ? path.slice(1) : path;
    return `${cleanBase}/${cleanPath}`;
}

export const fetchUserRecords = createAsyncThunk(
    'medicalRecords/fetchUserRecords',
    async (userId: number, { rejectWithValue }) => {
        try {
            // API returns list of records without file data
            // Using generic type for apiRequest if possible, else casting
            const records = await apiRequest<MedFile[]>(`/api/medical-records/user/${userId}`);

            return records.map((record: MedFile) => ({
                ...record,
                id: String(record.id),
                date: record.uploadDate ? new Date(record.uploadDate).toLocaleDateString() : new Date().toLocaleDateString(),
                url: getApiUrl(`/api/medical-records/${record.id}`)
            }));
        } catch (err: any) {
            return rejectWithValue(err.message || 'Failed to fetch records');
        }
    }
);

export const uploadRecord = createAsyncThunk(
    'medicalRecords/uploadRecord',
    async ({ file, category, userId, customName }: { file: File; category: ReportCategory; userId: number; customName?: string }, { rejectWithValue }) => {
        try {
            const formData = new FormData();
            // Pass customName as the filename argument to FormData.append
            formData.append('file', file, customName || file.name);
            formData.append('category', category);
            formData.append('userId', String(userId));

            const token = localStorage.getItem('jwt_token');
            // Using fetch because we need to handle FormData (axios usually works but apiRequest in this project forces JSON)
            const response = await fetch(getApiUrl('api/medical-records/upload'), {
                method: 'POST',
                headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
                    // Browser sets Content-Type for FormData
                },
                body: formData
            });

            if (!response.ok) {
                let errMsg = 'Upload failed';
                try {
                    const errData = await response.json();
                    errMsg = errData.message || errMsg;
                } catch (e) {/* ignore */ }
                throw new Error(errMsg);
            }

            const record = await response.json();
            return {
                ...record,
                id: String(record.id),
                date: new Date().toLocaleDateString(),
                url: getApiUrl(`/api/medical-records/${record.id}`)
            } as MedFile;

        } catch (err: any) {
            return rejectWithValue(err.message || 'Failed to upload');
        }
    }
);

export const deleteRecord = createAsyncThunk(
    'medicalRecords/deleteRecord',
    async (fileId: string, { rejectWithValue }) => {
        try {
            await apiRequest(`/api/medical-records/${fileId}`, 'DELETE');
            return fileId;
        } catch (err: any) {
            return rejectWithValue(err.message || 'Failed to delete');
        }
    }
);

const medicalRecordsSlice = createSlice({
    name: 'medicalRecords',
    initialState,
    reducers: {
        clearFiles: (state) => {
            state.files = [];
            state.loading = false;
            state.error = null;
        }
    },
    extraReducers: (builder) => {
        // Fetch
        builder.addCase(fetchUserRecords.pending, (state) => {
            state.loading = true;
            state.error = null;
        });
        builder.addCase(fetchUserRecords.fulfilled, (state, action) => {
            state.loading = false;
            state.files = action.payload;
        });
        builder.addCase(fetchUserRecords.rejected, (state, action) => {
            state.loading = false;
            state.error = action.payload as string;
        });

        // Upload
        builder.addCase(uploadRecord.pending, (state) => {
            state.loading = true;
            state.error = null;
        });
        builder.addCase(uploadRecord.fulfilled, (state, action) => {
            state.loading = false;
            state.files.push(action.payload);
        });
        builder.addCase(uploadRecord.rejected, (state, action) => {
            state.loading = false;
            state.error = action.payload as string;
        });

        // Delete
        builder.addCase(deleteRecord.fulfilled, (state, action) => {
            state.files = state.files.filter(file => file.id !== action.payload);
        });
    }
});

export const { clearFiles } = medicalRecordsSlice.actions;
export default medicalRecordsSlice.reducer;
