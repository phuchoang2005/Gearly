import { useContext, useState } from 'react';
import { loginUser } from '@u_services/authService.js';
import { AuthContext } from '@contexts/AuthContext.jsx';
import { showPromise } from '@utils/toast.js';

export const useLogin = () => {
    const { login } = useContext(AuthContext);
    const [loading, setLoading] = useState(false);

    const submit = async (data) => {
        setLoading(true);
        const res = await showPromise(
            loginUser({
                email: data.email,
                password: data.password,
            }),
            {
                loading: 'Logging in...',
                success: 'Login successful!',
                error: 'Login failed!',
            }
        );

        if (res) {
            login(res.data);

            if (data.rememberMe) {
                localStorage.setItem('rememberEmail', data.email);
            } else {
                localStorage.removeItem('rememberEmail');
            }
        }

        setLoading(false);
    };

    return { loading, submit };
};
