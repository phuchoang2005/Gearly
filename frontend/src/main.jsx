import {createRoot} from 'react-dom/client'
import './index.css'
import '@smastrom/react-rating/style.css';
import 'keen-slider/keen-slider.min.css';
import AppRoutes from '@routes/index.jsx';
import {GoogleOAuthProvider} from "@react-oauth/google";
import {AuthProvider} from "@contexts/AuthContext";
import {Toaster} from "react-hot-toast";
import queryClient from "./services/queryClient.js";
import {QueryClientProvider} from "@tanstack/react-query";
import {CheckoutProvider} from "@contexts/CheckoutContext.jsx";

const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
createRoot(document.getElementById('root')).render(
    <GoogleOAuthProvider clientId={clientId}>
        <QueryClientProvider client={queryClient}>
            <AuthProvider>
                <CheckoutProvider>
                    <AppRoutes/>
                    <Toaster
                        position="bottom-right"
                        reverseOrder={false}
                        toastOptions={{
                            className: '',
                            style: {
                                padding: '14px 16px',
                                fontSize: '0.875rem',
                                fontWeight: 500,
                                borderRadius: '8px',
                                boxShadow: '0 4px 10px rgba(0,0,0,0.1)',
                            },
                            success: {
                                duration: 3000,
                                style: {
                                    background: '#e6fffa',
                                    color: '#065f46',
                                    border: '1px solid #99f6e4',
                                },
                            },
                            error: {
                                duration: 5000,
                                style: {
                                    background: '#fef2f2',
                                    color: '#991b1b',
                                    border: '1px solid #fecaca',
                                },
                            },
                            // Custom styling for info
                            blank: {
                                icon: 'ℹ️',
                                duration: 4000,
                                style: {
                                    background: '#eff6ff',
                                    color: '#1e3a8a',
                                    border: '1px solid #bfdbfe',
                                },
                            },
                        }}
                    />
                </CheckoutProvider>
            </AuthProvider>
        </QueryClientProvider>
    </GoogleOAuthProvider>
);
