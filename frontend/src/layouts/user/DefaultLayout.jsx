import SharedHeader from '@u_components/shared/SharedHeader.jsx';
import { Outlet } from 'react-router-dom';
import Footer from "@u_components/shared/Footer.jsx";

export default function DefaultLayout() {
    return (
        <>
            <SharedHeader />
            <main>
                <Outlet />
            </main>
            <Footer />
        </>
    );
}
