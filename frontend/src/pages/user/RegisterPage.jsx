import { Link } from 'react-router-dom';
import RegisterForm from '@u_components/forms/RegisterForm.jsx';
import Logo from '@assets/brand-logo-black.png';

export default function RegisterPage() {
    return (
        <div className="min-h-screen flex items-start justify-center py-8 bg-gray-100">
            <div className="bg-white w-full max-w-2xl py-10 px-10 shadow-lg rounded-md">
                <Link to="/" className="block w-fit">
                    <img src={Logo} alt="Bookify logo" className="w-32 mb-6" />
                </Link>
                <h1 className="text-3xl font-semibold text-center mb-12">Sign Up</h1>

                <RegisterForm />
            </div>
        </div>
    );
}
