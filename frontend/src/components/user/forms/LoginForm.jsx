import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { loginSchema } from '@utils/validate.js';
import { useLogin } from '@u_hooks/useLogin.js';
import FormInput from '../shared/FormInput.jsx';
import { Link, useLocation } from 'react-router-dom';
import GoogleLoginButton from './GoogleLoginButton.jsx';
import { useEffect } from 'react';
import {showSuccess, showError, showInfo} from '@utils/toast.js';

export default function LoginForm() {
    const { loading, submit } = useLogin();
    const rememberedEmail = localStorage.getItem('rememberEmail') || '';
    const location = useLocation();

    const {
        register,
        handleSubmit,
        formState: { errors },
        setValue,
    } = useForm({
        resolver: yupResolver(loginSchema),
        defaultValues: {
            email: rememberedEmail,
            rememberMe: !!rememberedEmail,
        },
    });

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const verified = params.get('verified');
        const error = params.get('error');

        if (verified === '1') {
            showSuccess('Your email has been verified. Please log in.');
        } else if (verified === '0' && error) {
            showError(decodeURIComponent(error));
        } else if (location.state?.from) {
            showInfo('Please log in to continue');
        }

        window.history.replaceState({}, document.title, location.pathname);
    }, [location]);

    return (
        <form onSubmit={handleSubmit((d) => submit(d))} className="space-y-6 px-10">
            <FormInput
                label="Email"
                required
                placeholder="Enter Email Address"
                {...register('email')}
                error={errors.email?.message}
                autoComplete="email"
            />

            <FormInput
                label="Password"
                type="password"
                required
                placeholder="Enter Password"
                {...register('password')}
                error={errors.password?.message}
                autoComplete="current-password"
            />

            <div className="flex justify-between items-center text-sm">
                <label className="flex items-center gap-2">
                    <input
                        type="checkbox"
                        {...register('rememberMe')}
                        className="accent-black w-4 h-4"
                    />
                    Remember me
                </label>
                <Link to="/forgot-password" className="text-black underline hover:text-gray-700 transition">
                    Forgot Password?
                </Link>
            </div>

            <button
                type="submit"
                disabled={loading}
                className="w-1/3 mx-auto block bg-black text-white text-sm font-semibold py-3 px-4 rounded-md mt-6
                     shadow-[0_4px_14px_0_rgba(0,0,0,0.25)]
                     hover:shadow-[0_6px_20px_rgba(0,0,0,0.35)]
                     transition-all duration-200 ease-in-out
                     cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {loading ? 'Please wait…' : 'Log In'}
            </button>

            <GoogleLoginButton />

            <p className="text-center text-sm">
                Don’t have an account?{' '}
                <Link to="/register" className="text-black underline hover:text-gray-700 transition">
                    Sign up
                </Link>
            </p>
            <p className="text-center text-sm">
                <Link
                    to="/resend-verification"
                    className="text-black underline hover:text-gray-700 transition text-sm"
                >
                    Didn't receive verification email?
                </Link>
            </p>
        </form>
    );
}
