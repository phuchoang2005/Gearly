import {Routes, Route} from 'react-router-dom';
import HomeLayout from '@u_layouts/HomeLayout.jsx';
import DefaultLayout from '@u_layouts/DefaultLayout.jsx';
import PublicOnlyRoute from '@u_components/routing/PublicOnlyRoute.jsx';
import PrivateRoute from '@u_components/routing/PrivateRoute.jsx';
import ScrollToTop from '@u_components/shared/ScrollToTop';
import LoadingScreen from '@u_components/shared/LoadingScreen';
import {lazy, Suspense} from 'react';
import ScrollToTopButton from "@u_components/shared/ScrollToTopButton.jsx";
import ChatBox from "@u_components/shared/ChatBox.jsx";
import BookDetailsPage from "@u_pages/BookDetailsPage/BookDetailsPage.jsx";
import WishlistPage from "@u_pages/WishlistPage.jsx";
import CartPage from "@u_pages/CartPage.jsx";
import CheckoutPage from "@u_pages/CheckoutPage.jsx";
import PaymentPage from "@u_pages/PaymentPage.jsx";
import OrderConfirmationPage from "@u_pages/OrderConfirmationPage.jsx";
import MomoReturnPage from "@u_pages/MomoReturnPage.jsx";
import PersonalTab from "@u_pages/AccountManagementPage/SubTab/PersonalTab.jsx";
import SecurityTab from "@u_pages/AccountManagementPage/SubTab/SecurityTab.jsx";
import OrdersTab from "@u_pages/AccountManagementPage/SubTab/OrdersTab.jsx";
import BlogDetailsPage from "@u_pages/ShopPage/BlogPage/BlogDetailsPage.jsx";
import BlogPage from "@u_pages/ShopPage/BlogPage/BlogPage.jsx";
import AboutUsPage from "@u_pages/ShopPage/AboutUsPage.jsx";
import UserTermsConditionPage from "@u_pages/ShopPage/UserTermsConditionPage.jsx"
import PrivacyPage from "@u_pages/ShopPage/PrivacyPage.jsx"
import ReturnPolicyPage from "@u_pages/ShopPage/ReturnPolicyPage.jsx"

const HomePage = lazy(() => import('@u_pages/HomePage/HomePage.jsx'));
const ShopPage = lazy(() => import('@u_pages/ShopPage/ShopPage.jsx'));
const ResendVerificationPage = lazy(() => import('@u_pages/ResendVerificationPage'));
const ForgotPasswordPage = lazy(() => import('@u_pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('@u_pages/ResetPasswordPage.jsx'));
const AccountPage = lazy(() => import('@u_pages/AccountManagementPage/AccountPage.jsx'));
const LoginPage = lazy(() => import('@u_pages/LoginPage'));
const RegisterPage = lazy(() => import('@u_pages/RegisterPage.jsx'));

export default function UserRoutes() {
    return (
        <>
            <ScrollToTop/>
            <Suspense fallback={<LoadingScreen/>}>
                <Routes>
                    <Route element={<HomeLayout/>}>
                        <Route path="/" element={<HomePage/>}/>
                        <Route path="/home" element={<HomePage/>}/>
                    </Route>
                    <Route element={<PublicOnlyRoute/>}>
                        <Route path="/login" element={<LoginPage/>}/>
                        <Route path="/register" element={<RegisterPage/>}/>
                    </Route>
                    <Route element={<DefaultLayout/>}>
                        <Route path="/resend-verification" element={<ResendVerificationPage/>}/>
                        <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>
                        <Route path="/reset-password" element={<ResetPasswordPage/>}/>
                        <Route path="/shop" element={<ShopPage/>}/>
                        <Route path="/book/:bookId" element={<BookDetailsPage/>}/>
                        <Route path="/wishlist" element={<WishlistPage/>}/>
                        <Route path="/cart" element={<CartPage/>}/>
                        <Route path="/blog" element={<BlogPage/>}/>
                        <Route path="/blog/:id" element={<BlogDetailsPage/>}/>
                        <Route path="/about-us" element={<AboutUsPage/>}/>
                        <Route path="/terms" element={<UserTermsConditionPage/>}/>
                        <Route path="/privacy" element={<PrivacyPage/>}/>
                        <Route path="/returns" element={<ReturnPolicyPage/>}/>
                        <Route element={<PrivateRoute/>}>
                            <Route path="/me" element={<AccountPage/>}>
                                <Route index element={<PersonalTab/>}/>
                                <Route path="security" element={<SecurityTab/>}/>
                                <Route path="orders" element={<OrdersTab/>}/>
                            </Route>
                            <Route path="/checkout" element={<CheckoutPage/>}/>
                            <Route path="/payment" element={<PaymentPage/>}/>
                            <Route path="/order-confirmation" element={<OrderConfirmationPage/>}/>
                            <Route path="/momo-return" element={<MomoReturnPage/>}/>
                        </Route>
                    </Route>
                </Routes>
                <ChatBox/>
                <ScrollToTopButton/>
            </Suspense>
        </>
    );
}
