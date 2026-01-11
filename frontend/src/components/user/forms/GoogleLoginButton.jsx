import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useContext } from 'react';
import { AuthContext } from '@contexts/AuthContext.jsx';
import { showPromise } from '@utils/toast.js';

export default function GoogleLoginButton() {
    const navigate = useNavigate();
    const { login } = useContext(AuthContext);

    const handleGoogleLogin = async (credentialResponse) => {
        const credential = credentialResponse.credential;

        const res = await showPromise(
            () => axios.post('http://localhost:8080/api/auth/google/token', { credential }),
            {
                loading: 'Logging in with Google...',
                success: 'Login successful!',
                error: 'Google login failed. Please try again.',
            }
        );

        if (res) {
            const auth = res.data;
            login(auth);
            navigate('/');
        }
    };

    return (
        <div className="my-6 text-center">
            <div className="relative mb-3">
                <div className="flex items-center justify-center gap-2">
                    <span className="text-sm text-gray-400">or Log In with</span>
                </div>
            </div>

            <div className="w-3/5 mx-auto my-5">
                <GoogleLogin
                    onSuccess={handleGoogleLogin}
                    onError={() => showPromise(Promise.reject(), {
                        error: 'Google Sign-In failed.',
                    })}
                />
            </div>
        </div>
    );
}
