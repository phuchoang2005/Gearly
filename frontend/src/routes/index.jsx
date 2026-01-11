import { BrowserRouter, Routes, Route } from 'react-router-dom';
import UserRoutes from '@routes/UserRoutes.jsx';
import AdminRoutes from '@routes/AdminRoutes.jsx';

export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/admin/*" element={<AdminRoutes />} />
                <Route path="/*" element={<UserRoutes />} />
            </Routes>
        </BrowserRouter>
    );
}
