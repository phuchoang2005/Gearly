import { Routes, Route } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import LoadingScreen from "@u_components/shared/LoadingScreen.jsx";

// const AdminHomePage = lazy(() => import('@a_pages/AdminHomePage.jsx'));

export default function AdminRoutes() {
    return (
        <Suspense fallback={<LoadingScreen />}>
            <Routes>
                {/*<Route path="/admin" element={<AdminHomePage />} />*/}
                {/* Add more admin routes here */}
            </Routes>
        </Suspense>
    );
}
