import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { resetPasswordSchema } from '@utils/validate.js';
import { useResetPassword } from '@u_hooks/useResetPassword.js';
import FormInput from '@u_components/shared/FormInput.jsx';

export default function ResetPasswordForm() {
    const { loading, submit } = useResetPassword();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm({
        resolver: yupResolver(resetPasswordSchema),
    });

    return (
        <form onSubmit={handleSubmit(submit)} className="space-y-6">
            <FormInput
                label="New Password"
                type="password"
                required
                placeholder="Enter new password"
                {...register('newPassword')}
                error={errors.newPassword?.message}
                autoComplete="new-password"
            />

            <FormInput
                label="Confirm Password"
                type="password"
                required
                placeholder="Confirm new password"
                {...register('confirmPassword')}
                error={errors.confirmPassword?.message}
                autoComplete="new-password"
            />

            <button
                type="submit"
                disabled={loading}
                className={`w-1/2 mx-auto block bg-black text-white text-sm font-semibold py-3 px-4 rounded-md mt-6
                    shadow-[0_4px_14px_0_rgba(0,0,0,0.25)]
                    hover:shadow-[0_6px_20px_rgba(0,0,0,0.35)]
                    transition-all duration-200 ease-in-out
                    ${loading ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
            >
                {loading ? 'Please wait…' : 'Reset Password'}
            </button>
        </form>
    );
}
