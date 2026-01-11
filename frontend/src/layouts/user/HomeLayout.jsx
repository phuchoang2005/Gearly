import HomeHeader from '@u_components/shared/HomeHeader.jsx';
import { Outlet } from 'react-router-dom';
import Footer from "@u_components/shared/Footer.jsx";

export default function HomeLayout() {
    return (
        <>
            <HomeHeader />
            <main>
                <Outlet />
            </main>
            <Footer />
        </>
    );
}
