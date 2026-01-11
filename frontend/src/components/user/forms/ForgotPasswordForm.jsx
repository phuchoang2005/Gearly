import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { forgotPasswordSchema } from '@utils/validate.js';
import { useForgotPassword } from '@u_hooks/useForgotPassword.js';
import FormInput from '@u_components/shared/FormInput.jsx';
import { Link } from 'react-router-dom';

export default function ForgotPasswordForm() {
    const { loading, submit } = useForgotPassword();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm({
        resolver: yupResolver(forgotPasswordSchema),
    });

    return (
        <form onSubmit={handleSubmit(submit)} className="space-y-6">
            <FormInput
                label="Email"
                required
                placeholder="Enter your email"
                {...register('email')}
                error={errors.email?.message}
                autoComplete="email"
            />

            <div className="w-full">
                <button
                    type="submit"
                    disabled={loading}
                    className={`w-1/2 bg-black flex justify-center mx-auto text-white py-3 rounded-md font-medium
                        transition hover:bg-gray-900
                        ${loading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                >
                    {loading ? 'Please wait…' : 'Send Reset Link'}
                </button>
            </div>

            <p className="text-center text-sm">
                Remembered your password?{' '}
                <Link to="/login" className="text-black underline hover:text-gray-700 transition">
                    Log in
                </Link>
            </p>
        </form>
    );
}
