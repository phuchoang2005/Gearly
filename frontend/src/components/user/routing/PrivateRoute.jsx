    import { useContext } from "react";
    import { Navigate, Outlet, useLocation } from "react-router-dom";
    import { AuthContext } from "@contexts/AuthContext.jsx";

    export default function PrivateRoute() {
        const { auth } = useContext(AuthContext);
        const location = useLocation();

        if (!auth) {
            return <Navigate to="/login" state={{ from: location }} replace />;
        }

        return <Outlet />;
    }
