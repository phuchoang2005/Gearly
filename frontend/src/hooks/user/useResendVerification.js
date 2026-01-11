import { useState } from 'react';
import { resendVerificationEmail } from '@u_services/authService.js';
import { showPromise } from '@utils/toast.js';

export const useResendVerification = () => {
    const [loading, setLoading] = useState(false);

    const submit = async (data) => {
        setLoading(true);

        await showPromise(
            resendVerificationEmail(data.email),
            {
                loading: 'Resending verification email...',
                success: 'Verification email has been resent. Please check your inbox.',
                error: 'Failed to resend verification email.',
            }
        );

        setLoading(false);
    };

    return { loading, submit };
};
