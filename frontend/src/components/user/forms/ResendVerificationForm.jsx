import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { resendVerificationSchema } from '@utils/validate.js';
import { useResendVerification } from '@u_hooks/useResendVerification.js';
import FormInput from '@u_components/shared/FormInput.jsx';
import { Link } from 'react-router-dom';

export default function ResendVerificationForm() {
    const { loading, submit } = useResendVerification();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm({
        resolver: yupResolver(resendVerificationSchema),
    });

    return (
        <form onSubmit={handleSubmit(submit)} className="space-y-6">
            <FormInput
                label="Email"
                required
                placeholder="Enter your registered email"
                {...register('email')}
                error={errors.email?.message}
                autoComplete="email"
            />

            <button
                type="submit"
                disabled={loading}
                className={`w-1/2 mx-auto block bg-black text-white text-sm font-semibold py-3 px-4 rounded-md mt-6
                 shadow-[0_4px_14px_0_rgba(0,0,0,0.25)]
                 hover:shadow-[0_6px_20px_rgba(0,0,0,0.35)]
                 transition-all duration-200 ease-in-out
                 ${loading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                `}
            >
                {loading ? 'Please wait…' : 'Resend Email'}
            </button>

            <p className="text-center text-sm">
                Back to{' '}
                <Link to="/login" className="text-black underline hover:text-gray-700 transition">
                    Login
                </Link>
            </p>
        </form>
    );
}
