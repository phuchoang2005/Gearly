import ResetPasswordForm from '@u_components/forms/ResetPasswordForm.jsx';
import Logo from '@assets/brand-logo-black.png';
import { Link } from 'react-router-dom';

export default function ResetPasswordPage() {
    return (
        <div className="flex items-start justify-center py-8 bg-gray-100 min-h-screen">
            <div className="bg-white w-full max-w-xl py-10 px-10 shadow-lg rounded-md">
                <Link to="/" className="block w-fit">
                    <img src={Logo} alt="Bookify logo" className="w-32 mb-6" />
                </Link>

                <h1 className="text-3xl font-semibold text-center mb-8">Reset Your Password</h1>

                <ResetPasswordForm />
            </div>
        </div>
    );
}
