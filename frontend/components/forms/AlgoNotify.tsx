"use client";

import React, { useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

export type NotifyType = 'success' | 'error' | 'warning';

interface AlgoNotifyProps {
    show: boolean;
    type: NotifyType;
    title: string;
    message: string;
    onClose: () => void;
    duration?: number;
}

export default function AlgoNotify({
    show,
    type,
    title,
    message,
    onClose,
    duration = 5000
}: AlgoNotifyProps) {

    useEffect(() => {
        if (show && duration > 0) {
            const timer = setTimeout(() => {
                onClose();
            }, duration);
            return () => clearTimeout(timer);
        }
    }, [show, duration, onClose]);

    const themes = {
        success: {
            bg: 'bg-green-500/10',
            border: 'border-green-500/20',
            icon: 'text-green-500',
            progress: 'bg-green-500'
        },
        error: {
            bg: 'bg-red-500/10',
            border: 'border-red-500/20',
            icon: 'text-red-500',
            progress: 'bg-red-500'
        },
        warning: {
            bg: 'bg-blue-500/10', // Consistent with ALGOCORE theme
            border: 'border-blue-500/20',
            icon: 'text-blue-500',
            progress: 'bg-blue-500'
        }
    };

    const theme = themes[type];

    return (
        <AnimatePresence>
            {show && (
                <motion.div
                    initial={{ opacity: 0, x: 100, scale: 0.9 }}
                    animate={{ opacity: 1, x: 0, scale: 1 }}
                    exit={{ opacity: 0, x: 100, scale: 0.9 }}
                    className={`fixed top-10 right-10 z-[1000] min-w-[320px] max-w-[400px] p-6 rounded-xl border backdrop-blur-xl ${theme.bg} ${theme.border} shadow-2xl`}
                >
                    <div className="flex items-start gap-4">
                        {/* Type Icon */}
                        <div className={`mt-1 h-2 w-2 rounded-full ${theme.progress} shadow-[0_0_10px_currentColor] ${theme.icon}`} />
                        
                        <div className="flex-1">
                            <h4 className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 mb-1">
                                {type} Notification
                            </h4>
                            <p className="text-sm font-bold text-white mb-1 uppercase tracking-tighter">
                                {title}
                            </p>
                            <p className="text-xs text-white/50 leading-relaxed font-medium">
                                {message}
                            </p>
                        </div>

                        {/* Close Button */}
                        <button 
                            onClick={onClose}
                            className="text-white/20 hover:text-white transition-colors"
                        >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>

                    {/* Progress Bar Animation */}
                    <motion.div 
                        initial={{ width: '100%' }}
                        animate={{ width: '0%' }}
                        transition={{ duration: duration / 1000, ease: 'linear' }}
                        className={`absolute bottom-0 left-0 h-[2px] ${theme.progress} opacity-50`}
                    />
                </motion.div>
            )}
        </AnimatePresence>
    );
}
