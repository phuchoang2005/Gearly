import { Link } from 'react-router-dom';
import LoginForm from '@u_components/forms/LoginForm';
import Logo from '@assets/brand-logo-black.png';

export default function LoginPage() {
    return (
        <div className="min-h-screen flex items-start justify-center py-8 bg-gray-100">
            <div className="bg-white w-full max-w-xl py-10 px-10 shadow-lg rounded-md">
                <Link to="/" className="block w-fit">
                    <img src={Logo} alt="Bookify logo" className="w-32 mb-6" />
                </Link>
                <h1 className="text-3xl font-semibold text-center mb-12">Login</h1>

                <LoginForm />
            </div>
        </div>
    );
}
