import { useState, useRef, useEffect } from 'react';
import { Upload, FileText, Eye, Trash2, File, Image as ImageIcon, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { useSelector } from "react-redux";
import { useAppDispatch, type RootState } from "../store/store";
import { fetchUserRecords, uploadRecord, deleteRecord, type ReportCategory } from "../features/medicalRecords/medicalRecordsSlice";
import { apiClient } from "../api";

const MedicalReports = () => {
    const [activeTab, setActiveTab] = useState<ReportCategory>('Diagnostics');
    const dispatch = useAppDispatch();
    const { files, loading, error } = useSelector((state: RootState) => state.medicalRecords);
    const { user } = useSelector((state: RootState) => state.auth);
    const [isDragging, setIsDragging] = useState(false);

    // Upload Modal State
    const [showUploadModal, setShowUploadModal] = useState(false);
    // Use an object wrapper to allow renaming
    const [pendingUploads, setPendingUploads] = useState<{ file: File; id: string; name: string }[]>([]);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const categories: ReportCategory[] = ['Diagnostics', 'Scanning', 'Prescriptions', 'Bills'];

    // Preview State
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [previewType, setPreviewType] = useState<string>('');

    const closePreview = () => {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            setPreviewUrl(null);
            setPreviewType('');
        }
    };

    const handleViewFile = async (file: { url?: string; name: string; type: string }) => {
        if (!file.url) return;

        try {
            const response = await apiClient.get(file.url, { responseType: 'blob' });
            const blob = new Blob([response.data], { type: response.headers['content-type'] || file.type });
            const url = URL.createObjectURL(blob);

            setPreviewUrl(url);
            setPreviewType(file.type);
        } catch (e) {
            console.error("Failed to load file", e);
            alert("Failed to load file. Please try again.");
        }
    };

    // Helper to create upload objects
    const createUploadObjects = (files: FileList) => {
        return Array.from(files).map(file => ({
            file,
            id: Math.random().toString(36).substr(2, 9),
            name: file.name
        }));
    };

    // Actually, let's just use the `downloadFile` for now but we need to change STRATEGY.
    // I will pause this Edit and modify api.ts first.

    useEffect(() => {
        if (user?.id) {
            dispatch(fetchUserRecords(Number(user.id)));
        }
    }, [dispatch, user?.id]);

    useEffect(() => {
        const activeTabElement = document.getElementById(`tab-${activeTab}`);
        if (activeTabElement) {
            activeTabElement.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
        }
    }, [activeTab]);



    // Handle initial file selection/drop
    const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
        const uploadedFiles = event.target.files;
        if (!uploadedFiles) return;
        setPendingUploads(createUploadObjects(uploadedFiles));
        setShowUploadModal(true);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        if (e.dataTransfer.files) {
            setPendingUploads(createUploadObjects(e.dataTransfer.files));
            setShowUploadModal(true);
        }
    };

    const handleDeleteFile = (id: string) => {
        if (confirm('Are you sure you want to delete this record?')) {
            dispatch(deleteRecord(id));
        }
    };

    // Confirm upload from modal
    const confirmUpload = async () => {
        if (!user?.id) return;

        // Upload files sequentially or parallel
        for (const item of pendingUploads) {
            await dispatch(uploadRecord({
                file: item.file,
                category: activeTab,
                userId: Number(user.id),
                customName: item.name // Pass the edited name
            }));
        }

        setPendingUploads([]);
        setShowUploadModal(false);
    };

    // Cancel upload
    const cancelUpload = () => {
        setPendingUploads([]);
        setShowUploadModal(false);
    };

    // Update name in pending list
    const updatePendingFileName = (id: string, newName: string) => {
        setPendingUploads(prev => prev.map(item =>
            item.id === id ? { ...item, name: newName } : item
        ));
    };

    // Remove single file from pending list
    const removePendingFile = (id: string) => {
        setPendingUploads(prev => {
            const newPending = prev.filter(item => item.id !== id);
            if (newPending.length === 0) {
                setShowUploadModal(false);
            }
            return newPending;
        });
    };

    const formatSize = (bytes: number) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const filteredFiles = files.filter(f => f.category === activeTab);

    const getFileIcon = (type: string) => {
        if (type.startsWith('image/')) return <ImageIcon className="h-6 w-6 text-purple-500" />;
        if (type.includes('pdf')) return <FileText className="h-6 w-6 text-red-500" />;
        return <File className="h-6 w-6 text-blue-500" />;
    };

    const handlePrevCategory = () => {
        const currentIndex = categories.indexOf(activeTab);
        if (currentIndex > 0) {
            setActiveTab(categories[currentIndex - 1]);
        }
    };

    const handleNextCategory = () => {
        const currentIndex = categories.indexOf(activeTab);
        if (currentIndex < categories.length - 1) {
            setActiveTab(categories[currentIndex + 1]);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-slate-900 transition-colors duration-200 p-4 sm:p-6 lg:p-8">
            <div className="max-w-6xl mx-auto space-y-6">

                {/* Header */}
                <div className="flex flex-col gap-2">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Medical Reports</h1>
                            <p className="text-gray-600 dark:text-slate-400">Manage and view your medical documents securely.</p>
                        </div>
                        <button
                            onClick={() => user?.id && dispatch(fetchUserRecords(Number(user.id)))}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors dark:text-blue-400 dark:hover:bg-slate-800"
                            title="Refresh Records"
                        >
                            Refresh
                        </button>
                    </div>
                </div>

                {/* Error Banner */}
                {error && (
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-xl flex items-center justify-between">
                        <span>{error}</span>
                        <button onClick={() => dispatch({ type: 'medicalRecords/clearFiles' })} className="text-sm font-semibold hover:underline">Dismiss</button>
                    </div>
                )}

                {/* Tabs with Navigation Arrows */}
                <div className="flex items-center gap-2 bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-200 dark:border-slate-700 p-1">
                    <button
                        onClick={handlePrevCategory}
                        disabled={activeTab === categories[0]}
                        className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:text-slate-400 dark:hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="Previous category"
                    >
                        <ChevronLeft size={20} />
                    </button>

                    <div className="flex-1 overflow-x-auto scrollbar-hide">
                        <div className="flex space-x-1 min-w-max">
                            {categories.map((category) => (
                                <button
                                    key={category}
                                    id={`tab-${category}`}
                                    onClick={() => setActiveTab(category)}
                                    className={`flex-1 min-w-[120px] py-2.5 px-4 rounded-lg text-sm font-medium transition-all duration-200 ${activeTab === category
                                        ? 'bg-blue-50 text-blue-700 dark:bg-blue-600/20 dark:text-blue-300 shadow-sm'
                                        : 'text-gray-600 dark:text-slate-400 hover:bg-gray-50 dark:hover:bg-slate-700/50 hover:text-gray-900 dark:hover:text-slate-200'
                                        }`}
                                >
                                    {category === 'Bills' ? 'Medical Bills' : `${category} Reports`}
                                </button>
                            ))}
                        </div>
                    </div>

                    <button
                        onClick={handleNextCategory}
                        disabled={activeTab === categories[categories.length - 1]}
                        className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:text-slate-400 dark:hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                        aria-label="Next category"
                    >
                        <ChevronRight size={20} />
                    </button>
                </div>

                {/* Main Content Area */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                    {/* Upload Section */}
                    <div className="lg:col-span-1">
                        <div
                            className={`border-2 border-dashed rounded-xl p-8 text-center transition-all cursor-pointer bg-white dark:bg-slate-800 h-full flex flex-col justify-center items-center gap-4 ${isDragging
                                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/10'
                                : 'border-gray-300 dark:border-slate-600 hover:border-blue-400 dark:hover:border-slate-500'
                                }`}
                            onDragOver={handleDragOver}
                            onDragLeave={handleDragLeave}
                            onDrop={handleDrop}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-full">
                                <Upload className="h-8 w-8 text-blue-600 dark:text-blue-400" />
                            </div>
                            <div>
                                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-1">
                                    Upload {activeTab === 'Bills' ? 'Bill' : 'Report'}
                                </h3>
                                <p className="text-sm text-gray-500 dark:text-slate-400 max-w-[200px] mx-auto">
                                    Drag & drop files or click to browse. Supported formats: PDF, JPG, PNG
                                </p>
                            </div>
                            <input
                                type="file"
                                ref={fileInputRef}
                                className="hidden"
                                multiple
                                accept=".pdf,.jpg,.jpeg,.png"
                                onChange={handleFileUpload}
                            />
                        </div>
                    </div>

                    {/* Files List Section */}
                    <div className="lg:col-span-2 space-y-4">
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center justify-between">
                            <span>{activeTab === 'Bills' ? 'Medical Bills' : `${activeTab} Reports`}</span>
                            <span className="text-sm font-normal text-gray-500 dark:text-slate-400 bg-gray-100 dark:bg-slate-800 px-2 py-1 rounded-md">
                                {filteredFiles.length} {filteredFiles.length === 1 ? 'file' : 'files'}
                            </span>
                        </h2>

                        {loading && files.length === 0 ? (
                            <div className="p-8 text-center text-gray-500">Loading records...</div>
                        ) : filteredFiles.length === 0 ? (
                            <div className="flex flex-col items-center justify-center py-12 bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-slate-700 border-dashed">
                                <FileText className="h-12 w-12 text-gray-300 dark:text-slate-600 mb-3" />
                                <p className="text-gray-500 dark:text-slate-400">No documents found in this category</p>
                            </div>
                        ) : (
                            <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-slate-700 divide-y divide-gray-100 dark:divide-slate-700 overflow-hidden shadow-sm">
                                {filteredFiles.map((file) => (
                                    <div key={file.id} className="p-4 hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors flex items-center gap-4 group">
                                        <div className="p-2 bg-gray-100 dark:bg-slate-700 rounded-lg shrink-0">
                                            {getFileIcon(file.type)}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <h4 className="text-sm font-medium text-gray-900 dark:text-white truncate" title={file.name}>
                                                {file.name}
                                            </h4>
                                            <div className="flex items-center gap-3 mt-1 text-xs text-gray-500 dark:text-slate-400">
                                                <span>{file.date}</span>
                                                <span>•</span>
                                                <span>{formatSize(file.size)}</span>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => handleViewFile(file)}
                                                className="p-2 text-gray-500 hover:text-blue-600 dark:text-slate-400 dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors"
                                                title="View/Download"
                                            >
                                                <Eye className="h-4 w-4" />
                                            </button>
                                            <button
                                                onClick={() => handleDeleteFile(file.id)}
                                                className="p-2 text-gray-500 hover:text-red-600 dark:text-slate-400 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
                                                title="Delete"
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Rename/Review Upload Modal */}
            {showUploadModal && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                    <div className="bg-white dark:bg-slate-800 rounded-xl shadow-xl w-full max-w-lg overflow-hidden border border-gray-200 dark:border-slate-700">
                        <div className="p-6 border-b border-gray-100 dark:border-slate-700">
                            <h3 className="text-xl font-bold text-gray-900 dark:text-white">Review Uploads</h3>
                            <p className="text-sm text-gray-500 dark:text-slate-400 mt-1">
                                You can rename files before uploading.
                            </p>
                        </div>

                        <div className="p-6 max-h-[60vh] overflow-y-auto space-y-4">
                            {pendingUploads.map((item) => (
                                <div key={item.id} className="flex gap-4 items-start">
                                    <div className="p-2 bg-gray-100 dark:bg-slate-700 rounded-lg shrink-0 mt-1">
                                        {getFileIcon(item.file.type)}
                                    </div>
                                    <div className="flex-1 w-full">
                                        <label className="block text-xs font-medium text-gray-500 dark:text-slate-400 mb-1">
                                            File Name
                                        </label>
                                        <input
                                            type="text"
                                            value={item.name}
                                            onChange={(e) => updatePendingFileName(item.id, e.target.value)}
                                            className="w-full px-3 py-2 rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
                                        />
                                        <p className="text-xs text-gray-400 mt-1">{formatSize(item.file.size)}</p>
                                    </div>
                                    <button
                                        onClick={() => removePendingFile(item.id)}
                                        className="p-2 text-gray-400 hover:text-red-500 transition-colors mt-6"
                                        title="Remove from upload"
                                    >
                                        <X className="h-5 w-5" />
                                    </button>
                                </div>
                            ))}
                        </div>

                        <div className="p-6 border-t border-gray-100 dark:border-slate-700 flex justify-end gap-3 bg-gray-50 dark:bg-slate-900/50">
                            <button
                                onClick={cancelUpload}
                                className="px-4 py-2 rounded-lg text-gray-600 dark:text-slate-300 hover:bg-white dark:hover:bg-slate-700 border border-transparent hover:border-gray-200 dark:hover:border-slate-600 transition-all font-medium"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmUpload}
                                disabled={loading}
                                className="px-6 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold shadow-lg shadow-blue-500/30 transition-all disabled:opacity-70"
                            >
                                {loading ? 'Uploading...' : `Upload ${pendingUploads.length} ${pendingUploads.length === 1 ? 'File' : 'Files'}`}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {/* Preview Modal */}
            {previewUrl && (
                <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={closePreview}>
                    <div className="relative max-w-4xl w-full max-h-[90vh] flex flex-col items-center justify-center" onClick={e => e.stopPropagation()}>
                        <button
                            onClick={closePreview}
                            className="absolute -top-12 right-0 p-2 text-white hover:text-gray-300 transition-colors bg-white/10 rounded-full backdrop-blur-md"
                        >
                            <X size={24} />
                        </button>

                        {previewType.startsWith('image/') ? (
                            <img
                                src={previewUrl}
                                alt="Preview"
                                className="max-w-full max-h-[85vh] object-contain rounded-lg shadow-2xl"
                            />
                        ) : previewType === 'application/pdf' ? (
                            <iframe
                                src={previewUrl}
                                title="PDF Preview"
                                className="w-full h-[80vh] rounded-lg shadow-2xl bg-white"
                            />
                        ) : (
                            <div className="bg-white p-8 rounded-xl text-center">
                                <FileText size={64} className="mx-auto text-gray-400 mb-4" />
                                <p className="text-lg font-semibold mb-4">Preview not available for this file type</p>
                                <a
                                    href={previewUrl}
                                    download="document"
                                    target="_blank"
                                    rel="noreferrer"
                                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors inline-block cursor-pointer"
                                    onClick={() => {
                                        // For mobile, we might need to help it trigger a download
                                        // This default behavior usually works in modern browsers
                                    }}
                                >
                                    Download to View
                                </a>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default MedicalReports;
