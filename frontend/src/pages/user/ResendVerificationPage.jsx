import { Link } from 'react-router-dom';
import ResendVerificationForm from '@u_components/forms/ResendVerificationForm';
import Logo from '@assets/brand-logo-black.png';

export default function ResendVerificationPage() {
    return (
        <div className="flex items-start justify-center py-8 bg-gray-100">
            <div className="bg-white w-full max-w-xl py-10 px-10 shadow-lg rounded-md">
                <Link to="/" className="block w-fit">
                    <img src={Logo} alt="Bookify logo" className="w-32 mb-6" />
                </Link>
                <h1 className="text-3xl font-semibold text-center mb-8">Resend Verification</h1>
                <ResendVerificationForm />
            </div>
        </div>
    );
}
