import { useContext } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { AuthContext } from "@contexts/AuthContext.jsx";
import DefaultLayout from "@u_layouts/DefaultLayout.jsx";

export default function PublicOnlyRoute() {
    const { auth } = useContext(AuthContext);
    const location = useLocation();

    if (auth) {
        const redirectTo = location.state?.from || "/";
        return <Navigate to={redirectTo} replace />;
    }

    return (
        <DefaultLayout>
            <Outlet />
        </DefaultLayout>
    );
}
