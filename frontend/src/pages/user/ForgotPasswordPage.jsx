import ForgotPasswordForm from '@u_components/forms/ForgotPasswordForm';

export default function ForgotPasswordPage() {
    return (
        <div className="flex items-start justify-center py-8 bg-gray-100">
            <div className="bg-white w-full max-w-xl py-10 px-10 shadow-lg rounded-md">
                <h1 className="text-3xl font-semibold text-center mb-8">Forgot Password</h1>
                <ForgotPasswordForm />
            </div>
        </div>
    );
}
